/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.features;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.CategoricalFeature;
import sim.app.horde.scenarios.robot.darwin.agent.DarwinAgent;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;

/**
 *
 * @author drew
 */
public class InPlay extends CategoricalFeature{
    
    private static final long serialVersionUID = 1;

    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }    
    
    
    public InPlay() {
        super("InPlay",  new String[] {"NotInPlay", "InPlay"});
        }
    
    @Override
    public double getValue(Agent agent, Macro parent, Horde horde) {
        
        DarwinParser dp = ((DarwinAgent) agent).getCurrentData();
        return dp.getInPlay();
        }
    
    }