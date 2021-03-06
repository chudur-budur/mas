package sim.app.horde.agent;

import java.util.*;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.transitions.*;

public class ControllerAgent extends Agent implements Controller
    {
    /// IMPORTANT NOTE: IF YOU MODIFY ANY OF THE CODE BELOW,
    /// BE SURE TO DO THE SAME IN SIMCONTROLLERAGENT.JAVA, WHICH HAS
    /// AN EXACT COPY OF THIS CODE.  (JAVA DOESN'T HAVE MULTIPLE
    /// INHERITANCE).
    ///
    /// ALSO, IF YOU ADD A METHOD OR DELETE A METHOD, BE SURE
    /// TO CHECK IN CONTROLLER.JAVA TO SEE IF IT SHOULD BE THERE
    /// AS WELL.

    static final long serialVersionUID = 1L;
   
    /** All subsidiary agent groups of the ControllerAgent. */
    ArrayList<AgentGroup> subsidiaryAgents;
    
    /** Returns the subsidiary agents to the controller agent. */
    public ArrayList<AgentGroup> getSubsidiaryAgents() { return subsidiaryAgents; }
        
    public Object clone()
        {
        ControllerAgent clone = (ControllerAgent)(super.clone());
        clone.subsidiaryAgents = new ArrayList(subsidiaryAgents);
                
        // deep clone subsidiary agents.  Not sure if we need to do this
        for(int i = 0; i < clone.subsidiaryAgents.size(); i++)
            {
            AgentGroup ag = (AgentGroup)(clone.subsidiaryAgents.get(i).clone());
            ag.controller = this;
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
    
    public void dispenseForController(List<String> basicTypes, int [] basicAgentAlloc, HierarchyConstraints constraints)
        {
        // How do we handle real agents?
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
    
     	public void scheduleAgents(Horde horde)
     		{
     		horde.addAgent(this);
     		
       		for (AgentGroup group : subsidiaryAgents) group.scheduleAgents(horde);  		
     		}
    }
