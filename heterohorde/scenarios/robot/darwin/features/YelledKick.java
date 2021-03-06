/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.features;

import java.util.logging.Level;
import java.util.logging.Logger;
import sim.app.horde.Horde;
import sim.app.horde.Targetable;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.CategoricalFeature;
import sim.app.horde.scenarios.robot.darwin.agent.DarwinAgent;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;
import sim.app.horde.scenarios.robot.darwin.targets.GoalTarget;
import sim.app.horde.targets.Me;
import sim.app.horde.targets.Target;

/**
 * This feature is used by the receiver to know when the passer has kicked the ball
 * and to transition to a new state.
 * @author drew
 */
public class YelledKick extends CategoricalFeature {

    private static final long serialVersionUID = 1;

    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }
    long startTime;
    long timeoutLength = 2000;
    public YelledKick() {
        super("YelledKick", new String[] {"NotKicked", "Kicked"});
        
        targets = new Target[1];
        targets[0] = new Me();  //  default
        targetNames = new String[]{ "X" };// the other guy who I want to know about
        }

    @Override
    public void start(Agent agent, Macro parent, Horde horde) {
        super.start(agent, parent, horde); //To change body of generated methods, choose Tools | Templates.
        startTime = System.currentTimeMillis();
        }

    
    
    @Override
    public double getValue(Agent agent, Macro parent, Horde horde) {
        // get whether the targeted player has kicked
        
        // otherwise keep checking if the passer has kicked the ball.
        Targetable targetable = targets[0].getTargetable(agent, parent, horde);
        
        if (targetable instanceof DarwinAgent) 
            {
            
            DarwinParser dp = ((DarwinAgent) targetable).getCurrentData();
            return dp.getKicked();
            
            
            }
        
        return 0.0;
        
        }
    
    }