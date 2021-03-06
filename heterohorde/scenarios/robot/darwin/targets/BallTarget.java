/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.targets;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.robot.darwin.DarwinHorde;
import sim.app.horde.scenarios.robot.darwin.agent.DarwinAgent;
import sim.app.horde.scenarios.robot.darwin.agent.Real;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;
import sim.app.horde.targets.Target;
import sim.util.Double2D;

/**
 * This is a dynamic target...
 * @author drew
 */
public class BallTarget extends Target implements Real {

    
    private static final long serialVersionUID = 1L;
    
    
    @Override
    public String toString() {
        return "Ball Target"; 
        }

    @Override
    public Double2D getLocation(Agent agent, Macro parent, Horde horde) {
        
        
        DarwinParser dp = ((DarwinAgent) agent).getCurrentData();
        return new Double2D(dp.getClosestToBallLocX() * 10, dp.getClosestToBallLocY() * 10);
        }
    @Override
    public Double2D getRealTargetLocation(Agent agent, Macro parent, Horde horde) {
        
        DarwinParser dp = ((DarwinAgent) agent).getCurrentData();
        return new Double2D(dp.getClosestToBallLocX(), dp.getClosestToBallLocY());
        }
    }