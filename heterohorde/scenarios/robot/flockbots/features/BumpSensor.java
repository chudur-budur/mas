/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.flockbots.features;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.Feature;
import sim.app.horde.scenarios.robot.flockbots.FlockbotAgent;
import sim.app.horde.scenarios.robot.flockbots.comm.FlockbotParser;

/**
 *
 * @author drew
 */
public class BumpSensor extends Feature{

    public BumpSensor() {
        super("BumpSensor");
        }
    
    public double getValue(Agent agent, Macro parent, Horde horde)
        {
        FlockbotParser hd = ((FlockbotAgent) agent).getCurrentData();
        return hd.getHighBump();// for now that will do...
        }
    
    
    }
