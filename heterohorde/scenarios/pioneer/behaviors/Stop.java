package sim.app.horde.scenarios.pioneer.behaviors;

import sim.app.horde.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.*;
import sim.app.horde.scenarios.pioneer.PioneerAgent;

public class Stop extends Behavior
    {
    private static final long serialVersionUID = 1L;

    public Stop() { name = "Stop"; } 
                
    public void go(Agent agent, Macro parent, Horde horde)
        { 
        super.go(agent, parent, horde);         
        ((PioneerAgent)agent).stopRobot(); 
        }       
    }
