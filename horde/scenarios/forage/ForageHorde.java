package sim.app.horde.scenarios.forage;

import java.awt.Color;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.objects.*;
import sim.field.continuous.*;
import sim.util.*;

public class ForageHorde extends SimHorde
{
	public int HOME_X = 75;
	public int HOME_Y = 75;
	public int RADIUS = 45;

	public int OBSTACLES_X = 50;
	public int OBSTACLES_Y = 50;
	public int OBSTACLE_RADIUS = 25;

	// mean of poisson distribution for arrival of obstacles
	public int OBSTACLE_PERIOD = 500;

	// maximum size of obstacles
	public int OBSTACLE_WIDTH = 15;
	public int OBSTACLE_HEIGHT = 15;

	public Double2D[] startingPositions;
	public Color[] foragerColors;
	public int colorIndex = 0;

	public static int collectedBoxes = 0;

	public static boolean experiment = false;
	protected int[] INIT_NUM_AGENTS_PER; // = { 5, 2, 1 };
	public int numControllers = 1;
	Behavior foragerBehavior = null;
	public static int totalNumAgents;

	protected DataCollection collector = null;

	public Continuous2D boxesField;
	public Continuous2D homeField;

	public void setInitNumAgentsPer(int [] a)
	{
		INIT_NUM_AGENTS_PER= new int[a.length];
		for (int i=0; i < a.length; i++)
			INIT_NUM_AGENTS_PER[i] = a[i];
	}

	public int getCollectedBoxes()
	{
		return collectedBoxes;
	}

	public Agent buildNewAgent()
	{
		Forager f = new Forager(this);
		f.setRank(colorIndex);
		f.paint = foragerColors[colorIndex];
		f.setPose(startingPositions[colorIndex], random.nextDouble() * 2 * Math.PI);
		f.stoppable = schedule.scheduleRepeating(f, -1, 1);
		foragerBehavior = initBehavior(f, foragerBehavior);
		return f;
	}

	static int cnt=0;
	public ControllerAgent buildNewControllerAgent(int level)
	{
		Supervisor s = new Supervisor(this, level);
		s.setPose(new Double2D(10, 50 + level*5*cnt++), 0);
		s.stoppable = schedule.scheduleRepeating(s, -(1 + level), 1);
		return s;
	}

	public void setBasicBehaviorLocation(String s)
	{
		BASIC_BEHAVIORS_LOCATION = s;
	}

	public void setBasicTargetLocation(String s)
	{
		BASIC_TARGETS_LOCATION = s;
	}

	public void setBasicFeatureLocation(String s)
	{
		BASIC_FEATURES_LOCATION = s;
	}

	public void setTrainableMacroDirectory(String s)
	{
		TRAINABLE_MACRO_DIRECTORY = s;
	}

	private static final long serialVersionUID = 1L;

	public ForageHorde(long seed)
	{
		super(seed);

		setBasicBehaviorLocation("scenarios/forage/forage.behaviors");
		setBasicTargetLocation("scenarios/forage/forage.targets");
		setBasicFeatureLocation("scenarios/forage/forage.features");
		setTrainableMacroDirectory(getPathInDirectory("scenarios/forage/trained/"));


		setInitNumAgentsPer(new int[] { 10 });

		//setInitNumAgentsPer(new int[] { 5, 5, 2} );
		//setInitNumAgentsPer(new int[] { 5, 10 } );
	}

	public int getNumAgentsBelowMe(int level)
	{
		int numAgentsBelowMe=INIT_NUM_AGENTS_PER[0];

		for (int i=1; i < level; i++)
			numAgentsBelowMe *= INIT_NUM_AGENTS_PER[i];

		return numAgentsBelowMe;
	}


	public Behavior initBehavior(Agent a, Behavior b)
	{
		if (b == null)
			a.setBehavior(new TrainableMacro().reset(this, buildNewParameters(), initialParameterObjectName,
			              Behavior.provideAllBehaviors(a.level), currentFeatures()));
		else
			a.setBehavior((Behavior) (b.clone()));
		return a.getBehavior();
	}

	public void buildAgents(int level, ControllerAgent controller)
	{
		if (level == INIT_NUM_AGENTS_PER.length - 1) // top level
		{
			agentList.clear();
			trainingAgent = null;
			schedule.reset();
			colorIndex = 0;
		}

		Behavior behav = null;

		for (int i = 0; i < INIT_NUM_AGENTS_PER[level]; i++)
		{
			Agent a = null;
			if (level == 0) // foragers
			{
				Forager f = (Forager) buildNewAgent();
				if (controller != null) controller.addSubsidiaryAgent(f); // controller.subsidiaryAgents.add(f);
				a = f;
			}
			else
			{
				Supervisor f = (Supervisor)buildNewControllerAgent(level);
				if (!f.coded )
					behav = initBehavior(f, behav);
				if (controller != null) controller.addSubsidiaryAgent(f); //controller.subsidiaryAgents.add(f);
				// load lower level
				buildAgents(level - 1, f);
				if (level == 1) colorIndex = (colorIndex + 1) % numControllers;

				a = f;
			}

			agentList.add(a);

			if (level == INIT_NUM_AGENTS_PER.length - 1) // top level
			{
				if (i == 0) // it's going to be the training agent
					trainingAgent = a;
			}
		}
	}

	public void startBypass()
	{
		super.start();
	}

	public Marker getHomeBase()
	{
		return homeBase;
	}

	Marker homeBase;

	protected void initParameters()
	{
		width = 200;
		height = 200;
		//maximumDistanceForAgentsSquared = ((width * width / 4) + (height * height / 4));

		collectedBoxes = 0;

		hack = true;
		trainingLevel = INIT_NUM_AGENTS_PER.length - 1;
		featureLevel = trainingLevel;


		startingPositions = new Double2D[] { new Double2D(HOME_X, HOME_Y) };
		foragerColors = new Color[] { Color.red };

		if (trainingLevel > 0)
		{
			numControllers = 1;
			for (int i = 1; i < INIT_NUM_AGENTS_PER.length; i++)
				numControllers *= INIT_NUM_AGENTS_PER[i];

			startingPositions = new Double2D[numControllers];
			foragerColors = new Color[numControllers];
			for (int i = 0; i < numControllers; i++)
			{
				startingPositions[i] = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
				foragerColors[i] = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
			}
		}

		totalNumAgents = INIT_NUM_AGENTS_PER[0];

		for (int i = 1; i < INIT_NUM_AGENTS_PER.length; i++)
			totalNumAgents *= INIT_NUM_AGENTS_PER[i];
	}

	protected void initFields(int attachments)
	{
		// build home base
		Double2D loc = new Double2D(HOME_X + RADIUS, HOME_Y);
		homeBase = new Marker("Home Base");

		homeField = new Continuous2D(width, width, height);
		homeField.setObjectLocation(homeBase, loc);

		setParameterObject(0, homeBase);

		boxesField = new Continuous2D(width, width, height);
		for (int i = 0; i < 10; i++)
		{
			if (i < 1)
				new Box(this).setMinimumAttachments((int)totalNumAgents/2);
			else
				new Box(this).setMinimumAttachments(attachments); // will do all the other work
		}
	}

	public void start()
	{
		initParameters();
		super.start();
		initFields(5);

		// load the agents, including the training agent
		buildAgents(trainingLevel, null);

		resetBehavior();

		//buildEnvironment();

		//collector = new DataCollection(this, "keith-trained-data-", INIT_NUM_AGENTS_PER);
		//collector.stoppable = schedule.scheduleRepeating(collector, 2, 1);

		if (experiment)
		{
			for (int i = 0; i < agentList.size(); i++)
			{
				TrainableMacro m = (TrainableMacro) (agentList.get(i).getBehavior());
				m.userChangedBehavior(this, m.indexOfBehaviorNamed("SwarmForage"));
				m.startBehavior = m.indexOfBehaviorNamed("SwarmForage");

				//m.userChangedBehavior(this, m.indexOfBehaviorNamed("GroupForage"));
				//m.startBehavior = m.indexOfBehaviorNamed("GroupForage");
			}
			setTraining(false);
		}
	}

	public void finish()
	{
		if (collector != null) collector.stop();
	}

	public void buildEnvironment()
	{
		// build place to drag obstalces to
		OBSTACLES_X = width / 2;
		OBSTACLES_Y = height / 2;
		OBSTACLE_RADIUS = height / 2;
		Double2D loc = new Double2D(OBSTACLES_X + OBSTACLE_RADIUS, OBSTACLES_Y + OBSTACLE_RADIUS);
		final Marker obstacleDisposal = new Marker("Obstacle Disposal");
		markers.setObjectLocation(obstacleDisposal, loc);

		/*      Steppable movingHome = new Steppable() {

		        private static final long serialVersionUID = 1L;
		        double alpha = 0;

		        public void step(SimState state)
		        {
		        double nlx = HOME_X + RADIUS * Math.cos(alpha);
		        double nly = HOME_Y + RADIUS * Math.sin(alpha);

		        // alpha += 0.025;
		        alpha += 0.0125;
		        markers.setObjectLocation(homeBase, new Double2D(nlx, nly));
		        }
		        };
		        // schedule.scheduleRepeating(Schedule.EPOCH + 500, movingHome, 30);

		        Steppable movingDisposal = new Steppable() {

		        private static final long serialVersionUID = 1L;
		        double alpha = 0;

		        public void step(SimState state)
		        {
		        double nlx = OBSTACLES_X + OBSTACLE_RADIUS * Math.cos(alpha);
		        double nly = OBSTACLES_Y + OBSTACLE_RADIUS * Math.sin(alpha);

		        alpha += 0.025;
		        // alpha += 0.0125;
		        markers.setObjectLocation(obstacleDisposal, new Double2D(nlx, nly));
		        }
		        };
		        // schedule.scheduleRepeating(Schedule.EPOCH + 750, movingDisposal, 100);

		        Steppable addObstacle = new Steppable() {

		        private static final long serialVersionUID = 1L;

		        public void step(SimState state)
		        {
		        ForageHorde fHorde = (ForageHorde) state;

		        new Obstacle(fHorde, 1 + state.random.nextInt(OBSTACLE_WIDTH),
		        1 + state.random.nextInt(OBSTACLE_HEIGHT)).newRandomLocation();

		        // generate next time for obstalce
		        int time = 0;
		        double L = Math.exp(-OBSTACLE_PERIOD);
		        double p = 1;
		        do {
		        time++;
		        p = p * state.random.nextDouble();
		        } while (p > L);
		        time = time - 1;
		        state.schedule.scheduleOnce(state.schedule.getTime() + time, this);
		        }
		        };
		*/
		// schedule.scheduleOnce(OBSTACLE_PERIOD + random.nextInt(100), addObstacle);
	}

	public static void main(String[] args)
	{
		experiment = true;
		doLoop(ForageHorde.class, args);
		System.exit(0);
	}

}
