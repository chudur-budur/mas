/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.behaviors;

import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Behavior;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.robot.darwin.agent.*;
import sim.app.horde.targets.*;
import sim.app.horde.*;
import sim.app.horde.scenarios.robot.darwin.targets.GoalTarget;
import sim.app.horde.scenarios.robot.darwin.targets.PositionOne;
import sim.util.*;

/**
 *
 * @author drew
 */
public class GotoPose extends Behavior {
    private static final long serialVersionUID = 1L;

    public GotoPose() 
        {
        targets = new Target[2];
        targets[0] = new Me();
        targets[1] = new Me();
        targetNames = new String[] {"Where", "Facing"};
        name = "gotoPose";
        }
    
    public GotoPose(Target where, Target facing) 
        {
        this();
        targets[0] = where;
        targets[1] = facing;
        }
    
    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }
    /*
      +-------------+
      |+-----+-----+|
      ||     |     ||
      |+-----C-----+|
      ||     |     ||
      |B-----+-----+|
      A-------------+

      In Horde World, units of measure are in cm
      In Horde World, A is (0,0), B is (2.6, 2.0), and C is (30.0, 20.0)
      In Real World, A is (-30.0, -20.0), B is (-27.4, -18.0), and C is (0.0, 0.0)
    */
    @Override
    public void start(Agent agent, Macro parent, Horde horde) {
        super.start(agent, parent, horde);
        
        
        
        
        Targetable targetable = targets[0].getTargetable(agent, parent, horde);
        Targetable targetable2 = targets[1].getTargetable(agent, parent, horde);// good it is a attackerTwoAgent
        /*
          System.err.println("Targetable 2: " + targetable2.getClass() + " Target: " + targets[1]);
        
          System.err.println("Is attacker2 a Real? " + (targetable2 instanceof Real));
          System.err.println("Is targets[0] a real " + (targets[0] instanceof Real) + " also targets[0] = " + targets[0]);
        */
        Target target = targets[0];
        if (target instanceof Wrapper)
            target = ((Wrapper)target).getTopParameter(parent);
        
        Target target2 = targets[1];
        if (target2 instanceof Wrapper)
            target2 = ((Wrapper)target2).getTopParameter(parent);
        
        
        //System.err.println("Is targets[0] a real " + (target instanceof Real) + " also targets[0] = " + target.getClass());
        
        Real rm = null, rm2 = null;
        
        if (target instanceof Real && targetable2 instanceof Real) {
            //System.err.println("First was a real Target and the second was a targetable real");
            rm = (Real)target;
            rm2 = (Real)targetable2;
            }
        
        if (target2 instanceof Real && targetable instanceof Real) {
            //System.err.println("First was a real Target and the second was a targetable real");
            rm = (Real)targetable;
            rm2 = (Real)target2;
            }
        
        if (target instanceof Real && target2 instanceof Real) {
            // System.err.println("Both targets not real markers");
            rm = (Real)target;
            rm2 = (Real)target2;
            }
        
        if (targetable instanceof Real && targetable2 instanceof Real)
            {
            rm = (Real)targetable;
            rm2 = (Real)targetable2;
            }
        
        if (rm != null && rm2 != null) {
            Double2D loc = rm.getRealTargetLocation(agent, parent, horde);
            // position: loc.x, loc.y
            Double2D loc2 = rm2.getRealTargetLocation(agent, parent, horde);

            //System.err.println("loc = " + loc +  " loc2 = " + loc2);// loc is null!!

            
            double orientation = Math.atan2(loc2.y - loc.y, loc2.x - loc.x);
            // System.err.println("Oriented to " + orientation + " rad");

            //System.err.println("The Horde coords " + loc.toCoordinates() + "  converted: " + x + " " + y + " orientation: " + orientation);
            ((DarwinAgent) agent).incrementAck(); // only increment if got here otherwise might increment when don't want to
            ((DarwinAgent) agent).sendMotion(Motions.getGotoPose(loc.x, loc.y, orientation));
            
            }
        }
    
    @Override
    public void go(Agent agent, Macro parent, Horde horde) {
        super.start(agent, parent, horde);
        
        
        
        
        Targetable targetable = targets[0].getTargetable(agent, parent, horde);
        Targetable targetable2 = targets[1].getTargetable(agent, parent, horde);// good it is a attackerTwoAgent
        /*
          System.err.println("Targetable 2: " + targetable2.getClass() + " Target: " + targets[1]);
        
          System.err.println("Is attacker2 a Real? " + (targetable2 instanceof Real));
          System.err.println("Is targets[0] a real " + (targets[0] instanceof Real) + " also targets[0] = " + targets[0]);
        */
        Target target = targets[0];
        if (target instanceof Wrapper)
            target = ((Wrapper)target).getTopParameter(parent);
        
        Target target2 = targets[1];
        if (target2 instanceof Wrapper)
            target2 = ((Wrapper)target2).getTopParameter(parent);
        
        
        //System.err.println("Is targets[0] a real " + (target instanceof Real) + " also targets[0] = " + target.getClass());
        
        Real rm = null, rm2 = null;
        
        if (target instanceof Real && targetable2 instanceof Real) {
            //System.err.println("First was a real Target and the second was a targetable real");
            rm = (Real)target;
            rm2 = (Real)targetable2;
            }
        
        if (target2 instanceof Real && targetable instanceof Real) {
            //System.err.println("First was a real Target and the second was a targetable real");
            rm = (Real)targetable;
            rm2 = (Real)target2;
            }
        
        if (target instanceof Real && target2 instanceof Real) {
            // System.err.println("Both targets not real markers");
            rm = (Real)target;
            rm2 = (Real)target2;
            }
        
        if (targetable instanceof Real && targetable2 instanceof Real)
            {
            rm = (Real)targetable;
            rm2 = (Real)targetable2;
            }
        
        if (rm != null && rm2 != null) {
            Double2D loc = rm.getRealTargetLocation(agent, parent, horde);
            // position: loc.x, loc.y
            Double2D loc2 = rm2.getRealTargetLocation(agent, parent, horde);

            //System.err.println("loc = " + loc +  " loc2 = " + loc2);// loc is null!!

            
            double orientation = Math.atan2(loc2.y - loc.y, loc2.x - loc.x);
            System.err.println("Oriented to " + orientation + " rad");

            //System.err.println("The Horde coords " + loc.toCoordinates() + "  converted: " + x + " " + y + " orientation: " + orientation);
            ((DarwinAgent) agent).incrementAck(); // only increment if got here otherwise might increment when don't want to
            ((DarwinAgent) agent).sendMotion(Motions.getGotoPoseUpdate(loc.x, loc.y, orientation));
            
            }
        }
    }
