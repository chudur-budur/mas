package sim.app.horde;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import sim.app.horde.agent.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.agent.SimAgent;
import sim.app.horde.behaviors.*;
import sim.app.horde.objects.*;
import sim.field.continuous.*;
import sim.field.grid.*;
import sim.util.*;

public class SimHorde extends Horde
    {
    private static final long serialVersionUID = 1;

    public static final String ARENA_DIRECTORY = "arenas/";
    public static String ARENA_EXTENSION = ".arena";
    public static String DEFAULT_ARENA = "default" + ARENA_EXTENSION;

    // width and height of the environment
    public double width = 100;
    public double height = 100;

    // No obstacles should be larger than this size
    public static final double MAX_OBSTACLE_DIAMETER = 25;

    public static final boolean ALLOW_COLLISIONS_WITH_AGENTS = false;

    // objects come in two size categories: SMALL/MEDIUM and ARBITRARILY LARGE
    // SMALL/MEDIUM object may be detected within a certain range, depending on
    // the object.
    public double SMALL_CLOSE_RANGE = 1.0;
    public double SMALL_MEDIUM_RANGE = 5.0;
    public double SMALL_FAR_RANGE = 10.0;
    public double SMALL_FULL_RANGE = Math.max(width, height);

    // ARBITRARILY LARGE are meant to be relatively sparse, and so they are
    // globally detectable
    public double LARGE_FULL_RANGE = Math.max(width, height);
    
    public boolean hasControllers;

    // fields
    public IntGrid2D ground = new IntGrid2D((int)width + 1, (int)height + 1);
    public DoubleGrid2D food = new DoubleGrid2D((int)width + 1, (int)height + 1);
    public Continuous2D agents = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);
    public Continuous2D obstacles = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
    public Continuous2D regions = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
    public Continuous2D markers = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);
    public Set<Marker> markerTable = new HashSet<Marker>();

    /**
     * Converts a floating-point coordinate into a grid cell for the ground or
     * floor
     */
    public int gridX(double x)
        {
        return (int) (x + 0.5);
        } // we presume that x is positive, else the proper equation is Math.floor(x+0.5);

    /**
     * Converts a floating-point coordinate into a grid cell for the ground or
     * floor
     */
    public int gridY(double y)
        {
        return (int) (y + 0.5);
        } // we presume that y is positive, else the proper equation is Math.floor(y+0.5);

    // mouse location in the horde world
    public Double2D mouseLoc;

    public SimHorde(long seed)
        {
        super(seed);
        setArena(Horde.defaultArena);
        }

    Targetable defaultTargetable = new Targetable()
        {
        public Double2D getTargetLocation(Agent agent, Horde horde)
            {
            return mouseLoc;
            }

        public int getTargetStatus(Agent agent, Horde horde)
            {
            return 0;
            }

        public int getTargetRank(Agent agent, Horde horde)
            {
            return -1;
            }

        public boolean getTargetIntersects(Agent agent, Horde horde, Double2D location, double slopSquared)
            {
            return getTargetLocation(agent, horde).distanceSq(location) <= slopSquared;
            }

        public void setParameterValue(int index) {}
        public void setTargetLocation(Agent agent, Horde horde, Double2D location) {}
        public void setTargetStatus(Agent agent, Horde horde, int status) {}
        public void setTargetRank(Agent agent, Horde horde, int rank) {}
        };

    public static final Color[] parameterObjectColor = new Color[] { Color.red, Color.blue, Color.green, Color.magenta, Color.cyan, Color.yellow, new Color(139,90,43), Color.pink };
    public static final String[] parameterObjectColorName = new String[] { "#FF0000", "#0000FF", "#00FF00", "#FF00FF", "#00FFFF", "#FFFF00", "#8B5A2B", "#FF69B4" };
    //{ "Red", "Blue", "Green", "Magenta", "Cyan", "Yellow", "Brown", "Pink" };

    Targetable[] parameterObject = new Targetable[] { defaultTargetable, defaultTargetable, defaultTargetable, defaultTargetable, defaultTargetable, defaultTargetable, defaultTargetable, defaultTargetable };

    public Targetable getParameterObject(int i)
        {
        return parameterObject[i];
        }
        
    
    /** Returns a paint color for a given token. */
    public Paint scanPaint(String token)
        {
        token = token.trim().toLowerCase();
        if (token.length() == 0)
            {
            return null;
            }
        else if (token.contains("."))
            {
            int[] vals = new int[3]; 
            String[] c = token.split("\\.");
            if (c == null || c.length != 3)
                throw new RuntimeException("A. Bad color token: " + token);
            for(int i = 0; i < c.length; i++)
                {
                try { vals[i] = Integer.parseInt(c[i]); }
                catch (NumberFormatException e) { throw new RuntimeException("B. Bad color token: " + token, e); }
                if (vals[i] < 0 || vals[i] > 255)
                    throw new RuntimeException("C. Bad color token: " + token); 
                }
            return new Color(vals[0], vals[1], vals[2]);
            }
        else
            {
            for(int i = 0; i < AgentPortrayal2D.colorNames.length; i++)
                if (AgentPortrayal2D.colorNames[i].equals(token))
                    return AgentPortrayal2D.predefinedColors[i];
            throw new RuntimeException("Unknown color name in token: " + token);
            }
        }
    
    public void setParameterObject(int i, Targetable obj)
        {
        // find the targetable if it's already a parameter
        int foundObj = -1;
        for (int j = 0; j < parameterObject.length; j++)
            if (parameterObject[j] == obj)
                {
                foundObj = j;
                break;
                }

        // CASE 1:
        // obj is not a parameter. Make it a parameter and
        // replace the old one
        if (foundObj == -1)
            {
            parameterObject[i].setParameterValue(-1);
            obj.setParameterValue(i);
            parameterObject[i] = obj;
            }
        // CASE 2:
        // obj is a parameter already. Swap with the existing parameter
        else
            {
            Targetable old = parameterObject[i];
            old.setParameterValue(foundObj);
            obj.setParameterValue(i);
            parameterObject[i] = obj;
            parameterObject[foundObj] = old;
            }
        }


    public Marker getMarker(String name)
        {
        Iterator<Marker> im = markerTable.iterator();
                
        while (im.hasNext())
            {
            Marker m = im.next();
            if (m.toString().equals(name))
                return m;
            }
                
        return null;
        }
    
    
    public static String[] provideAllArenaFilenames()
        {
        try
            {
            // reload
            FilenameFilter pbmonly = new FilenameFilter()
                {
                public boolean accept(File dir, String name) { return name.endsWith(ARENA_EXTENSION); }
                };

            // Find and load all arenas
            String[] names = new File(getPathRelativeToClass(locationRelativeClass, ARENA_DIRECTORY)).list(pbmonly);
            if (names == null) names = new String[0];
            return names;
            }
        catch (Exception e)
            {
            System.err.println("Could not provide arena filenames: " + e);
            return new String[0];
            }
        }
    
    void loadArena(String resource)
        {
        Scanner scan = null;
        HashMap<String,ec.util.ParameterDatabase> databases = new HashMap<String,ec.util.ParameterDatabase>();
        
        // clear out agent lists
        
        roots = new HashMap<String, AgentGroup>();
        coroots = new HashMap<String, AgentGroup>();
        
        try
            {
            scan = new Scanner(new InputStreamReader(locationRelativeClass.getResourceAsStream(resource), "UTF-8"));
            //scan = new Scanner(new InputStreamReader(SimHorde.class.getResourceAsStream(resource), "UTF-8"));
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
            if (tok.startsWith("#")) // comment
                {
                scan.nextLine();
                continue;
                }
            // size is: size width height
            else if (tok.equals("size"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                double w = vscan.nextDouble();
                double h = vscan.nextDouble();
                agents.width = regions.width = markers.width = obstacles.width = width = w;
                agents.height = regions.height = markers.height = obstacles.height = height = h;
                }
            // Food grid is: food filename
            else if (tok.equals("food"))
                {
                String val = scan.nextLine().trim();
                InputStream str = SimHorde.class.getResourceAsStream(val);
                double[][] res = null;
                try
                    {
                    if (val.endsWith(".pgm") || val.endsWith(".pbm") || val.endsWith(".PGM")
                        || val.endsWith(".PBM"))
                        ; // food.setTo(TableLoader.convertToDoubleArray(TableLoader.loadPNMFile(str,
                    // true)));
                    /*
                     * ^ | --- the above line has problem, need to fix. I
                     * commented out to test the pruning code -khaled
                     */
                    else
                        food.setTo(TableLoader.loadTextFile(str, true));
                    } catch (Exception e)
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
                    if (val.endsWith(".pgm") || val.endsWith(".pbm") || val.endsWith(".PGM")
                        || val.endsWith(".PBM"))
                        ground.setTo(TableLoader.loadPNMFile(str, true));
                    else if (val.endsWith(".png") || val.endsWith(".gif"))
                        ; // ground.setTo(TableLoader.loadPNGFile(str)); //<--
                    // this line has problem, need to fix. -khaled
                    else
                        ground.setTo(TableLoader.convertToIntArray(TableLoader.loadTextFile(str,
                                    true)));
                    } catch (Exception e)
                    {
                    System.err.println("Could not load file defining the ground grid");
                    e.printStackTrace();
                    }
                }
            // Markers are: marker x y targetableIndex color name
            // if targetableIndex < 0, it's not initially targetable
            else if (tok.equals("marker"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
                int index = vscan.nextInt();
                Paint paint = scanPaint(vscan.next());
                String name = vscan.nextLine().trim();  // the rest of the line is the name

                Marker marker = new Marker(name);
                marker.setDefaultPaint(paint);
                markerTable.add(marker);
                markers.setObjectLocation(marker, loc);
                if (index >= 0) setParameterObject(index, marker);
                }
            // Circles are: circle x y targetableIndex diameter field color 
            // fields are: 0=obstacles 1=regions 2=immovable obstacles 3=immovable regions
            // if targetableIndex < 0, it's not initially targetable
            else if (tok.equals("circle"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
                int index = vscan.nextInt();
                double diameter = vscan.nextDouble();
                int f = vscan.nextInt();
                Continuous2D field = (f % 2 == 0 ? obstacles : regions);
                Paint paint = scanPaint(vscan.next());

                CircularBody body = new CircularBody(diameter, field, loc);
                body.setMovable(f / 2 == 0);
                body.setHittable(f != 3);
                body.setDefaultPaint(paint);
                field.setObjectLocation(body, loc);
                if (index >= 0) setParameterObject(index, body);
                }
            // Rects are: rect x y targetableIndex width height field color
            // fields are: 0=obstacles 1=regions 2=immovable obstacles 3=immovable and un-hittable/unselectable regions
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
                Continuous2D field = (f % 2  == 0 ? obstacles : regions);
                Paint paint = scanPaint(vscan.next());

                RectangularBody body = new RectangularBody(width, height, field, loc);
                body.setMovable(f / 2 == 0);
                body.setHittable(f != 3);
                body.setDefaultPaint(paint);
                field.setObjectLocation(body, loc);
                if (index >= 0) setParameterObject(index, body);
                }
            // SquareCircles are: squarecircle x y targetableIndex field color
            // fields are: 0=obstacles 1=regions 2=immovable obstacles 3=immovable regions
            // if targetableIndex < 0, it's not initially targetable
            // This is just a hack to get the square-circle shape loadable for
            // now
            else if (tok.equals("squarecircle"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
                int index = vscan.nextInt();
                int f = vscan.nextInt();
                Continuous2D field = (f % 2 == 0 ? obstacles : regions);
                Paint paint = scanPaint(vscan.next());

                Area r = new Area(new Rectangle2D.Double(0, 0, 10, 10));
                Area r2 = new Area(new Ellipse2D.Double(5, 5, 15, 15));
                r.add(r2);

                Body body = new Body(r, field, loc);
                body.setMovable(f / 2 == 0);
                body.setHittable(f != 3);
                body.setDefaultPaint(paint);
                field.setObjectLocation(body, loc);
                if (index >= 0) setParameterObject(index, body);
                }
            // Text is: text x y targetableIndex field color fontsize string
            // fields are: 0=obstacles 1=regions 2=immovable obstacles 3=immovable regions
            // The string can have whitespace inside
            // if targetableIndex < 0, it's not initially targetable
            else if (tok.equals("text"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
                int index = vscan.nextInt();
                int f = vscan.nextInt();
                Continuous2D field = (f % 2 == 0 ? obstacles : regions);
                Paint paint = scanPaint(vscan.next());
                int fontSize = vscan.nextInt();
                String string = vscan.nextLine().trim();

                Body body = new Body(string, fontSize, field, loc);
                body.setMovable(f / 2 == 0);
                body.setHittable(f != 3);
                body.setDefaultPaint(paint);
                field.setObjectLocation(body, loc);
                if (index >= 0) setParameterObject(index, body);
                }
            // Agent allocation is: rootName name num x y height width
            else if (tok.equals("cobasic"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                String rootName = vscan.next();
                String agentName = vscan.next();

                // get database
                ec.util.ParameterDatabase db = null;
                if (databases.containsKey(agentName))
                    db = databases.get(agentName);
                else
                    {
                    db = Agent.getDatabase(agentName);
                    databases.put(agentName, db);
                    }

                int num = vscan.nextInt();
                double x = vscan.nextDouble();
                double y = vscan.nextDouble();
                double w = vscan.nextDouble();
                double h = vscan.nextDouble();
                                
                BasicInfo info = defineBasicInfo(cobasicAlloc, rootName, agentName);
                info.numBasics = num;
                info.xPos = x;
                info.yPos = y;
                info.height = h;
                info.width = w;
                }
            // Agent allocation is: rootName name num x y height width
            else if (tok.equals("basic"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                String rootName = vscan.next();
                String agentName = vscan.next();
                
                // get database
                ec.util.ParameterDatabase db = null;
                if (databases.containsKey(agentName))
                    db = databases.get(agentName);
                else
                    {
                    db = Agent.getDatabase(agentName);
                    databases.put(agentName, db);
                    }

                int num = vscan.nextInt();
                double x = vscan.nextDouble();
                double y = vscan.nextDouble();
                double w = vscan.nextDouble();
                double h = vscan.nextDouble();
                                
                BasicInfo info = defineBasicInfo(basicAlloc, rootName, agentName);
                info.numBasics = num;
                info.xPos = x;
                info.yPos = y;
                info.height = h;
                info.width = w;
                }
            // Root is: root name       [for controller agents]
            // Or: root name number [for controller agents]
            // Or: root name x y orientation
            // Or: root name number x y width height
            else if (tok.equals("root"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                String agentName = vscan.next();
                
                // get database
                ec.util.ParameterDatabase db = null;
                if (databases.containsKey(agentName))
                    db = databases.get(agentName);
                else
                    {
                    db = Agent.getDatabase(agentName);
                    databases.put(agentName, db);
                    }

                if (vscan.hasNextDouble())      
                    {
                    double x = vscan.nextDouble();
                    if (vscan.hasNextDouble())
                        {
                        BasicInfo info = defineBasicInfo(basicAlloc, agentName, agentName);                     

                        double y = vscan.nextDouble();
                        double o = vscan.nextDouble();
                        if (vscan.hasNextDouble())
                            {
                            // root name number x y width height
                            info.numBasics = (int)x;
                            info.xPos = y;
                            info.yPos = o;
                            info.width = vscan.nextDouble();
                            info.height = vscan.nextDouble();

                            for(int i = 0; i < info.numBasics; i++)
                                {
                                SimAgent agent = (SimAgent)(Agent.provideAgent(db, null, agentName, this));
                                addAgent(agent, roots);
                                }
                            }
                        else
                            {
                            // root name x y orientation
                            info.numBasics += 1;
                            info.xPos = x;
                            info.yPos = y;
                            info.height = height;
                            info.width = width;

                            SimAgent agent = (SimAgent)(Agent.provideAgent(db, null, agentName, this));
                            agent.setLocation(new Double2D(x,y));
                            agent.setOrientation(o);
                            addAgent(agent, roots);
                            }
                        }
                    else
                        {
                        // root name number
                        double num = x;
                        for(int i = 0; i < num; i++)
                            {
                            SimControllerAgent agent = (SimControllerAgent)(Agent.provideAgent(db, null, agentName, this));
                            addAgent(agent, roots);
                            hasControllers = true;
                            }
                        }
                    }
                else
                    {
                    // root name 
                    SimControllerAgent agent = (SimControllerAgent)(Agent.provideAgent(db, null, agentName, this));
                    addAgent(agent, roots);
                    hasControllers = true;
                    }
                }
            // Coroot is: coroot name behavior  [for controller agents]
            // Or: coroot name behavior number [for controller agents]
            // Or: coroot name behavior x y orientation
            // Or: coroot name behavior number x y width height
            else if (tok.equals("coroot"))
                {
                String val = scan.nextLine().trim();
                Scanner vscan = new Scanner(val);
                String agentName = vscan.next();
                
                // get database
                ec.util.ParameterDatabase db = null;
                if (databases.containsKey(agentName))
                    db = databases.get(agentName);
                else
                    {
                    db = Agent.getDatabase(agentName);
                    databases.put(agentName, db);
                    }

                String agentBehavior = vscan.next();

                if (vscan.hasNextDouble())      
                    {
                    double x = vscan.nextDouble();
                    if (vscan.hasNextDouble())
                        {
                        BasicInfo info = defineBasicInfo(cobasicAlloc, agentName, agentName);                   

                        double y = vscan.nextDouble();
                        double o = vscan.nextDouble();
                        if (vscan.hasNextDouble())
                            {
                            // root name behavior number x y width height
                            info.numBasics = (int)x;
                            info.xPos = y;
                            info.yPos = o;
                            info.width = vscan.nextDouble();
                            info.height = vscan.nextDouble();
                                        
                            for(int i = 0; i < info.numBasics; i++)
                                {
                                SimAgent agent = (SimAgent)(Agent.provideAgent(db, null, agentName, this));
                                agent.getBehavior().userChangedBehavior(this, agent.indexOfBehavior(agentBehavior));
                                addAgent(agent, coroots);
                                // don't think we should also say agents.add(agent)
                                }
                            }
                        else
                            {
                            // root name behavior x y orientation
                            info.numBasics += 1;
                            info.xPos = x;
                            info.yPos = y;
                            info.height = height;
                            info.width = width;

                            SimAgent agent = (SimAgent)(Agent.provideAgent(db, null, agentName, this));
                            agent.getBehavior().userChangedBehavior(this, agent.indexOfBehavior(agentBehavior));
                            agent.setLocation(new Double2D(x,y));
                            agent.setOrientation(o);
                            addAgent(agent, coroots);
                            // don't think we should also say agents.add(agent)
                            }
                        }
                    else
                        {
                        // root name behavior number
                        double num = x;
                        for(int i = 0; i < num; i++)
                            {
                            SimControllerAgent agent = (SimControllerAgent)(Agent.provideAgent(db, null, agentName, this));
                            agent.getBehavior().userChangedBehavior(this, agent.indexOfBehavior(agentBehavior));
                            addAgent(agent, coroots);
                            hasControllers = true;
                            }
                        }
                    }
                else
                    {
                    // root name behavior
                    SimControllerAgent agent = (SimControllerAgent)(Agent.provideAgent(db, null, agentName, this));
                    agent.getBehavior().userChangedBehavior(this, agent.indexOfBehavior(agentBehavior));
                    addAgent(agent, coroots);
                    hasControllers = true;
                    }
                }
            // This must be a custom class with its own initFromParamString method
            else
                {
                try
                    {
                    int numObjs = Integer.valueOf(tok);
                    String className = scan.next().trim();
                    String params = scan.nextLine().trim();
                                        
                    Class theClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
                    Constructor constructor = theClass.getConstructor(new Class[] { SimHorde.class, String.class});
                                        
                    for (int i = 0; i < numObjs; i++)
                        constructor.newInstance(this, params);
                    } 
                catch (Exception e)
                    {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    }
                }
            }
        scan.close();
        }

    private BasicInfo defineBasicInfo(Map<String, Map<String,BasicInfo>> alloc, String rootName, String agentName)
        {
        Map<String, BasicInfo> rootAlloc = alloc.get(rootName);
                
        if (rootAlloc == null)
            {
            rootAlloc = new HashMap<String, BasicInfo>();
            basicAlloc.put(rootName, rootAlloc);
            }
                
        BasicInfo info = rootAlloc.get(agentName);
                
        if (info == null)
            {
            info = new BasicInfo();
            info.basicType = agentName;
            rootAlloc.put(agentName, info);
            }
        else
            System.err.println("WARNING: Agents of type " + agentName + 
                " already allocated for root type " + rootName + 
                " Previously defined values will be lost");
        return info;
        }
        
    public void start()
        {
        // set up the agents field
        agents = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);
        obstacles = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
        regions = new Continuous2D(LARGE_FULL_RANGE * 1.5, width, height);
        markers = new Continuous2D(SMALL_MEDIUM_RANGE * 1.5, width, height);

        loadArena(ARENA_DIRECTORY + arena);

        super.start();
        
        initPose(roots, basicAlloc);
        }

    /*
     * Initialize the pose of all agents and add basics to the leaf structure
     */
    private void initPose(HashMap<String, AgentGroup> roots,
        Map<String, Map<String, BasicInfo>> bInfo)
        {
        for (String rootType : roots.keySet())
            {
            AgentGroup group = roots.get(rootType);
            Map<String, BasicInfo> info = bInfo.get(rootType);
                        
            for (Agent agent : group.getAgents())
                if (agent instanceof SimControllerAgent)
                    {
                    ((SimControllerAgent)agent).initPose(info);
                    }
                else
                    {
                    // If it's a SimAgent, and the location isn't already set set the location
                    if (agent instanceof SimAgent && ((SimAgent)agent).getLocation().x == 0 && ((SimAgent)agent).getLocation().y == 0)
                        {
                        BasicInfo b = info.get(agent.getName());

                        if (b == null) throw new RuntimeException("HierarchyConstraints: No info available for basic type " + agent.getName());
                                                
                        ((SimAgent)agent).setLocation(new Double2D(random.nextDouble() * b.width + b.xPos, random.nextDouble() * b.height + b.yPos));
                        ((SimAgent)agent).setOrientation(random.nextDouble() * Math.PI * 2);
                        }
                    }
            }
                
        }

    public void stop()
        {
        // reload the arenas
        arenas = provideAllArenaFilenames();
        }

    String[] arenas = provideAllArenaFilenames();
    int arenaIndex = 0;
    String arena = arenas[arenaIndex];
        
    public int getArena() { return arenaIndex; }
    public void setArena(int val) { arenaIndex = val; arena = arenas[arenaIndex]; }
    public void setArena(String name)
        {
        if (name == null) return;
        for(int i = 0; i < arenas.length; i++)
            if (arenas[i].toLowerCase().trim().equals(name.toLowerCase().trim()))
                { setArena(i); return; }
        throw new RuntimeException("Unknown arena name " + name);
        }
                
    public Object domArena() { return arenas; }
    public String desArena() { return "The arena is only changed when STOP is pressed."; }


    public static void main(String[] args)
        {
        if (args.length > 0)
            Horde.defaultArena = args[0];
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
