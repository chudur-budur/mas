/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.features;

import sim.app.horde.Horde;
import sim.app.horde.Targetable;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.CategoricalFeature;
import sim.app.horde.scenarios.robot.darwin.agent.DarwinAgent;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;
import sim.app.horde.targets.Me;
import sim.app.horde.targets.Target;

/**
 * has the robot received a signal indicating a failure
 * @author drew
 */
public class YelledFail extends CategoricalFeature{

    private static final long serialVersionUID = 1;

    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }
    
    public YelledFail() {
        super("YelledFail", new String[] {"NotYelledFail", "YelledFail"});
        targets = new Target[1];
        targets[0] = new Me();  //  default
        targetNames = new String[]{ "X" };// the other guy who I want to know about
        }

    
    
    @Override
    public double getValue(Agent agent, Macro parent, Horde horde) {
        Targetable targetable = targets[0].getTargetable(agent, parent, horde);
        
        if (targetable instanceof DarwinAgent) 
            {
            
            DarwinParser dp = ((DarwinAgent) targetable).getCurrentData();
            // System.err.println("Agent " + dp.getPlayerID() + "  which I am targeting has Yelled equal to: " + dp.getYelledFail());
            return dp.getYelledFail();
            
            }
        
        return 0.0;
        }
    
    }