package sim.app.horde.scenarios.pioneer.features;

import sim.app.horde.agent.Agent;
import sim.app.horde.Horde;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.Feature;
import sim.app.horde.scenarios.pioneer.PioneerAgent;

public class ClosestObstacleDistance extends Feature {
    private static final long serialVersionUID = 1L;

    public ClosestObstacleDistance() { super("Closest Obstacle Distance"); } 
        
    public double getValue(Agent agent, Macro parent, Horde horde) { 
        return ((PioneerAgent)agent).closestObstacleDistance; 
        }
    }
