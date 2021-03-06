/*
 * Copyright 2006 by Sean Luke and George Mason University Licensed under the Academic Free License version 3.0 See the
 * file "LICENSE" for more information
 */
package sim.app.horde.agent;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.features.*;
import sim.app.horde.transitions.*;
import sim.engine.*;

import java.util.*;
import ec.util.*;
import java.io.*;

/**
 * AGENT
 */

public class Agent extends AgentPortrayal2D /* unavoidable */ implements Steppable, Cloneable
    {
    static final long serialVersionUID = 1L;
        
    /** The type for features and behaviors which exist in every single agent: such as 'done' or 'counter' */
    public static final String TYPE_GLOBAL = "global";
    /** The type for general-purpose features and behaviors which exist in the physical world. */
    public static final String TYPE_PHYSICAL = "physical";
    /** The directory which presently stores agent subdirectories. */
    public static final String AGENTS_DIRECTORY = "agents/";
    /** An agent subdirectory's database file name. */
    public static final String DATABASE_NAME = "db";
    /** Parameter in an Agent's parameter file for the number of types the agent has. */
    public static final Parameter P_NUM_TYPES = new ec.util.Parameter("num-types");
    /** Parameter in an Agent's parameter file for a given type, as in type.0 or type.1 */
    public static final Parameter P_TYPE = new ec.util.Parameter("type");
    /** Parameter in an Agent's parameter file for the Java class of the Agent. */
    public static final Parameter P_CLASS = new ec.util.Parameter("class");

    // The group containing the agent, if any (else null)
    AgentGroup group;
    
    // The name of the agent, that is, the "kind" of the agent.  Corresponds to a directory in agents/
    String name;
    
    // The feature and behavior types the agent is compatible with
    ArrayList<String> types = new ArrayList<String>();
    
    // The agent's behavior
    TrainableMacro behavior;

    // The horde, duh
    Horde horde;
    
    // Has the agent's start() method been called already?
    boolean started = false;
    
    // The Stoppable for the Agent's scheduled repeating: presently unused.
    public Stoppable stoppable;

    // Is the Agent permitted to become a training agent?
    boolean trainable = true;
        
    /** Sets whether this agent is permitted to become a training agent.  Perhaps it's
        just a foil in the simulation, for example.  */
    public void setTrainable(boolean val) { trainable = val; }
    /** Returns whether this agent is permitted to become a training agent.  Perhaps it's
        just a foil in the simulation, for example.  */
    public boolean isTrainable() { return trainable; }
    
    
    public Object clone()
        {
        try
            {
            Agent clone = (Agent)(super.clone());
            // don't clone the types, current or available features, horde, name, or group
            // Just clone the behavior
            // The group will get set by my AgentGroup when it clones.
            clone.behavior = (TrainableMacro)(behavior.clone());
            return clone;
            }
        catch (CloneNotSupportedException e) { return null; }  // never happens
        }
    
    /** Provides any joint behaviors for this Agent.  By default, returns an empty array.  ControllerAgent overrides this. */
    public Behavior[] provideJointBehaviors(ec.util.ParameterDatabase db) { return new Behavior[0]; }  // no joint behaviors by default
    
    /** Loads an Agent parameter database file as a database and returns it. */
    public static ec.util.ParameterDatabase getDatabase(String name)
        {
        try 
            {
            return new ec.util.ParameterDatabase(new File(Horde.getPathRelativeToClass(Horde.locationRelativeClass, Horde.AGENT_DIRECTORY) + AGENTS_DIRECTORY + name + "/" + DATABASE_NAME));
            }
        catch (IOException e) { throw new RuntimeException("Could not open file " + 
                Horde.getPathRelativeToClass(Horde.locationRelativeClass, Horde.AGENT_DIRECTORY) + AGENTS_DIRECTORY + name + "/" + DATABASE_NAME, e); }
        }
        
    /** This factory method returns a new agent from the class as specified in the database, with setup() called on it*/
    public static Agent provideAgent(ec.util.ParameterDatabase db, AgentGroup group, String name, Horde horde)
        {
        Agent proto = (Agent)(db.getInstanceForParameter(P_CLASS, null, Agent.class));
        proto.group = group;
        proto.name = name;
        proto.horde = horde;
        proto.setup(db);
        return proto;
        }
                    
    /** Reloads the entire Agent from its parameter database, resetting it. */
    public void reload()
        {
        setup(getDatabase(name));
        }
        
    /** Though this is public -- because interfaces (see Controller.java) must be public --
        do NOT call this method.  Instead, call setup(AgentGroup, String, Horde) */
    public void setup(ec.util.ParameterDatabase db)
        {
        // Load my types
        int numTypes = db.getInt(P_NUM_TYPES, null, 1);
        if (numTypes < 1) throw new RuntimeException("Invalid number of types in Agent " + this.name);
        for(int i = 0; i < numTypes ; i++)
            {
            String type = db.getString(P_TYPE.push("" + i), null);
            if (type == null) throw new RuntimeException("Missing type " + i + " in Agent " + this.name);
            types.add(type);
            }
                
        // Load my behaviors
        Behavior[] basic = Behavior.provideAllBasicBehaviors(this);
        Behavior[] joints = provideJointBehaviors(db);
        Behavior[] basicAndJoints = new Behavior[basic.length + joints.length];
        System.arraycopy(basic, 0, basicAndJoints, 0, basic.length);
        System.arraycopy(joints, 0, basicAndJoints, basic.length, joints.length);
        Behavior[] trainable = TrainableMacro.provideAllTrainableMacros(this, basicAndJoints);
                
        // create the TrainableMacro
        Behavior[] all = new Behavior[basicAndJoints.length + trainable.length];
        System.arraycopy(basicAndJoints,0,all,0,basicAndJoints.length);
        System.arraycopy(trainable,0,all,basicAndJoints.length, trainable.length);

        behavior = new TrainableMacro();
        behavior.reset(horde, all, new Feature[0]);
        
        started = false; // so start() is called again
        }

    /** Returns whether the given agent is compatible with the provided type. */
    public boolean hasType(String type)
        {
        return types.contains(type);
        }

    /** Returns the label to be drawn with a selected agent.  Override this
        method to return a different label. */
    public String getBehaviorBacktrace()
        {
        TrainableMacro top = getBehavior();
        if (top.currentBehavior < 0)
            return "[No Behavior]";
        else
            return top.getBehaviors()[top.currentBehavior].getBehaviorBacktrace();
        }

    /** Returns the agents's behavior (always a TrainableMacro) */
    public TrainableMacro getBehavior()
        {
        return behavior;
        }

    /** Sets the behavior to the provided behavior. */
    public void setBehavior(TrainableMacro behavior)
        {
        this.behavior = behavior;
        }

    /** Returns the agents's name (the "kind" of agent), which corresponds to its directory. */
    public String getName()
        {
        return name;
        }

    /** Returns the agent's group, if any. */
    public AgentGroup getGroup()
        {
        return group;
        }
        
    /** Sets the agent's group, if any. */
    public void setGroup(AgentGroup group)
        {
        this.group = group;
        }

    /**
     * Returns the index of the provided behavior in the current training macro.
     * Throws an exception if the provided behavior does not exist.
     */
    public int indexOfBehavior(String name)
        {
        Behavior[] behaviors = behavior.behaviors;
        for (int i = 0; i < behaviors.length; i++)
            if (behaviors[i].getName().equals(name))
                return i;
        throw new RuntimeException("Class not found among behaviors: " + name);
        }

    /**
     * Returns the index of the provided feature in the current training macro.
     * Throws an exception if the provided feature does not exist.
     */
    public int indexOfFeature(Class feature)
        {
        Feature[] features = behavior.features;
        for (int i = 0; i < features.length; i++)
            if (features[i].getClass() == feature)
                return i;
        throw new RuntimeException("Class not found among features: " + feature);
        }

    /** Returns true if I am the training agent.  */
    public boolean isTheTrainingAgent()
        {
        return horde.getTrainingAgent() == this;
        }

    /** Returns true if I am the training agent AND am presently in training mode.  */
    public boolean isTraining()
        {
        if (behavior instanceof TrainableMacro)
            return isTheTrainingAgent()
                && ((TrainableMacro) behavior).isTraining();
        return false;
        }

    /** Restarts the Agent such that next time step() is called, it'll have its behavior's start() method called again. */
    public void restart(Horde horde)
        {
        if (started)
            behavior.stop(this, null, horde);
        started = false;
        }

    /** Returns the Agent's Horde. */
    public Horde getHorde()
        {
        return horde;
        }

    public void step(SimState state)
        {
        int val = horde.getShouldGo();
        if (val == horde.GO_ALWAYS ||
        	(val == horde.GO_WHEN_TRAINING_AGENT
        		&& isTheTrainingAgent()))
        	{
	        if (!started)
	            {
	            behavior.start(this, null, horde);
	            started = true;
	            }
	        behavior.go(this, null, horde);
	        }
        }

    /** The distance from this agent to the nearest leaf node in the agent hierarchy under it. */
    public int maxLeafDistance()
        {
        return 0;
        }
        
    public void dispenseForRoots(ArrayList<Integer> rootArrayList, ArrayList<Agent> basicAgents)
        {
        }
    }
