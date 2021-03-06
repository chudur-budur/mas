/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.agent;


import sim.app.horde.agent.SimAgent;
import sim.app.horde.scenarios.robot.behaviors.CommandMotions;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;
import sim.engine.SimState;
import sim.util.Double2D;
import sim.portrayal.*;
import sim.app.horde.*;
import sim.portrayal.simple.*;
import java.awt.*;

public class Particle extends OrientedPortrayal2D
    {
    private static final long serialVersionUID = 1;

    double orientation;
    public void setOrientation(double o) { orientation = o; }
    public double getOrientation(Object object, DrawInfo2D info)
        {
        return orientation;
        }
        
    public Particle(SimHorde horde, String params) 
        {
        super(new SimplePortrayal2D(), Color.BLUE);
        setShape(SHAPE_KITE);
        scale = 0.25;
        horde.markers.setObjectLocation(this, new Double2D(0,0));
        orientation = 0;
        }
                
    public String toString() { return ""; } // override this later to add a label
    }
