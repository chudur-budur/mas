/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.targets;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.robot.darwin.agent.Real;
import sim.app.horde.targets.Target;
import sim.util.Double2D;

/**
 *
 * @author drew
 */
public class PositionTwo extends Target implements Real{

    private static final long serialVersionUID = -5424100016975541678L;
    Double2D posTwo = new Double2D(5.0, -7.5);
    
    @Override
    public String toString() {
        return "Position Two: (0, -.75) in meters"; 
        }

    @Override
    public Double2D getLocation(Agent agent, Macro parent, Horde horde) {
        return posTwo;
        }
    @Override
    public Double2D getRealTargetLocation(Agent agent, Macro parent, Horde horde) {
        //return realPosTwo;
        return new Double2D(0.5, -0.75);
        }
    }
