package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.targets.*;

public class SetStatus extends Behavior
    {
    private static final long serialVersionUID = 1;

    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }

    int val;
        
    public boolean shouldAddDefaultExample() { return false; }

    public SetStatus()
        {
        this(1);
        } 

    public SetStatus(int val)
        {
        targets = new Target[1];
        targets[0] = new Me();
        targetNames = new String[1];
        targetNames[0] = "Of";
        this.val = val;
        name = "SetStatus[" + val + "]";
        setKeyStroke('1');
        }

    public void start(Agent agent, Macro parent, Horde horde)
        {
        Targetable targetable = targets[0].getTargetable(agent, parent, horde);
        if (targetable == null)  // uh oh
            System.err.println("Cannot set the status of target " + targets[0]);
        else targetable.setTargetStatus(agent, horde, val);
        }
    }
