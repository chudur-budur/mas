/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.behaviors;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Behavior;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.robot.darwin.agent.DarwinAgent;

/**
 * Pivots in place looking for ball.
 * @author drew
 */
public class LocateBall extends Behavior{
    
    
    private static final long serialVersionUID = 1L;

    
    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }
    
    public LocateBall() {
        name = "LocateBall";
        }
    
    long lastSent = 0;
    @Override
    public void start(Agent agent, Macro parent, Horde horde) {
        super.start(agent, parent, horde);
        
           
        ((DarwinAgent) agent).incrementAck();
        DarwinAgent da = (DarwinAgent) agent;
        
        da.prevBehavior = this;
        ((DarwinAgent) agent).sendMotion(Motions.MOVE_THETA);
        lastSent = System.currentTimeMillis();
       
        
        }
    
    
    }
