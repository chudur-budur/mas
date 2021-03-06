/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin;

import sim.app.horde.Horde;
import sim.app.horde.SimHorde;
import sim.app.horde.scenarios.robot.darwin.agent.*;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;
import sim.display.RateAdjuster;
import sim.engine.SimState;
import static sim.engine.SimState.doLoop;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.Double2D;

/**
 *
 * @author drew
 */
public class DarwinHorde extends SimHorde{
    private static final long serialVersionUID = 1;

    static {
        locationRelativeClass = DarwinHorde.class;
        }

    public DarwinHorde(long seed) {
        super(seed);
        }
    
    Object ball = null;
    
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

    public void start()
        {
        super.start();
        
        // at this point, the object representing the ball is in the objects
        // field and is located at [29.5, 19.5]
        
        Bag bag = regions.getObjectsAtLocation(new Double2D(29.5, 19.5));
        ball = bag.get(bag.size()-1);
        schedule.scheduleRepeating(new RateAdjuster(100)); // this will make it 60 FPS and slow down moving things
        
        schedule.scheduleRepeating(new Steppable()
            {
            public void step(SimState state)
                {
                if (getTrainingAgent() != null)
                    {
                    DarwinParser dp = ((DarwinAgent) getTrainingAgent()).getCurrentData();
                    if (dp != null)
                        {
                        Double2D loc = new Double2D(dp.getClosestToBallLocX()*10 - 0.5,dp.getClosestToBallLocY()*10 - 0.5);
                        regions.setObjectLocation(ball, loc);
                            
                        /* // this converts the ball relative to the robot
                        // to the ball global
                        // get current ball location
                        double x = dp.getBallX() * 10;
                        double y = dp.getBallY() * 10;
            
                        // Get theta and negate
                        double theta = dp.getPoseAngle();
            
                        // Rotate 
                        x = x * Math.cos(theta) + y * (0 - Math.sin(theta));
                        y = x * Math.sin(theta) + y * Math.cos(theta);          
            
                        // Translate to inertial frame of real world
                        x = x + dp.getPoseX() * 10;
                        y = y + dp.getPoseY() * 10;
            
                        //System.err.println("Ball location in real world: " + x + " " + y);

                        // Translate to inertial frame of horde world
                        x += 30;
                        y += 20;
            
                        // In Horde, the ball is drawn with the lower left
                        // corner on its location.  We need to draw it so that
                        // the center of the ball is on the location.
                        x -= 0.5;
                        y -= 0.5;
                        Double2D loc = new Double2D(x,y);
                        //System.err.println("Translated Ball location: " + loc);
                        regions.setObjectLocation(ball, loc);
                        */
                        }
                    }
                }
            }, 2);
        
        bag = markers.getObjectsAtLocation(new Double2D(0,0));
        for(int i = 0; i < bag.size(); i++)
            {
            final int index = i;
            final Particle particle = (Particle)(bag.get(i));
        
            schedule.scheduleRepeating(new Steppable()
                {
                public void step(SimState state)
                    {
                    if (getTrainingAgent() != null) {
                        DarwinParser dp = ((DarwinAgent) getTrainingAgent()).getCurrentData();
                        if (dp != null && dp.getParticleX() != null && dp.getParticleX().length > 0)
                            {
                            // get current agent location                     
                            double x = dp.getParticleX()[index] * 10;
                            double y = dp.getParticleY()[index] * -10;
                            double theta = dp.getParticleA()[index];

                            // translate from C to A
                            x += 30;
                            y += 20;
                            Double2D loc = new Double2D(x,y);
                            markers.setObjectLocation(particle, loc);
                            particle.setOrientation(theta);
                            }
                        }
                    }
                });
            }

        bag = markers.getObjectsAtLocation(new Double2D(60, 40));
        for(int i = 0; i < bag.size(); i++)
            {
            final VirtualAgent particle = (VirtualAgent)(bag.get(i));
        
            schedule.scheduleRepeating(new Steppable()
                {
                public void step(SimState state)
                    {
                    if (getTrainingAgent() != null) {
                        DarwinParser dp = ((DarwinAgent) getTrainingAgent()).getCurrentData();
                        if (dp != null)
                            {
                            // get current agent location                     
                            double x = dp.getOtherRobotX(particle.robotNumber) * 10;
                            double y = dp.getOtherRobotY(particle.robotNumber) * 10;
                            double theta = dp.getOtherRobotA(particle.robotNumber);

                            // translate from C to A
                            x += 30;
                            y += 20;
                            Double2D loc = new Double2D(x,y);
                            markers.setObjectLocation(particle, loc);
                            particle.orientation = theta;
                            }
                        }
                    }
                });
            }


        
        }

    
    public static void main(String[] args)
        {
        if (args.length > 0)
            Horde.defaultArena = args[0];
        doLoop(DarwinHorde.class, args);
        System.exit(0);
        }
    
    }
