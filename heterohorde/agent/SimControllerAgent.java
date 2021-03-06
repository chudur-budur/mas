package sim.app.horde.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ec.util.MersenneTwisterFast;
import sim.app.horde.*;
import sim.app.horde.behaviors.Behavior;
import sim.app.horde.behaviors.JointBehavior;
import sim.app.horde.behaviors.TrainableMacro;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;
import sim.util.MutableDouble2D;

public class SimControllerAgent extends SimAgent implements Controller
    {
    /** Drift speed. */
    static final double DRIFT_JUMP = 0.95;
    /** Drifts the agent a bit towards a given location.  Used by step(). */
    void driftToLocation(MutableDouble2D location)  
        {
        Double2D loc = getLocation();
        if (loc == null)
            {
            loc = new Double2D(location);
            }
        else 
            {
            loc = new Double2D(
                loc.x * DRIFT_JUMP + location.x * (1-DRIFT_JUMP),
                loc.y * DRIFT_JUMP + location.y * (1-DRIFT_JUMP));
            }
        setLocation(loc);
        }
    
    /** Drifts the agent a bit towards the mean of its subsidiaries. */
    public void step(SimState state)
        {
        super.step(state);
        
        // Compute the mean location of the subsidiary agents to the SimControllerAgent       
        int count = 0;
        Continuous2D agents = ((SimHorde) horde).agents;
        MutableDouble2D mean = new MutableDouble2D();
        for (AgentGroup group : subsidiaryAgents)
            for (Agent agent : group.getAgents())
                {
                Double2D loc = agents.getObjectLocation(agent);
                if (loc == null)
                    System.err.println("WARNING: controller agent with missing subsidiary agent in Horde.agents");
                else
                    { mean.addIn(loc); count++; }
                }

        // Drift to the mean of the subsidiary agents
        if (count > 0)
            {
            mean.multiplyIn(1.0 / count);
            driftToLocation(mean);
            }
        }
        
        
        
        
    /// DIRECT COPY FROM CONTROLLER AGENT
    /// DO NOT MODIFY UNLESS ALSO MOFIED IN CONTROLLERAGENT.JAVA
        
    static final long serialVersionUID = 1L;
        
    /** All subsidiary agent groups of the ControllerAgent. */
    ArrayList<AgentGroup> subsidiaryAgents = new ArrayList<AgentGroup>();
    
    /** Returns the subsidiary agents to the controller agent. */
    public ArrayList<AgentGroup> getSubsidiaryAgents() { return subsidiaryAgents; }
        
    public Object clone()
        {
        SimControllerAgent clone = (SimControllerAgent)(super.clone());
        clone.subsidiaryAgents = new ArrayList(subsidiaryAgents);
        // deep clone subsidiary agents.  Not sure if we need to do this
        for(int i = 0; i < clone.subsidiaryAgents.size(); i++)
            {
            AgentGroup ag = (AgentGroup)(clone.subsidiaryAgents.get(i).clone());
            ag.controller = clone;
            clone.subsidiaryAgents.set(i, ag);
            }
        return clone;
        }
        
    /** Though this is public -- because interfaces (see Controller.java) must be public --
        do NOT call this method.  Instead, call setup(AgentGroup, String, Horde) */
    public void setup(ec.util.ParameterDatabase db)
        {
        // NOTE: we call super.setup() last, so we can get our subsidiary agents
        // set up first here
                
        // at this point horde has been set (in Agent.setup(...), so we can use it
                
        // Load groups with a single agent in them.  We can use that to prototype clone
        int numSubs = db.getInt(P_NUM_SUBS, null, 1);
        if (numSubs < 1) throw new RuntimeException("Invalid number of subsidiaries in Agent " + this.name);
        for(int i = 0; i < numSubs ; i++)
            subsidiaryAgents.add(new AgentGroup(this, db, P_SUB.push("" + i), horde));
        super.setup(db);
        }               
        
    /** Called by Agent.setup(ParameterDatabase, Horde).  Note that this is in turn
        called by ControllerAgent.setup(ParameterDatabase, Horde), so subsidiaryAgents has been
        already set up. */
    public Behavior[] provideJointBehaviors(ec.util.ParameterDatabase db)
        {
        if (db.exists(P_NUM_JOINTS))  // is it heterogeneous?
            {
            System.err.println("Heterogeneous Controller Agent.  Loading Joint Behaviors: " + this.name);
            if (subsidiaryAgents.size() == 1)  // hmmmm
                System.err.println("WARNING.  Joint behaviors provided for homogeneous agents.  Hope you know what you're doing: " + this.name);
                                
            int numJoints = db.getInt(P_NUM_JOINTS, null, 0);
            if (numJoints < 1) throw new RuntimeException("Invalid number of joint behaviors in Agent " + this.name);
                        
            // Create joints
            Behavior[] joints = new Behavior[numJoints];
            for(int i = 0; i < numJoints ; i++)
                joints[i] = new JointBehavior(this, db, P_JOINT.push("" + i));
            return joints;
            }
        else if (subsidiaryAgents.size() == 1)  // is it homogeneous?
            {
            System.err.println("Homogeneous Controller Agent.  Creating Joint Behaviors: " + this.name);
            boolean all = db.getBoolean(P_ALLOW_ALL_BEHAVIORS, null, false);  // all behaviors or just macros?
            if (all) System.err.println("Allowing *all* joint behaviors.");
            Agent sub = (Agent)(subsidiaryAgents.get(0).agents.get(0));
                        
            // count the joints to load
            int count = 0;
            for(int i = 0; i < sub.behavior.behaviors.length; i++)
                {
                Behavior behavior = sub.behavior.behaviors[i];
                if (behavior instanceof TrainableMacro || all)
                    count++;
                }
            Behavior[] joints = new Behavior[count];
                        
            // load the joints
            int count2 = 0;
            for(int i = 0; i < sub.behavior.behaviors.length; i++)
                {
                Behavior behavior = sub.behavior.behaviors[i];
                if (behavior instanceof TrainableMacro || all)
                    joints[count2++] = new JointBehavior(this, behavior);
                }
            return joints;
            }
        else
            {
            throw new RuntimeException("Heterogeneous Controller Agent with no joint behaviors defined: " + this.name);
            }
        }


    /** Cached max leaf distance of the agent. */
    int maxLeafDistance = -1;  // indicates that the distance has not yet been set
    
    /** Returns the max leaf distance of this ControllerAgent.  This
        value is cached internally once computed. */
    public int maxLeafDistance()
        {
        if (maxLeafDistance != -1)
            return maxLeafDistance;
                        
        int dist = 1;

        for (AgentGroup group : subsidiaryAgents)
            {
            Agent sub = group.getAgent(0);
            int subDist = sub.maxLeafDistance();

            if (dist < subDist + 1)
                dist = subDist + 1;
            }
                                
        maxLeafDistance = dist;
        return dist;
        }

    /** Computed requirements per nested basic agent type */
    int [] computedMinAgents;
    int [] computedMaxAgents;
    int [] computedPreferredAgents;

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
       
    public void computeRequirements(List<String> basicTypes) 
        { 
        // Do we cache these with lazy initializer or just recompute??  Should only get called once!
        computedMaxAgents = new int[basicTypes.size()];
        computedMinAgents = new int[basicTypes.size()];
        computedPreferredAgents = new int[basicTypes.size()];
        
        for (AgentGroup group : subsidiaryAgents)
            {
            group.computeRequirements(basicTypes);
                        
            for (int i = 0; i < basicTypes.size(); i++)
                {
                computedMaxAgents[i] = computedMaxAgents[i] + group.getComputedMaxAgents(i);
                computedMinAgents[i] = computedMinAgents[i] + group.getComputedMinAgents(i);
                computedPreferredAgents[i] = computedPreferredAgents[i] + group.getComputedPreferredAgents(i);
                }
            }
        }
    
    public void dispenseForController(List<String> basicTypes, 
        int [] basicAgentAlloc, HierarchyConstraints constraints) 
        { 
        
        int [] currentAlloc = new int[basicAgentAlloc.length];
        System.arraycopy(basicAgentAlloc, 0, currentAlloc, 0, basicAgentAlloc.length);
        
        for (AgentGroup group : subsidiaryAgents)
            {
            int [] alloc = new int[currentAlloc.length];
            for (int i = 0; i < alloc.length; i++) 
                {
                alloc[i] = constraints.getComputedReqts(group, i);
                                
                if (alloc[i] >= currentAlloc[i])
                    {
                    alloc[i] = currentAlloc[i];
                    currentAlloc[i] = 0;
                    }
                else
                    currentAlloc[i] = currentAlloc[i] - alloc[i];
                }
                        
            group.dispenseAgents(basicTypes, alloc, constraints);
            }
        }
    
    public int agentCount(String type)
        {
        int count = 0;
        
        for (AgentGroup group : subsidiaryAgents)
            {
            Agent proto = group.getAgent(0);
                        
            if (proto.getName().equals(type)) count += group.getAgents().size();
                        
            else if (proto instanceof Controller)
                {
                for (Agent agent : group.getAgents())
                    {
                    Controller ctl = (Controller)agent;
                    int c = ctl.agentCount(type);
                    count += c;
                                        
                    if (c == 0) break;
                    }
                }
            }
                
        return count;
        }
    
    /**
     * At least one (basic) agent of the specified type has grabbed something
     * 
     * @param type The agent type
     * @return True if a subsidiary agent has grabbed something
     */
    public boolean hasManipulatedAgent(String type)
        {
        boolean manipulated = false;
                
        for (AgentGroup group : subsidiaryAgents)
            {
            Agent proto = group.getAgent(0);
                        
            // Assuming this is a basic agent
            if (proto.getName().equals(type)) 
                for (Agent a : group.getAgents())
                    {
                    if (((SimAgent) a).manipulated != null) return true;
                    }
                        
            else if (proto instanceof Controller)
                {
                for (Agent a : group.getAgents())
                    if (((SimControllerAgent) a).hasManipulatedAgent(type)) return true;
                }
            }
                
        return manipulated;
        }
    public void scheduleAgents(Horde horde)
        {
        horde.addAgent(this);

        for (AgentGroup group : subsidiaryAgents) group.scheduleAgents(horde);
        }

    public void initPose(Map<String, BasicInfo> info)
        {
        MutableDouble2D mean = new MutableDouble2D();
        int count = 0;
        for (AgentGroup group : subsidiaryAgents)
            {

            for (Agent agent : group.agents)
                if (agent instanceof SimControllerAgent)
                    {
                    ((SimControllerAgent)agent).initPose(info);
                    }
                else
                    {
                    // If it's a SimAgent, and the location isn't already set set the location
                    if (agent instanceof SimAgent && ((SimAgent)agent).loc.x == 0 && ((SimAgent)agent).loc.y == 0)
                        {
                        BasicInfo b = info.get(agent.name);

                        if (b == null) throw new RuntimeException("HierarchyConstraints: No info available for basic type " + agent.name);

                        ((SimAgent)agent).setLocation(new Double2D(horde.random.nextDouble() * b.width + b.xPos, horde.random.nextDouble() * b.height + b.yPos));
                        ((SimAgent)agent).setOrientation(horde.random.nextDouble() * Math.PI * 2);
                        }
                    }

	        for (Agent agent : group.getAgents())
                {
                mean.addIn(((SimAgent)agent).getLocation());
                count++;
                }
            }
	        mean.multiplyIn(1.0/count);
	        setLocation(new Double2D(mean.x, mean.y));
        }
    }