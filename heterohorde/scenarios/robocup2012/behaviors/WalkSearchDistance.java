package sim.app.horde.scenarios.robocup2012.behaviors;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.scenarios.robocup2012.HumanoidAgent;

public class WalkSearchDistance extends Behavior
    {
    private static final long serialVersionUID = 1L;

    public WalkSearchDistance() 
        {
        name = "WalkSearchDistance";
        }

    public boolean getShouldAddDefaultExample() { return false; }    

    public void start(Agent agent, Macro parent, Horde horde)
        {
        super.start(agent, parent, horde);

        HumanoidAgent hAgent = (HumanoidAgent) agent;
        for (int i = 0; i < 10; i++)
            hAgent.humanoid.doMotion(HumanoidAgent.RCB4_MOT_WALK_ONE_STEP);

        hAgent.humanoid.doMotion(HumanoidAgent.RCB4_MOT_CALIBRATE_HOME);
        }
    }