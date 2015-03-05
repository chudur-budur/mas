package sim.app.horde;

import java.util.*;

import sim.engine.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;
import sim.app.horde.classifiers.*;
import sim.app.horde.classifiers.decisiontree.*;
import sim.app.horde.classifiers.decisiontree.pruning.*;
import sim.app.horde.classifiers.knn.*;

import sim.app.horde.irl.*;

public class Horde extends SimState
{
	private static final long serialVersionUID = 1;

	// note these are not final.  This is a hack to let us change the locations
	public static String BASIC_BEHAVIORS_LOCATION = "behaviors/basic.behaviors";
	public static String BASIC_TARGETS_LOCATION = "targets/basic.targets";
	public static String BASIC_FEATURES_LOCATION = "features/basic.features";
	public static String TRAINED_MACRO_DIRECTORY = Horde.getPathInDirectory("behaviors/trained/");

	public static String getPathInDirectory(String s)
	{
		return Horde.class.getResource("").getPath()+"/" +s;
	}

	/**
	 * Called by go(...) to signal that the macro has transitioned,
	 * This in turn will cause the ButtonArray to change the state
	 * of its buttons to show the user that this has happened.
	 */
	public MacroObserver observer;

	// the default classifier is a decision tree
	public static final int METHOD_DECISION_TREE_UNPRUNED = 0;
	public static final int METHOD_DECISION_TREE_PEP = 1;
	public static final int METHOD_K_NEAREST_NEIGHBOR = 2;
	public int method = METHOD_DECISION_TREE_UNPRUNED;
	public int getMethod()
	{
		return method;
	}
	public void setMethod(int val)
	{
		method = val;
	}

	public Classifier makeNewClassifier()
	{
		switch (getMethod())
		{
		case METHOD_DECISION_TREE_UNPRUNED:
			return new DecisionTree();
		case METHOD_DECISION_TREE_PEP:
		{
			return new DecisionTree(new PessimisticErrorPruning());
		}
		case METHOD_K_NEAREST_NEIGHBOR:
			return new KNN();
		default:
			return null;  // never happens
		}
	}

	public Object getClassifierAlgorithNames()
	{
		return new String[] { "Decision Tree", "Decision Tree PEP", "K-Nearest Neighbor" };
	}

	public String describeClassifierAlgorithm()
	{
		return "Classification technique to be used for training.";
	}

	public Agent trainingAgent;

	//has the simulation started?
	public boolean started = false;

	// what level are we training agents at?
	public int trainingLevel = 0;

	public boolean defaultExamplesAreSpecial = true;
	public boolean isDefaultExamplesAreSpecial()
	{
		return defaultExamplesAreSpecial;
	}

	public void setDefaultExamplesAreSpecial(boolean val)
	{
		defaultExamplesAreSpecial = val;
	}

	public String describeDefaultExamplesAreSpecial()
	{
		return "For this behavior, should default examples be \"special\"\nthat is, "
		       + "should they consume as much classification space as possible?";
	}
	public int getTrainingLevel()
	{
		return trainingLevel;
	}
	public void setTrainingLevel(int t)
	{
		if (t >=0) trainingLevel = t;
	}
	public String describeTrainingLevel()
	{
		return "Training levels are used in multiagent hierchies.";
	}

	// used for pruning the decision trees
	// public double data = 0.0;
	public String[] provideAllSavedMacroNames()
	{
		TrainableMacro[] tm = TrainableMacro.provideAllTrainableMacros();
		String[] s = new String[tm.length];
		for(int i = 0; i < tm.length; i++)
			s[i] = tm[i].getName();
		return s;
	}

	/**
	 * Doesn't check to see if the name is valid and different from other names
	 * -- you need to check that.
	 */
	public void save(String name, javax.swing.KeyStroke stroke, int level)
	{
		getTrainingMacro().setName(name);
		getTrainingMacro().setKeyStroke(stroke);
		getTrainingMacro().setLevel(level);
		getTrainingMacro().save();
	}

	/** Doesn't check to see if the name is valid -- you need to check that. */
	public void load(String name)
	{
		trainingAgent.setBehavior(TrainableMacro.load(name));
	}

	public void showClassifiers()
	{
		if(trainingAgent != null)
		{
			TrainableMacro tm = (TrainableMacro)trainingAgent.getBehavior();
			if (tm != null)
				tm.showClassifiers(this);
			else
				System.err.println("--- Horde.showClassifiers() :"
				                   + " trainingAgent.getBehavior() == null.");
		}
		else
			System.err.println("--- Horde.showClassifiers() : trainingAgent == null." );
	}

	/** you could call this or directly call it on the macro, your choice */
	public void setRecording(boolean val)
	{
		try
		{
			// if recording == false --> learining == true
			// if recording == true --> learining == false
			getTrainingMacro().learnPolicy(this, val);
		}
		catch(Exception e)
		{
			System.err.println("--- Horde.setRecording() : trainingAgent == null, "
			                   + " something went wrong!");
			e.printStackTrace();
		}
	}

	/**
	 * Expert demos for IRL
	 */
	public ExpertDemo[] demo = new ExpertDemo[16];
	public void loadExpertDemosForIrl()
	{
		for(int i = 0 ; i < demo.length ; i++)
		{
			demo[i] = new ExpertDemo();
			demo[i].loadExpertDemo(i);
		}
	}

	/**
	 * IRL mode
	 */
	public void setIrlMode(boolean val)
	{
		try
		{
			loadExpertDemosForIrl();
			getTrainingMacro().setIrlMode(val);
			getTrainingMacro().applyIrl(this);
		}
		catch(Exception e)
		{
			System.err.println("--- Horde.setSaveSnapshot() : trainingAgent == null, "
			                   + " something went wrong!");
			e.printStackTrace();
		}
	}

	public void showPerspectiveReward()
	{
		try
		{
			loadExpertDemosForIrl();
			getTrainingMacro().showPerspectiveReward(this);
		}
		catch(Exception e)
		{
			System.err.println("--- Horde.someFunction() : trainingAgent == null, "
			                   + " something went wrong!");
			e.printStackTrace();
		}
	}

	/**
	 * Now do manual driving, no recording examples or learning models.
	 */
	public void setManualDriving(boolean val)
	{
		try
		{
			getTrainingMacro().setManualDriving(val);
		}
		catch(Exception e)
		{
			System.err.println("--- Horde.setManualDriving() : trainingAgent == null, "
			                   + " something went wrong!");
			e.printStackTrace();
		}
	}

	/**
	 * Save some custom snapshots for khaled's experiment
	 */
	public sim.app.horde.classifiers.decisiontree.pruning.HordeSnapshot hs = null ;
	public void setSaveSnapshots(boolean val)
	{
		try
		{
			if(val)
			{
				hs = new sim.app.horde.classifiers.decisiontree.pruning.HordeSnapshot();
				hs.newTrajectory();
			}
			else if(hs != null)
			{
				hs.flushTrajectories();
				hs = null ;
			}
			getTrainingMacro().setSaveSnapshots(val);
		}
		catch(Exception e)
		{
			System.err.println("--- Horde.setSaveSnapshot() : trainingAgent == null, "
			                   + " something went wrong!");
			e.printStackTrace();
		}
	}

	/**
	 * You could call this or directly call it on the macro, your choice
	 */
	public void setNewBehavior(int newBehavior)
	{
		try
		{
			System.err.println("--- Horde.setNewBehavior() :"
			                   + " will call TrainableMacro.userChangedBehavior()");
			getTrainingMacro().userChangedBehavior(this, newBehavior);
		}
		catch(Exception e)
		{
			System.err.println("--- Horde.setNewBehavior() : trainingAgent may be null.");
		}
	}

	public Agent getTrainingAgent()
	{
		return trainingAgent;
	}

	public String describeTrainingAgent()
	{
		return "The current Agent instance presently being trained.";
	}

	public TrainableMacro getTrainingMacro()
	{
		//Agent a = getTrainingAgent();
		if (trainingAgent == null)
			return null ;
		else
			return ((TrainableMacro)(trainingAgent.getBehavior()));
	}

	public String describeTrainingMacro()
	{
		return "The current TrainableMacro"
		       + " instance presently being trained.";
	}

	public Horde(long seed)
	{
		super(seed);
		System.err.println("--- Horde.Horde(seed) : called SimState(seed).");
	}

	/**
	 * Current Features, These are the features which
	 * the training macro relies on at present.
	 */
	ArrayList currentFeatures = new ArrayList();
	public void removeCurrentFeature(Feature feature)
	{
		currentFeatures.remove(feature);
	}
	public void addCurrentFeature(Feature feature)
	{
		currentFeatures.add(feature);
	}
	public void clearCurrentFeatures()
	{
		currentFeatures.clear();
	}
	public Feature[] currentFeatures()
	{
		Feature[] f = new Feature[currentFeatures.size()];
		for(int i = 0 ; i < f.length; i++)
			f[i] = (Feature)(currentFeatures.get(i));
		return f;
	}

	/**
	 * Default Features, These are the standard default
	 * features which always appear initially.
	 */
	public Feature[] defaultFeatures()
	{
		return new Feature[] { };
		// new TargetDirection(parameters[0]), new TargetDistance(parameters[0]) };
	}

	// NOTE: not sure what this does -- Sean
	public int featureLevel = 0;
	public int getFeatureLevel()
	{
		return featureLevel;
	}
	public void setFeatureLevel(int f)
	{
		if (f >= 0) featureLevel =f ;
	}

	boolean singleState = false;
	public boolean getSingleState()
	{
		return this.singleState;
	}
	public void setSingleState(boolean inputFlag)
	{
		singleState = inputFlag;
	}
	public String describeSingleState()
	{
		return "Is the behavior being trained a "
		       + "policy (single state), or is it stateful?";
	}

	// this junk is for specifying whether or not a training macro is designed to be
	// one-shot or continuous, that is, using default examples when uses in a higher-level FSA.
	// NOTE: name is different than underlying TrainableMacro because it's confusing to the
	// user, it sounds like Horde should or should not be dumping in default examples during
	// training, and that's not what this.
	public boolean usesDefaultExample()
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null)
			return tm.getShouldAddDefaultExample();
		else
			return true;
	}

	public void setUsesDefaultExample(boolean val)
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null)
			tm.setShouldAddDefaultExample(val);
	}

	public String describeUsesDefaultExample()
	{
		return "Should default examples be included during training?";
	}

	public boolean isDone()
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null)
			return tm.getFlag(tm.FLAG_DONE);
		else
			return false;
	}

	public void setDone(boolean val)
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null) tm.setFlag(tm.FLAG_DONE, val);
	}

	public String describeDone()
	{
		return "Setting of the \"Done\" flag.";
	}

	public boolean isFailed()
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null) return tm.getFlag(tm.FLAG_FAILED);
		else return false;
	}

	public void setFailed(boolean val)
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null) tm.setFlag(tm.FLAG_FAILED, val);
	}
	public String describeFailed()
	{
		return "Setting of the \"Failed\" flag.";
	}

	public int getCounter()
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null) return tm.getCounter(tm.COUNTER_BASIC);
		else return 0;
	}

	public void setCounter(int val)
	{
		TrainableMacro tm = getTrainingMacro();
		if (tm != null) tm.setCounter(tm.COUNTER_BASIC, val);
	}

	public Object getCounterRange()
	{
		return new sim.util.Interval(0L, 5L);
	}

	public String describeCounter()
	{
		return "Setting of the primary counter.";
	}

	public void start()
	{
		super.start();
		System.err.println("--- Horde.start(): staring Horde.");
		started = true;

		// reset the behavior
		resetBehavior();
	}

	// gotta have it here rather than SimHorde so reset will work
	public static final String[] initialParameterObjectName = new String[] { "A", "B", "C" };
	public Target[] buildNewParameters()
	{
		return new Target[]
		       {
		           new Parameter(0, initialParameterObjectName[0]),
		           new Parameter(1, initialParameterObjectName[1]),
		           new Parameter(2, initialParameterObjectName[2])
		       };
	}

	public void resetBehavior()
	{
		// reset the training Agent's behavior
		System.err.println("--- Horde.resetBehavior() : "
		                   + "forgetting all behaviors and resetting.");
		if (trainingAgent != null)
		{
			System.err.println("--- Horde.resetBehavior() : "
			                   + " Horde.trainingAgent != null.");
			trainingAgent.setBehavior(new
			                          TrainableMacro().reset(this, buildNewParameters(),
			                                  initialParameterObjectName,
			                                  (trainingAgent.getUnderlyingBehaviorArray() == null ?
			                                   Behavior.provideAllBehaviors(trainingAgent.level) :
			                                   trainingAgent.getUnderlyingBehaviorArray()),
			                                  currentFeatures()));
		}
		else
			System.err.println("--- Horde.resetBehavior() : "
			                   + " Horde.trainingAgent == null ??");
		distributeAndRestartBehaviors();
		// resetting snapshot stuffs
		if(hs != null)
		{
			hs.flushTrajectories();
			hs = null ;
		}
	}

	public void distributeAndRestartBehaviors()
	{
		System.err.println("--- Horde.distributeAndRestartBehaviors() : "
		                   + "distribute and restart all behaviors.");
		trainingAgent.restart(this);
	}


	/*** BEGIN HACKS FOR KEITH'S EXPERIMENT ***/
	public final static int PORT = 6000;
	public static final String STOP = "Stop";
	public static final int NUM_FEATURES = 4;
	static double[] tempfeatures = new double[NUM_FEATURES];
	public  double[] features = new double[NUM_FEATURES];
	public static java.io.PrintStream toSocket = null;
	/*
	// dunno if I gotta do these static to be easier, but why not...


	static
	{
	try
	{
	ServerSocket server = new ServerSocket(PORT);
	Socket sock = server.accept();
	final InputStream i = sock.getInputStream();
	final OutputStream o = sock.getOutputStream();

	// build the input stream to read incoming features
	Thread t = new Thread(new Runnable()
	{
	public void run()
	{
	Scanner scan = new Scanner(i);
	while(true)
	{
	// load into temporary facility, then do a quick dump to the locked array
	for(int x = 0; x < features.length; x++)
	tempfeatures[x] = scan.nextDouble();
	synchronized(features)
	{
	System.arraycopy(tempfeatures, 0, features, 0, features.length);
	}
	}
	}
	});

	t.setDaemon(true);
	t.start();

	// build the output stream
	toSocket = new PrintStream(o);
	stopAllRobots();
	}
	catch (IOException e)
	{
	throw new RuntimeException("FAILED TO OPEN AND SET UP SOCKET", e);
	}
	}

	public static void stopAllRobots() { toSocket.println(STOP); }
	*/
	/*** END HACKS FOR KEITH'S EXPERIMENT ***/
}
