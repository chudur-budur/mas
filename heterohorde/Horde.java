package sim.app.horde;

import java.util.*;
import java.io.*;

import sim.engine.*;
import sim.app.horde.agent.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.*;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;
import sim.app.horde.classifiers.*;
import sim.app.horde.classifiers.decisiontree.*;
import sim.app.horde.classifiers.decisiontree.pruning.*;
import sim.app.horde.classifiers.knn.*;

public abstract class Horde extends SimState
    {
    private static final long serialVersionUID = 1;

    // note these are not final. This is a hack to let us change the locations
    
    /** Change this class in your static { } of your Horde subclass to
        modify the locations of all the file locations relative to it. */
    public static Class locationRelativeClass = Horde.class;
    public static String BASIC_BEHAVIORS_LOCATION = "behaviors/basic.behaviors";
    public static String BASIC_TARGETS_LOCATION = "targets/basic.targets";
    public static String BASIC_FEATURES_LOCATION = "features/basic.features";
    public static String AGENT_DIRECTORY = "agent/";

    // Horde Default Arena.  Set in various main() methods
    public static String defaultArena = null;

    // All base agents.  This might be agents under a controller agent
    // or they may be 
    HashMap<String, AgentGroup> roots = new HashMap<String, AgentGroup>();
    HashMap<String, AgentGroup> coroots = new HashMap<String, AgentGroup>();

    // Add an agent to the top-level AgentGroup for roots or coroots
    public void addAgent(Agent agent, HashMap<String, AgentGroup> groups)
        {
        String name = agent.getName();
        if (!groups.containsKey(name))
            groups.put(name, new AgentGroup());  // make a dummy AgentGroup
        AgentGroup group = groups.get(name);
        group.addAgent(agent);  // implicitliy calls agent.setGroup(group)
        }        

    // Each root type can have one or more BasicInfo, so a two-deep map is created 
    // to store the BasicInfo definitions for all root types
    Map<String, Map<String, BasicInfo>> basicAlloc = new HashMap<String, Map<String, BasicInfo>>();
    Map<String, Map<String, BasicInfo>> cobasicAlloc = new HashMap<String, Map<String, BasicInfo>>();

    protected void createAgents()
        {
        boolean trainingAgentSet = false;

        trainingAgent = null;  // set the training agent to null so when setTrainingAgent(...) is called below, it's guaranteed to triger an observer.trainingAgentChanged()

        for(AgentGroup group : roots.values())
            for(Agent agent : group.getAgents())
                {
                // Even though these are agent groups they need to be handled a little
                // differently.  Only need to compute requirements once per top-level
                // controller and then builds hierarchy based on MIN, PREF, and MAX
                // constraints
                if (agent instanceof Controller)
                    {
                    Controller ctl = (Controller)agent;
                    Map<String, BasicInfo> info = basicAlloc.get(agent.getName());
                    HierarchyConstraints constraints = new HierarchyConstraints();
                        
                    List<String> types = new ArrayList<String>(info.keySet());
                    int [] basicAgentAlloc = new int[types.size()];
                    
                    for (int i = 0; i < types.size(); i++)
                        basicAgentAlloc[i] = info.get(types.get(i)).getNumBasics();
                    
                    ctl.computeRequirements(types);
                    
                    ctl.dispenseForController(types, basicAgentAlloc, constraints);
                    
                    constraints.mode = HierarchyConstraints.PREFERRED;
                    ctl.dispenseForController(types, basicAgentAlloc, constraints);
                    
                    constraints.mode = HierarchyConstraints.MAX;
                    ctl.dispenseForController(types, basicAgentAlloc, constraints);
                    }
                else 
                    agent.reload();
                
                agent.setTrainable(true);  // it's true by default, so this is cargo-cult programming right here :-)
                            
                // We schedule with an interval inverse to the height of the agent in its hierarchy
                // so that controllers are scheduled BEFORE their subsidiaries, and thus have a chance
                // to ask their subsidiaries to change behaviors before the subsidiaries are stepped.
                // See AgentGroup.fireBehaviors(...) comments
                if (!trainingAgentSet)
                    {
                    setTrainingAgent(agent);
                    trainingAgentSet = true;
                    }
                }
                                
        for(AgentGroup group : coroots.values())
            for(Agent agent : group.getAgents())
                {
                if (agent instanceof Controller)
                    {
                    Controller ctl = (Controller)agent;
                    Map<String, BasicInfo> info = cobasicAlloc.get(agent.getName());
                    HierarchyConstraints constraints = new HierarchyConstraints();
                        
                    List<String> types = new ArrayList<String>(info.keySet());
                    int [] basicAgentAlloc = new int[types.size()];
                    
                    for (int i = 0; i < types.size(); i++)
                        basicAgentAlloc[i] = info.get(types.get(i)).getNumBasics();
                    
                    ctl.computeRequirements(types);
                    
                    ctl.dispenseForController(types, basicAgentAlloc, constraints);
                    
                    constraints.mode = HierarchyConstraints.PREFERRED;
                    ctl.dispenseForController(types, basicAgentAlloc, constraints);
                    
                    constraints.mode = HierarchyConstraints.MAX;
                    ctl.dispenseForController(types, basicAgentAlloc, constraints);
                    }
                // We schedule with an interval inverse to the height of the agent in its hierarchy
                // so that controllers are scheduled BEFORE their subsidiaries, and thus have a chance
                // to ask their subsidiaries to change behaviors before the subsidiaries are stepped.
                // See AgentGroup.fireBehaviors(...) comments
                agent.setTrainable(false);  // not permitted to train coroots or cobasics
                }
        }

    // called by the reset button to COMPLETELY clear out reset the behavior of all agents and their features, and reset the buttons to reflect this
    public void reset()
        {
        TrainableMacro macro = getTrainingMacro();
        // reset the current agent, deleting all examples
        if (macro != null)
            macro.reset(this, macro.behaviors, macro.features);
        // redistribute
        distributeAndRestartBehaviors();
        if (observer != null)
            observer.resetting();
        }
        


    public static InputStream getStreamRelativeToClass(Class theClass, String s)
        {
        return theClass.getResourceAsStream(s);
        }

    public static String getPathRelativeToClass(Class theClass, String s)
        {
        return theClass.getResource("").getPath() + "/" + s;
        }

    /** Called by go(...) to signal that the macro has transitioned. This in turn will
        cause the ButtonArray to change the state of its buttons to show the user that this
        has happened.  */
    public MacroObserver observer;

    // the default classifier is a decision tree
    public static final int METHOD_DECISION_TREE_UNPRUNED = 0;
    public static final int METHOD_DECISION_TREE_PEP = 1;
    public static final int METHOD_K_NEAREST_NEIGHBOR = 2;
        
    public int method = METHOD_DECISION_TREE_UNPRUNED;
    public int getMethod() { return method; }
    public void setMethod(int val) { method = val; }
        
    public Classifier makeNewClassifier()
        {
        switch (getMethod())
            {
            case METHOD_DECISION_TREE_UNPRUNED: return new DecisionTree();
            case METHOD_DECISION_TREE_PEP: return new DecisionTree(new PessimisticErrorPruning());
            case METHOD_K_NEAREST_NEIGHBOR: return new KNN();
            default: return null; // never happens
            }
        }

    public Object domMethod() { return new String[] { "Decision Tree", "Decision Tree PEP", "K-Nearest Neighbor" }; }
    public String desMethod() { return "Classification technique to be used for training."; }

    public Agent trainingAgent;

    // has the simulation started?
    public boolean started = false;
    
    // should we rebuild the JointBehavior index caches?  This occurs
    // when the user has defined some new behaviors, or maybe has reloaded
    // a behavior. Every time their go() method is called, JointBehaviors 
    // will check here to see if their indices are out of date and they may need to rebuild
    public boolean shouldRebuildJointBehaviorIndices = true;  // always start true
    public boolean getShouldRebuildJointBehaviorIndices() { return shouldRebuildJointBehaviorIndices; }
    public void setShouldRebuildJointBehaviorIndices(boolean val) { shouldRebuildJointBehaviorIndices = val; }
    public boolean hideShouldRebuildJointBehaviorIndices() { return true; }  // don't show in inspector

    public boolean defaultExamplesAreSpecial = true;
    public boolean getDefaultExamplesAreSpecial() { return defaultExamplesAreSpecial; }
    public void setDefaultExamplesAreSpecial(boolean val) { defaultExamplesAreSpecial = val; }
    public String desDefaultExamplesAreSpecial() { return "For this behavior, should default examples be \"special\"\nthat is, should they consume as much classification space as possible?"; }
    public String nameDefaultExamplesAreSpecial() { return "SpecialDefaults"; }

    public int getCurrentTimer() { if (getTrainingMacro() != null) return getTrainingMacro().getTimer(Macro.TIMER_BASIC); else return Macro.CLEARED_TIMER; }
    public String desCurrentTimer() { return "The current timer value of the training agent."; }

    public boolean getPropagateFlags() { if (getTrainingMacro() != null) return getTrainingMacro().getPropagateFlags(); else return false; }
    public void setPropagateFlags(boolean val) { if (getTrainingMacro() != null) getTrainingMacro().setPropagateFlags(val); }
    public String desPropagateFlags() { return "Does the training macro propagate flags?"; }

	public static final int GO_ALWAYS = 0;
	public static final int GO_WHEN_IN_TRAINING_AGENT_GROUP = 1;
	public static final int GO_WHEN_TRAINING_AGENT = 2;
	
	public int shouldGo = GO_ALWAYS;
	public int getShouldGo() { return shouldGo; }
	public void setShouldGo(int val) { if (val >= GO_ALWAYS && val <= GO_WHEN_TRAINING_AGENT) shouldGo = val; }
	public Object domShouldGo() { return new String[] { "Always", "When In Group", "When Training Agent" }; }



    /** Doesn't check to see if the name is valid and different from other names -- you need to check that. */
    public void save(String name, javax.swing.KeyStroke stroke, String[] targetNames)
        {
        getTrainingMacro().setName(name);
        getTrainingMacro().setKeyStroke(stroke);
        getTrainingMacro().setTargetNames(targetNames);
        getTrainingMacro().save(getTrainingAgent());
        getTrainingMacro().setTargetNames(initialParameterObjectNames);  // restor A, B, C etc.
        }

    public void showClassifiers()
        {
        TrainableMacro tm = getTrainingMacro();
        if (tm != null) tm.showClassifiers(this);
        }

    /** you could call this or directly call it on the macro, your choice */
    public void setTraining(boolean val)
        {
        getTrainingMacro().userChangedTraining(this, val);

        // spread to other agents if val is false. For now, we just have one
        if (!val && getDistributeModel())
            distributeAndRestartBehaviors();
        }

    /** you could call this or directly call it on the macro, your choice */
    public void setNewBehavior(int newBehavior)
        {
        getTrainingMacro().userChangedBehavior(this, newBehavior);
        }

    /** This pool contains the most recently previously-trained agents of various
        names (types).  These agents hold the current examples, and so need to be transferred
        to new training agents during setTrainingAgent.
    */
    HashMap<String, Agent> trainingAgentsPool = new HashMap<String, Agent>();

    /** Returns true if the agent was actually changed. */
    public boolean setTrainingAgent(Agent agent)
        {
        // Check to see if the agent is permitted to be trainable.  All agents will call
        // setTrainingAgent regardless of whether they're trainable or not, so we need to
        // filter here.
        if (!agent.isTrainable())
            {
            return false;
            }
        else if (trainingAgent == null)  // never been set yet
            {
            trainingAgent = agent;
            if (observer != null) observer.trainingAgentChanged();
            return true;
            }
        else if (trainingAgent == agent)
            {
            // do nothing
            return false;
            }
        else //  it's a different kind of agent
            {
            trainingAgentsPool.put(trainingAgent.getName(), trainingAgent);
            trainingAgent.getBehavior().setTraining(false);  // let him go, but don't build a new model


            // set up new agent
            Agent oldAgent = trainingAgentsPool.get(agent.getName());  // this COULD be the immediately-previous agent
            if (oldAgent != null)  // there was a previously trained version of this agent
                agent.getBehavior().transferExamplesFrom(oldAgent.getBehavior());
            // don't need to state that the new agent is training

            trainingAgent = agent; // set new agent
            if (observer != null) observer.trainingAgentChanged();

            return true;
            }
        }
                
    void clearTrainingAgentsPool()
        {
        for (Agent agent : trainingAgentsPool.values())
            agent.getBehavior().clearExamples();
        trainingAgentsPool.clear();
        }

    public Agent getTrainingAgent()
        {
        return trainingAgent;
        }

    public String desTrainingAgent() { return "The current Agent instance presently being trained."; }

    public TrainableMacro getTrainingMacro()
        {
        Agent a = getTrainingAgent();
        if (a == null) return null;
        return ((TrainableMacro) (a.getBehavior()));
        }

    public String desTrainingMacro() { return "The current TrainableMacro instance presently being trained."; }

    public Horde(long seed)
        {
        super(seed);
        }

    public boolean isSingleState() { return (trainingAgent == null) || trainingAgent.getBehavior().isSingleState(); }
    public void setSingleState(boolean inputFlag) 
        { 
        if (trainingAgent != null)
            trainingAgent.getBehavior().setSingleState(inputFlag); 
        }
    public String desSingleState() { return "Is the behavior being trained a policy (single state), or is it stateful?"; }



    // this junk is for specifying whether or not a training macro is designed to be
    // one-shot or continuous, that is, using default examples when uses in a
    // higher-level FSA.
    // NOTE: name is different than underlying TrainableMacro because it's
    // confusing to the
    // user, it sounds like Horde should or should not be dumping in default
    // examples during
    // training, and that's not what this.
    public boolean getUsesDefaultExample()
        {
        TrainableMacro tm = getTrainingMacro();
        if (tm != null) return tm.getShouldAddDefaultExample();
        else return true;
        }

    public void setUsesDefaultExample(boolean val)
        {
        TrainableMacro tm = getTrainingMacro();
        if (tm != null) tm.setShouldAddDefaultExample(val);
        }

    public String desUsesDefaultExample() { return "Should default examples be included during training?"; }
    public String nameUsesDefaultExample() { return "UseDefaults"; }

    public boolean isDone()
        {
        TrainableMacro tm = getTrainingMacro();
        if (tm != null) return tm.getFlag(tm.FLAG_DONE);
        else return false;
        }

    /** Sets the failed flag.  Does not propagate. */
    public void setDone(boolean val)
        {
        TrainableMacro tm = getTrainingMacro();
        if (tm != null) tm.setFlag(tm.FLAG_DONE, val, false);
        }

    public String desDone() { return "Setting of the \"Done\" flag.  No propagation."; }

    public boolean isFailed()
        {
        TrainableMacro tm = getTrainingMacro();
        if (tm != null) return tm.getFlag(tm.FLAG_FAILED);
        else return false;
        }

    /** Sets the failed flag.  Does not propagate. */
    public void setFailed(boolean val)
        {
        TrainableMacro tm = getTrainingMacro();
        if (tm != null) tm.setFlag(tm.FLAG_FAILED, val, false);
        }

    public String desFailed() { return "Setting of the \"Failed\" flag.  No propagation."; }

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

    public Object domCounter()
        {
        return new sim.util.Interval(0L, 5L);
        }

    public String desCounter() { return "Setting of the primary counter."; }

    public void start()
        {
        super.start();
                
        setShouldRebuildJointBehaviorIndices(true); // for good measure
                
        started = true;

        clearAgents();
        createAgents(); // override this in subclasses
        scheduleAgents();
        }

    private void scheduleAgents()
        {
        for (AgentGroup grp : roots.values()) grp.scheduleAgents(this);
        for (AgentGroup grp : coroots.values()) grp.scheduleAgents(this);
        }
    
    // gotta have it here rather than SimHorde so reset will work
    public static final String[] initialParameterObjectNames = new String[] { "A", "B", "C", "D", "E", "F", "G", "H" };

    public Target[] buildNewParameters() 
        { 
            
        return new Target[] 
            { 
            new Parameter(0, initialParameterObjectNames[0]),
            new Parameter(1, initialParameterObjectNames[1]),
            new Parameter(2, initialParameterObjectNames[2]),
            new Parameter(3, initialParameterObjectNames[3]),
            new Parameter(4, initialParameterObjectNames[4]),
            new Parameter(5, initialParameterObjectNames[5]), 
            new Parameter(6, initialParameterObjectNames[6]), 
            new Parameter(7, initialParameterObjectNames[7]) 
            };
        }

    /** This is the master list of all agents in the simulation, hashed into buckets by agent name (type) */
    public HashMap<String, ArrayList<Agent>> allAgents = new HashMap<String, ArrayList<Agent>>();

    public void clearAgents() { allAgents.clear(); clearTrainingAgentsPool(); }
    public void addAgent(Agent agent)
        {
        ArrayList<Agent> agents = allAgents.get(agent.getName());
        if (agents == null)
            {
            agents = new ArrayList<Agent>();
            allAgents.put(agent.getName(), agents);
            }
        agents.add(agent);
        agent.stoppable = schedule.scheduleRepeating(agent, 0 - agent.maxLeafDistance(), 1.0);
        agent.restart(this);  // make sure it's in INITIAL_BEHAVIOR
        }
    
    boolean distributeModel = true;
    public void setDistributeModel(boolean val) { distributeModel = val; }
    public boolean getDistributeModel() { return distributeModel; }
    public String desDistributeModel() { return "When the agent is trained, should the result be distributed to other agents?\nOnly uncheck this for testing purposes."; }

    /** Distributes and restarts behaviors of all agents like the training agent */
    public void distributeAndRestartBehaviors()
        {
        setShouldRebuildJointBehaviorIndices(true); // for good measure?

        // Restart to like agents
        ArrayList<Agent> agents = allAgents.get(trainingAgent.getName());
                        
        for(int i = 0; i < agents.size(); i++)
            {
            Agent other = agents.get(i);
            if (!other.equals(trainingAgent))
                other.setBehavior((TrainableMacro)(trainingAgent.getBehavior().clone()));
            other.restart(this);
            }
        }


    /// This code resets features in the underlying training agent
        
    /** Current Features.  These are the features which the training macro relies on at present. */
    public void removeCurrentFeature(Feature feature)
        {
        int index = -1;
        Feature[] features = getTrainingMacro().features;

        for(int i = 0; i < features.length; i++)
            {
            if (features[i] == feature)  // must be pointer-equivalent
                { 
                if (index != -1)  // uh oh
                    System.err.println("Multiple instances of same feature (" + feature + ").  This shouldn't happen.  Removing first such instance.");
                else 
                    index = i; 
                }
            }
                        
        if (index==-1)  // uh oh
            throw new RuntimeException("No such feature found: " + feature);
                
        Feature[] newFeatures = new Feature[features.length - 1];
        int count = 0;
        for (int i = 0; i < features.length; i++)
            if (i != index)
                {
                newFeatures[count] = features[i];
                count++;
                }
                        
        // reset the macro to the revised features.  This will involve deleting
        // all current examples and resetting the current model, oh well.
        getTrainingMacro().reset(this, getTrainingMacro().behaviors, newFeatures);
        
        // redistribute to all the like-minded agents.  This will basically lobotomize them all.
        distributeAndRestartBehaviors();
        }
        
    /** If 'user' is true, then the feature is being added by the user,
        else it's just being added as part of constructing the initial
        AddList. */
    public void addCurrentFeature(Feature feature, boolean user)
        {
        Feature[] features = getTrainingMacro().features;
        Feature[] newFeatures = new Feature[features.length + 1];
        System.arraycopy(features, 0, newFeatures, 0, features.length);
        newFeatures[features.length] = feature;
        
        if (user)
            {
            // reset the macro to the revised features.  This will involve deleting
            // all curret examples and resetting the current model, oh well.
            getTrainingMacro().reset(this, getTrainingMacro().behaviors, newFeatures);
                
            // redistribute to all the like-minded agents.  This will basically lobotomize them all.
            distributeAndRestartBehaviors();
            }
        }

    public void clearCurrentFeatures()
        {
        // reset the macro to the revised features.  This will involve deleting
        // all curret examples and resetting the current model, oh well.
        getTrainingMacro().reset(this, getTrainingMacro().behaviors, new Feature[0]);
        
        // redistribute to all the like-minded agents.  This will basically lobotomize them all.
        distributeAndRestartBehaviors();
        }
    }
        
