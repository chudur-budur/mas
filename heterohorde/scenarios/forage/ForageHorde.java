package sim.app.horde.scenarios.forage;

import java.util.List;

import sim.app.horde.Horde;
import sim.app.horde.SimHorde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Behavior;
import sim.app.horde.behaviors.TrainableMacro;
import sim.app.horde.objects.Marker;
import sim.util.Double2D;

public class ForageHorde extends SimHorde
    {
    public int HOME_X = 75;
    public int HOMaE_Y = 75;
    public int RADIUS = 45;

    public int OBSTACLES_X = 50;
    public int OBSTACLES_Y = 50;
    public int OBSTACLE_RADIUS = 25;

    // mean of poisson distribution for arrival of obstacles
    public int OBSTACLE_PERIOD = 500;

    // maximum size of obstacles
    public int OBSTACLE_WIDTH = 15;
    public int OBSTACLE_HEIGHT = 15;

    public static int collectedBoxes = 0;
    public static int collectedWeight = 0;

    public static boolean experiment = false;
    Behavior foragerBehavior = null;

    static
        {
        locationRelativeClass = ForageHorde.class;
        }
        
    protected DataCollection collector = null;

    public int getCollectedBoxes()
        {
        return collectedBoxes;
        }

    static int cnt = 0;
        
    private static final long serialVersionUID = 1L;

    public ForageHorde(long seed)
        {
        super(seed);
        }

    public void startBypass()
        {
        super.start();
        }

    public void start()
        {
        //arena = "scenarios/forage/arenas/basic.arena.txt";
        //arena = "scenarios/forage/arenas/L1.arena.txt";
        //arena = "scenarios/forage/arenas/L2.arena.txt";
        //arena = "scenarios/forage/arenas/L3.arena.txt";
        //arena = "scenarios/forage/arenas/L4.arena.txt";

        //schedule.reset();
                
        super.start();
        collectedBoxes = 0;
        collectedWeight = 0;
        //resetBehavior();

        // collector = new DataCollection(this, "keith-trained-data-",
        // INIT_NUM_AGENTS_PER);
        // collector.stoppable = schedule.scheduleRepeating(collector, 2, 1);

        if (experiment)
            {
            List<Agent> agentList = allAgents.get("Forager");
                        
            for (int i = 0; i < agentList.size(); i++)
                {
                TrainableMacro m = (TrainableMacro) (agentList.get(i)
                    .getBehavior());
                m.userChangedBehavior(this,
                    m.indexOfBehaviorNamed("SwarmForage"));
                m.startBehavior = m.indexOfBehaviorNamed("SwarmForage");

                // m.userChangedBehavior(this,
                // m.indexOfBehaviorNamed("GroupForage"));
                // m.startBehavior = m.indexOfBehaviorNamed("GroupForage");
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
        OBSTACLES_X = (int)(width / 2);
        OBSTACLES_Y = (int)(height / 2);
        OBSTACLE_RADIUS = (int)(height / 2);
        Double2D loc = new Double2D(OBSTACLES_X + OBSTACLE_RADIUS, OBSTACLES_Y
            + OBSTACLE_RADIUS);
        final Marker obstacleDisposal = new Marker("Obstacle Disposal");
        markers.setObjectLocation(obstacleDisposal, loc);

        /*
         * Steppable movingHome = new Steppable() {
         * 
         * private static final long serialVersionUID = 1L; double alpha = 0;
         * 
         * public void step(SimState state) { double nlx = HOME_X + RADIUS *
         * Math.cos(alpha); double nly = HOME_Y + RADIUS * Math.sin(alpha);
         * 
         * // alpha += 0.025; alpha += 0.0125;
         * markers.setObjectLocation(homeBase, new Double2D(nlx, nly)); } }; //
         * schedule.scheduleRepeating(Schedule.EPOCH + 500, movingHome, 30);
         * 
         * Steppable movingDisposal = new Steppable() {
         * 
         * private static final long serialVersionUID = 1L; double alpha = 0;
         * 
         * public void step(SimState state) { double nlx = OBSTACLES_X +
         * OBSTACLE_RADIUS * Math.cos(alpha); double nly = OBSTACLES_Y +
         * OBSTACLE_RADIUS * Math.sin(alpha);
         * 
         * alpha += 0.025; // alpha += 0.0125;
         * markers.setObjectLocation(obstacleDisposal, new Double2D(nlx, nly));
         * } }; // schedule.scheduleRepeating(Schedule.EPOCH + 750,
         * movingDisposal, 100);
         * 
         * Steppable addObstacle = new Steppable() {
         * 
         * private static final long serialVersionUID = 1L;
         * 
         * public void step(SimState state) { ForageHorde fHorde = (ForageHorde)
         * state;
         * 
         * new Obstacle(fHorde, 1 + state.random.nextInt(OBSTACLE_WIDTH), 1 +
         * state.random.nextInt(OBSTACLE_HEIGHT)).newRandomLocation();
         * 
         * // generate next time for obstalce int time = 0; double L =
         * Math.exp(-OBSTACLE_PERIOD); double p = 1; do { time++; p = p *
         * state.random.nextDouble(); } while (p > L); time = time - 1;
         * state.schedule.scheduleOnce(state.schedule.getTime() + time, this); }
         * };
         */
        // schedule.scheduleOnce(OBSTACLE_PERIOD + random.nextInt(100),
        // addObstacle);
        }

    public static void main(String[] args)
        {
        experiment = true;
        doLoop(ForageHorde.class, args);
        System.exit(0);
        }

    }
