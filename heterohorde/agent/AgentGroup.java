package sim.app.horde.agent;

import java.util.*;

import ec.util.*;
import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.targets.Target;
import sim.app.horde.targets.ControllerTarget;
import sim.app.horde.targets.Wrapper;
import sim.util.Double2D;

/** 
    An AgentGroup has a collection of Subsidiary agents.
*/

public class AgentGroup implements Cloneable
    {
    static final long serialVersionUID = 1L;

    /** Parameter for the maximum number of agents permitted in the agent group */
    public static final String P_MAX = "max";  // yes, I know this isn't a Parameter, it's a string
    /** Parameter for the minimum number of agents permitted in the agent group */
    public static final String P_MIN = "min";  // yes, I know this isn't a Parameter, it's a string
    /** Parameter for the preferred number of agents permitted in the agent group */
    public static final String P_PREFERRED = "pref";  // yes, I know this isn't a Parameter, it's a string

    /** The AgentGroup's subsidiary agents.  */
    ArrayList<Agent> agents = new ArrayList<Agent>();

    /** The AgentGroup's controller */
    Controller controller;

    /** The minimum number of agents permitted in the agent group */
    int minAgents;

    /** The maximum number of agents permitted in the agent group */
    int maxAgents;

    /** The preferred number of agents permitted in the agent group */
    int preferredAgents;
    
    /** Computed requirements per basic agent type */
    int [] computedMinAgents;
    int [] computedMaxAgents;
    int [] computedPreferredAgents;

    /** Builds a dummy AgentGroup with no controller and no parameters set.
        Used for top-level AgentGroups in Horde.java such as roots and coroots. */
    public AgentGroup()
        {
        }
                
    /** Builds an AgentGroup given a controller and a database.
        Initially the AgentGroup will contain a single agent, which will be used
        as a prototype to flesh out the AgentGroup with more agents. */
    public AgentGroup(Controller controller, ec.util.ParameterDatabase db, Parameter base, Horde horde)
        {
        this.controller = controller;
        
        // Initially an AgentGroup contains a single prototypical agent at position 0
        String name = db.getString(base, null);
        
        minAgents = db.getInt(base.push(P_MAX), null, 1);
        if (minAgents < 1) throw new RuntimeException("Min agents must be >= 1");
        
        maxAgents = db.getInt(base.push(P_MAX), null, minAgents);
        if (maxAgents < minAgents) throw new RuntimeException("Max agents must be >= min agents");

        preferredAgents = db.getInt(base.push(P_PREFERRED), null, 1);
        if (preferredAgents < minAgents || preferredAgents > maxAgents) 
            throw new RuntimeException("Preferred agents must be between min and max agents inclusive");
        
        Agent proto = Agent.provideAgent(Agent.getDatabase(name), this, name, horde);
        addAgent(proto);
        }
                
    public Object clone()
        {
        try
            {
            AgentGroup clone = (AgentGroup)(super.clone());
            // just clone the agents, not the controller.  It'll get set by my Controller when it clones
            clone.agents = new ArrayList(agents);
                
            // deep clone the agents
            for(int i = 0; i < clone.agents.size(); i++)
                {
                Agent a = (Agent)(clone.agents.get(i).clone());
                a.group = clone;
                clone.agents.set(i, a);
                }
            return clone;
            }
        catch (CloneNotSupportedException e) { return null; } // never happens
        }
        
    public Target[] wrapControllerTargets(Target[] targets, Macro parent)
        {
        if (targets == null || targets.length == 0) return targets;
        else 
            {
            Target[] newTargets = new Target[targets.length];
            for(int i = 0; i < targets.length; i++)
                {
                if (targets[i] != null)
                                
                    // Only wrap wrappers
                    if (targets[i] instanceof Wrapper)
                        newTargets[i] = new ControllerTarget(targets[i], parent);
                    else newTargets[i] = targets[i];
                }
            return newTargets;
            }
        }
        
    /** 
        Informs subsidiary agents to perform a certain behavior (the index) corresponding
        to the provided joint behavior.
        
        <p><b>
        NOTE THAT CONTROLLERS OUGHT TO BE SCHEDULED BEFORE THEIR SUBSIDIARIES
        IN ORDER TO MAXIMIZE SPEED, ELSE IT MAY TAKE SEVERAL TICKS TO DISTRIBUTE
        COMMANDS!  -- Sean
        
        <p>See Horde.createAgents() comments
        </b>
    */
    public void fireBehaviors(JointBehavior joint, int behaviorIndex, Macro parent)
        {
        for(int i = 0; i < agents.size(); i++)
            agents.get(i).behavior.setNewBehaviorRequestedByUser(behaviorIndex, wrapControllerTargets(joint.getTargets(), parent));
        }
    
    /** Returns the given agent. */
    public Agent getAgent(int index)
        {
        return agents.get(index);
        }

    /** Returns all agents. */
    public ArrayList<Agent> getAgents()
        {
        return agents;
        }

    /** Returns the group's name, which is defined as the name of its first agent. */
    public String getName()
        {
        return agents.get(0).getName();
        }

    /** Returns the group's controller. */
    public Controller getController()
        {
        return controller;
        }

    /** Adds an agent to the group. */
    public void addAgent(Agent agent)
        {
        agents.add(agent);
        agent.setGroup(this);
        }

    public int getComputedMaxAgents(int basicTypeIndex)
        {
        return computedMaxAgents[basicTypeIndex];
        }

    public int getComputedMinAgents(int basicTypeIndex)
        {
        return computedMinAgents[basicTypeIndex];
        }

    public int getComputedPreferredAgents(int basicTypeIndex)
        {
        return computedPreferredAgents[basicTypeIndex];
        }
    
    public void computeRequirements (List<String> basicTypes)
        {
        // Do we cache these with lazy initializer or just recompute??  Should only get called once!
        computedMaxAgents = new int[basicTypes.size()];
        computedMinAgents = new int[basicTypes.size()];
        computedPreferredAgents = new int[basicTypes.size()];
        
        Agent proto = agents.get(0);
        
        if (proto instanceof Controller)
            {
            Controller ctl = (Controller)proto;
            ctl.computeRequirements(basicTypes);
                
            for (int i = 0; i < basicTypes.size(); i++)
                {
                computedMaxAgents[i] = maxAgents * ctl.getComputedMaxAgents(i);
                computedMinAgents[i] = minAgents * ctl.getComputedMinAgents(i);
                computedPreferredAgents[i] = preferredAgents * ctl.getComputedPreferredAgents(i);
                }
            }
        else
            {
            for (int i = 0; i < basicTypes.size(); i++)
                if (basicTypes.get(i).equals(proto.name))
                    {
                    computedMaxAgents[i] = maxAgents;
                    computedMinAgents[i] = minAgents;
                    computedPreferredAgents[i] = preferredAgents;
                    }
            }
        }
        
    public void dispenseAgents(List<String>basicTypes, int[] alloc, HierarchyConstraints constraints)
        {
        Agent proto = agents.get(0);
        
        if (proto instanceof Controller)
            {
            Controller ctl = (Controller)proto;
                
            // How many controllers if we use the preferred size?  Smallest of
            // the basic agents
            int numControllers = preferredAgents;
            for (int i = 0; i < alloc.length; i++)
                {
                int prefB = constraints.getComputedReqts(ctl, i);
                int n = (alloc[i] / prefB) + (alloc[i] % prefB > 0 ? 1 : 0);
                if (n < numControllers) numControllers = n;
                }

            ceateAndDispenseToControllers(basicTypes, alloc, numControllers, constraints);
            }
        else
            allocBasicAgents(basicTypes, alloc, constraints);
        }

    public void scheduleAgents(Horde horde)
        {
        Agent proto = agents.get(0);

        if (proto instanceof Controller)
        	for (Agent agent: agents) ((Controller)agent).scheduleAgents(horde);
        else
        	for (Agent agent: agents) horde.addAgent(agent);
        }

    private void allocBasicAgents(List<String> basicTypes, int[] alloc, HierarchyConstraints constraints)
        {
        System.out.println("Allocate basic agents to the group " + constraints);
        Agent proto = agents.get(0);

        for (int i = 0; i < basicTypes.size(); i++)
        	if (basicTypes.get(i).equals(proto.name))
                {
                while (agents.size() < alloc[i])
                	{
                	Agent clone = (Agent)proto.clone();
                	addAgent(clone);
                	}
                }
        }

    private void ceateAndDispenseToControllers(List<String> basicTypes, 
        int[] alloc, int numControllers, HierarchyConstraints constraints)
        {
        Controller ctl;
        Agent proto = agents.get(0);

        while (agents.size() < numControllers)
	        {
	        Agent clone = (Agent)proto.clone();
	        addAgent(clone);
	        }

        for (Agent agent : agents)
            {
            int [] ctlAlloc = new int[alloc.length];

            for (int i = 0; i < alloc.length; i++)
                {
                ctlAlloc[i] = (int) Math.ceil(alloc[i] / numControllers);
                alloc[i] = alloc[i] - ctlAlloc[i];
                }

            numControllers--;
            ctl = (Controller)agent;
            ctl.dispenseForController(basicTypes, ctlAlloc, constraints);
            }
        } 

    }
