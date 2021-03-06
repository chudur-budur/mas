/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.targets;

import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.robot.darwin.agent.DarwinAgent;
import sim.app.horde.scenarios.robot.darwin.agent.Real;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;
import sim.app.horde.targets.Target;
import sim.util.Double2D;

/**
 *
 * @author drew
 */
public class AttackingGoalTarget extends Target implements Real{

    
    private static final long serialVersionUID = 1L;
    
    // make it 30cm beyond the goal so that when the robot is close to the goal he
    // doesn't kick the other way to try and kick at 2.76m.
    Double2D positiveGoalTarget = new Double2D(30.6, 0);
    Double2D negativeGoalTarget = new Double2D(-30.6, 0);
    
    Double2D realPositiveGoalTarget = new Double2D(3.06, 0);
    Double2D realNegativeGoalTarget = new Double2D(-3.06, 0);
    
    boolean isPositive = false;
    
    @Override
    public String toString() {
        
        return "Attacking Goal Target: ( +/- 3.06, 0) in meters"; 
        }

    @Override
    public Double2D getLocation(Agent agent, Macro parent, Horde horde) {
        DarwinParser dp = ((DarwinAgent) agent).getCurrentData();
        if(dp.getAttackingGoalSign() > 0) {
            isPositive = true;
            return positiveGoalTarget;
            }
        else {
            isPositive = false;
            return negativeGoalTarget;
            }
        }
    @Override
    public Double2D getRealTargetLocation(Agent agent, Macro parent, Horde horde) {
        DarwinParser dp = ((DarwinAgent) agent).getCurrentData();
        if(dp.getAttackingGoalSign() > 0) {
            isPositive = true;
            return realPositiveGoalTarget;
            }
        else {
            isPositive = false;
            return realNegativeGoalTarget;
            }
        
        
        }
    }
