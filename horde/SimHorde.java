
package sim.app.horde;

import sim.util.*;
import sim.field.continuous.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.objects.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.*;
import java.util.*;
import sim.field.grid.*;
import java.net.*;
import java.io.*;

public class SimHorde extends Horde
{
	private static final long serialVersionUID = 1;

	// width and height of the environment
	public int width = 100;
	public int height = 100;


	// No obstacles should be larger than this size
	public static final double MAX_OBSTACLE_DIAMETER = 25;

	public static final boolean ALLOW_COLLISIONS_WITH_AGENTS = false;

	// objects come in two size categories: SMALL/MEDIUM and ARBITRARILY LARGE
	// SMALL/MEDIUM object may be detected within a certain range, depending on the object.
	public double SMALL_CLOSE_RANGE = 1.0;
	public double SMALL_MEDIUM_RANGE = 5.0;
	public double SMALL_FAR_RANGE = 10.0;
	public double SMALL_FULL_RANGE = Math.max(width, height);

	// ARBITRARILY LARGE are meant to be relatively sparse, and so they are globally detectable
	public double LARGE_FULL_RANGE = Math.max(width, height);

	// fields
	public IntGrid2D ground = new IntGrid2D(width + 1, height + 1);
	public DoubleGrid2D food = new DoubleGrid2D(width + 1, height + 1);
	public Continuous2D agents = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);
	public Continuous2D obstacles = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
	public Continuous2D regions = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
	public Continuous2D markers = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);

	// Other required stuffs
	String arena = "arenas/default.arena.txt";

	/** Converts a floating-point coordinate into a grid cell for the ground or floor */
	public int gridX(double x)
	{
		// we presume that x is positive, else the proper equation is Math.floor(x+0.5);
		return (int)(x + 0.5);
	}

	/** Converts a floating-point coordinate into a grid cell for the ground or floor */
	public int gridY(double y)
	{
		// we presume that y is positive, else the proper equation is Math.floor(y+0.5);
		return (int)(y + 0.5);
	}

	// mouse location in the horde world
	public Double2D mouseLoc;

	public ArrayList agentList = new ArrayList();

	public SimHorde(long seed)
	{
		super(seed);
		System.err.println("--- SimHorde.SimHorde(seed) :"
		                   + " called Horde(seed).");
	}

	Targetable defaultTargetable = new Targetable()
	{
		public void setParameterValue(int index) { }
		public Double2D getTargetLocation(Agent agent, Horde horde)
		{
			return mouseLoc;
		}
		public void setTargetLocation(Agent agent, Horde horde, Double2D location) { }
		public int getTargetStatus(Agent agent, Horde horde)
		{
			return 0;
		}
		public void setTargetStatus(Agent agent, Horde horde, int status) { }
		public int getTargetRank(Agent agent, Horde horde)
		{
			return -1;
		}
		public void setTargetRank(Agent agent, Horde horde, int rank) { }
		public boolean getTargetIntersects(Agent agent, Horde horde, Double2D location,
		                                   double slopSquared)
		{
			return getTargetLocation(agent, horde).distanceSq(location) <= slopSquared;
		}
	};


	public static final Color[] parameterObjectColor = new Color[] { Color.red, Color.blue, Color.green };
	public static final String[] parameterObjectColorName = new String[] { "red", "blue", "green" };

	Targetable[] parameterObject = new Targetable[] { defaultTargetable, defaultTargetable,
	        defaultTargetable
	                                                };
	public Targetable getParameterObject(int i)
	{
		return parameterObject[i];
	}
	public void setParameterObject(int i, Targetable obj)
	{
		// find the targetable if it's already a parameter
		int foundObj = -1;
		for(int j = 0; j < parameterObject.length; j++)
			if (parameterObject[j] == obj)
			{
				foundObj = j;
				break;
			}

		// CASE 1:
		// obj is not a parameter.  Make it a parameter and
		// replace the old one
		if (foundObj == -1)
		{
			parameterObject[i].setParameterValue(-1);
			obj.setParameterValue(i);
			parameterObject[i] = obj;
		}
		// CASE 2:
		// obj is a parameter already.  Swap with the existing parameter
		else
		{
			Targetable old = parameterObject[i];
			old.setParameterValue(foundObj);
			obj.setParameterValue(i);
			parameterObject[i] = obj;
			parameterObject[foundObj] = old;
		}
	}

	public int getNumAgents()
	{
		return agentList.size();
	}

	public void setNumAgents(int s )
	{
		if (s < 1) return;
		else if (s < agentList.size())
		{
			// trim agents
			int len = agentList.size() - s;
			for(int i = 0 ; i < len; i++)
			{
				SimAgent agent = (SimAgent)(agentList.remove(agentList.size() - 1));
				agent.stoppable.stop();
				agents.remove(agent);
			}
			// is the training agent still there?
			if (!agentList.contains(trainingAgent))
				trainingAgent = (Agent)(agentList.get(0));
		}
		else if (s > agentList.size())
		{
			// add new agents
			int len = s - agentList.size();
			for(int i = 0; i < len; i++)
			{
				Agent agent = buildNewAgent();
				agentList.add(agent);
			}
		}

		distributeAndRestartBehaviors();
	}

	public void distributeAndRestartBehaviors()
	{
		for(int i = 0 ; i < agentList.size(); i++)
		{
			Agent agent = (Agent)(agentList.get(i));
			// only redistribute to other agents at my level
			if (agent.level == trainingLevel)
			{
				// if I'm not the training agent, steal from him
				if (agent != trainingAgent && trainingAgent != null &&
				        trainingAgent.getBehavior() != null)
					agent.setBehavior((TrainableMacro)
					                  (trainingAgent.getBehavior().clone()));
				// now restart
				agent.restart(this);
			}
		}
	}

	void loadArena(String resource)
	{
		Scanner scan = null;
		try
		{
			scan = new Scanner(new InputStreamReader(
			                       SimHorde.class.getResourceAsStream(resource), "UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			System.err.println("For some reason, UTF-8 isn't supported.");
			e.printStackTrace();
			return;
		}
		while (scan.hasNext())
		{
			String tok = scan.next().trim();
			if (tok.startsWith("#"))  // comment
			{
				scan.nextLine();
				continue;
			}
			// size is:   size width height
			else if (tok.equals("size"))
			{
				String val = scan.nextLine().trim();
				Scanner vscan = new Scanner(val);
				int w = vscan.nextInt();
				int h = vscan.nextInt();
				agents.width = regions.width = markers.width = obstacles.width = width = w;
				agents.height = regions.height = markers.height
				                                 = obstacles.height = height = h;
			}
			// Food grid is:    food filename
			else if (tok.equals("food"))
			{
				String val = scan.nextLine().trim();
				InputStream str = SimHorde.class.getResourceAsStream(val);
				double[][] res = null;
				try
				{
					if (val.endsWith(".pgm") || val.endsWith(".pbm") ||
					        val.endsWith(".PGM") || val.endsWith(".PBM"))
						;
					// food.setTo(TableLoader.convertToDoubleArray(
					// 			TableLoader.loadPNMFile(str, true)));
					/*
					   ^
					  |
					  -- the above line has problem, need to fix.
					  -- I commented out to test the pruning code -khaled */
					else food.setTo(TableLoader.loadTextFile(str, true));
				}
				catch (Exception e)
				{
					System.err.println("Could not load file defining the food grid");
					e.printStackTrace();
				}
			}
			// Ground is: ground filename
			else if (tok.equals("ground"))
			{
				String val = scan.nextLine().trim();
				InputStream str = SimHorde.class.getResourceAsStream(val);
				double[][] res = null;
				try
				{
					if (val.endsWith(".pgm") || val.endsWith(".pbm") ||
					        val.endsWith(".PGM") || val.endsWith(".PBM"))
						ground.setTo(TableLoader.loadPNMFile(str, true));
					else if (val.endsWith(".png") || val.endsWith(".gif"))
						; //ground.setTo(TableLoader.loadPNGFile(str));
					// -- the above line has problem, need to fix. -khaled
					else ground.setTo(TableLoader.convertToIntArray(
						                      TableLoader.loadTextFile(str, true)));
				}
				catch (Exception e)
				{
					System.err.println("Could not load file defining the ground grid");
					e.printStackTrace();
				}
			}
			// Markers are:	marker x y targetableIndex name
			// if targetableIndex < 0, it's not initially targetable
			else if (tok.equals("marker"))
			{
				String val = scan.nextLine().trim();
				Scanner vscan = new Scanner(val);
				Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
				int index = vscan.nextInt();
				String name = vscan.next();

				Marker marker = new Marker(name);
				markers.setObjectLocation(marker, loc);
				if (index >= 0)
					setParameterObject(index, marker);
			}
			// Circles are:     circle x y targetableIndex diameter field
			// fields are:      0=obstacles 1=regions
			// if targetableIndex < 0, it's not initially targetable
			else if (tok.equals("circle"))
			{
				String val = scan.nextLine().trim();
				Scanner vscan = new Scanner(val);
				Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
				int index = vscan.nextInt();
				double diameter = vscan.nextDouble();
				int f = vscan.nextInt();
				Continuous2D field = (f == 0 ? obstacles : regions);

				CircularBody body = new CircularBody(diameter, field, loc);
				field.setObjectLocation(body, loc);
				if (index >= 0)
					setParameterObject(index, body);
			}
			// Rects are:       rect x y targetableIndex width height field
			// fields are:      0=obstacles 1=regions
			// if targetableIndex < 0, it's not initially targetable
			else if (tok.equals("rect"))
			{
				String val = scan.nextLine().trim();
				Scanner vscan = new Scanner(val);
				Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
				int index = vscan.nextInt();
				double width = vscan.nextDouble();
				double height = vscan.nextDouble();
				int f = vscan.nextInt();
				Continuous2D field = (f == 0 ? obstacles : regions);

				RectangularBody body = new RectangularBody(width, height, field, loc);
				field.setObjectLocation(body, loc);
				if (index >= 0)
					setParameterObject(index, body);
			}
			// SquareCircles are:       squarecircle x y targetableIndex field
			// fields are:      0=obstacles 1=regions
			// if targetableIndex < 0, it's not initially targetable
			// This is just a hack to get the square-circle shape loadable for now
			else if (tok.equals("squarecircle"))
			{
				String val = scan.nextLine().trim();
				Scanner vscan = new Scanner(val);
				Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
				int index = vscan.nextInt();
				int f = vscan.nextInt();
				Continuous2D field = (f == 0 ? obstacles : regions);

				Area r = new Area(new Rectangle2D.Double(0,0,10,10));
				Area r2 = new Area(new Ellipse2D.Double(5,5,15,15));
				r.add(r2);

				Body body = new Body(r, field, loc);
				field.setObjectLocation(body, loc);
				if (index >= 0)
					setParameterObject(index, body);
			}
			// Text is:     text x y targetableIndex field fontsize string
			// fields are:      0=obstacles 1=regions
			// The string can have whitespace inside
			// if targetableIndex < 0, it's not initially targetable
			else if (tok.equals("text"))
			{
				String val = scan.nextLine().trim();
				Scanner vscan = new Scanner(val);
				Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
				int index = vscan.nextInt();
				int f = vscan.nextInt();
				Continuous2D field = (f == 0 ? obstacles : regions);
				int fontSize = vscan.nextInt();
				String string = vscan.nextLine().trim();

				Body body = new Body(string, fontSize, field, loc);
				field.setObjectLocation(body, loc);
				if (index >= 0)
					setParameterObject(index, body);
			}
			// Agent is:        agent x y orientation
			// Agents are not initially targetable :-( due to their dynamic nature
			// The first agent will be the initial training agent
			else if (tok.equals("agent"))
			{
				String val = scan.nextLine().trim();
				Scanner vscan = new Scanner(val);
				Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
				double orientation = ((vscan.nextDouble() % Math.PI) + Math.PI) % Math.PI;

				agentLocs.add(loc);
				agentOrientations.add(orientation);
			}
		}
		scan.close();
	}

	/*
	      public void addObstacles()
	      {
	      Double2D loc = new Double2D(0, 0);
	      obstacles.setObjectLocation(new CircularBody(10, obstacles, loc), loc);

	      loc = new Double2D(10, 20);
	      obstacles.setObjectLocation(new RectangularBody(10, 10, obstacles, loc), loc);

	      loc = new Double2D(10, 20);
	      regions.setObjectLocation(new RectangularBody(30, 30, obstacles, loc), loc);

	      loc = new Double2D(30, 30);
	      Area r = new Area(new Rectangle2D.Double(0,0,10,10));
	      Area r2 = new Area(new Ellipse2D.Double(5,5,15,15));
	      r.add(r2);
	      obstacles.setObjectLocation(new Body(r, obstacles, loc), loc);

	      loc = new Double2D(20, 20);
	      Shape s = new Font("Serif", 0, 12).createGlyphVector(new FontRenderContext(
	      new AffineTransform(),false,true),"Vittorio").getOutline();
	      obstacles.setObjectLocation(new Body(s, obstacles, loc), loc);


	      loc = new Double2D(width / 2.0, height / 2.0);
	      Marker homeBase = new Marker("Home Base");
	      markers.setObjectLocation(homeBase, loc);

	      loc = new Double2D(width / 3.0, height / 3.0);
	      Marker elsewhere = new Marker("Elsewhere");
	      markers.setObjectLocation(elsewhere, loc);

	      loc = new Double2D(width / 3.0, height / 3.0);
	      Marker third = new Marker("Third Place");
	      markers.setObjectLocation(third, loc);

	      // reset the default targets
	      setParameterObject( 0, homeBase);
	      setParameterObject( 1, elsewhere);
	      setParameterObject( 2, third);
	      }
	*/

	protected boolean hack = false ;
	public void start()
	{
		super.start();
		System.err.println("--- SimHorde.start() : starting SimHorde.");

		// set up the agents field
		agents = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);
		obstacles = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
		regions = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
		markers = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);

		agentCount = 0;

		System.err.println("--- SimHorde.start() : calling loadArena().");
		loadArena(arena);

		// clear out agents
		agentList.clear();

		// make the training agent
		if (!hack)
		{
			trainingAgent = (trainingLevel > 0) ?
			                buildNewControllerAgent(trainingLevel) : buildNewAgent();
			agentList.add(trainingAgent);
		}

		// add the other agents
		setNumAgents(agentCount);
	}


	public ControllerAgent buildNewControllerAgent(int trainingLevel)
	{
		return null;
	}

	int agentCount = 0;
	Bag agentLocs = new Bag();
	DoubleBag agentOrientations = new DoubleBag();

	public Agent buildNewAgent()
	{
		System.err.println("--- SimHorde.buildNewAgent() : buidling new agent.");
		SimAgent agent = new SimAgent(this);  // all the agents will use the training macro for now

		Double2D d = null;

		if (agentCount < agentLocs.size())
		{
			d = (Double2D)(agentLocs.get(agentCount));
			agent.setPose(d, agentOrientations.get(agentCount));
			agentCount++;
		}
		else
		{
			// build a random location
			do
			{
				d = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
			}
			while (Utilities.collision(this, d, null));
			agent.setPose(d, random.nextDouble() * Math.PI * 2 - Math.PI);
		}

		agent.stoppable = schedule.scheduleRepeating(agent);
		return agent;
	}

	public void setTraining(boolean val)
	{
		super.setRecording(val);
		// spread to other agents if val is false.  For now, we just have one
		if (!val)
			distributeAndRestartBehaviors();
	}

	public static void main(String[] args)
	{
		doLoop(SimHorde.class, args);
		System.exit(0);
	}

	// available ranks
	int nextRank = 0;
	public int assignRank()
	{
		return nextRank++;
	}
}
