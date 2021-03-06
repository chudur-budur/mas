/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.features;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.Feature;
import sim.app.horde.scenarios.robot.darwin.agent.DarwinAgent;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;
import sim.app.horde.scenarios.robot.darwin.targets.RobotOne;
import sim.app.horde.targets.Me;
import sim.app.horde.targets.Target;

/**
 *
 * @author drew
 */
public class PoseXOf extends Feature{

    private static final long serialVersionUID = 1;

    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }    
    
    
    public PoseXOf() {
        super("PoseXOf");
        targets = new Target[1];
        targets[0] = new Me();  //  default
        targetNames = new String[]{ "Of" };// the other guy who I want to know about
        }
    
    @Override
    public double getValue(Agent agent, Macro parent, Horde horde) {
        
        if (targets[0] instanceof RobotOne)
            {
            //System.err.println(((RobotOne)(targets[0])).getLocation(agent, parent, horde));
            return ((RobotOne)(targets[0])).getLocation(agent, parent, horde).x;
            }
        else if (targets[0] instanceof Me)
            {
            DarwinParser dp = ((DarwinAgent) agent).getCurrentData();
            return dp.getPoseX();
            }
        else return 0;
        }
    
    }
