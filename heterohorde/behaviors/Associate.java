package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.*;
import sim.app.horde.targets.*;

public class Associate extends Behavior 
    {
    private static final long serialVersionUID = 1L;

    public static String getType() { return sim.app.horde.agent.Agent.TYPE_GLOBAL; }

    public boolean shouldAddDefaultExample() { return false; }

    public Associate()
        {
        name = "Associate";
        targets = new Target[1];
        targets[0] = new Me();
        targetNames = new String[1];
        targetNames[0] = "With";
        }
    
    public void start(Agent agent, Macro parent, Horde horde) 
        {            
        parent.setAssociatedObject(Macro.ASSOCIATED_OBJECT_BASIC, targets[0].getTargetable(agent, parent, horde)); 
        }
    }
