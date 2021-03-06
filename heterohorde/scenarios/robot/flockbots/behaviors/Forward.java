/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.flockbots.behaviors;

import sim.app.horde.agent.Agent;
import sim.app.horde.Horde;
import sim.app.horde.behaviors.Behavior;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.robot.flockbots.FlockbotAgent;

/**
 *
 * @author drew
 */
public class Forward extends Behavior {

    private static final long serialVersionUID = 1L;

    public Forward() {
        name = "forward";
        }
    
    
    
    @Override
    public void start(Agent agent, Macro parent, Horde horde) {
        super.start(agent, parent, horde);
        ((FlockbotAgent) agent).sendMotion(Motions.FORWARD);
        }
    
    
    
    }
