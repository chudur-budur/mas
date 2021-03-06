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
import sim.app.horde.agent.*;
import java.awt.*;
import sim.app.horde.objects.*;
import sim.app.horde.behaviors.*;
import java.util.*;


public class RealMarker extends Marker implements Real
    {
    private static final long serialVersionUID = 1;

    public RealMarker(String label) { super(label); }

    public Double2D getRealTargetLocation(Agent agent, Macro parent, Horde horde)
        {
        Double2D d = ((SimHorde)horde).markers.getObjectLocation(this);
        return new Double2D((d.x - 30) * 0.1, (d.y - 20) * 0.1);
        }
   
    public double getOrientation(Agent agent, Macro parent, Horde horde) { return 0; }

        
    public RealMarker(SimHorde horde, String params)
        {
        super("");
        Scanner vscan = new Scanner(params);
        Double2D loc = new Double2D(vscan.nextDouble(), vscan.nextDouble());
        int index = vscan.nextInt();
        Paint paint = horde.scanPaint(vscan.next());
        String name = vscan.nextLine().trim();  // the rest of the line is the name

        setLabel(name);
        setDefaultPaint(paint);
        horde.markerTable.add(this);
        horde.markers.setObjectLocation(this, loc);
        if (index >= 0) horde.setParameterObject(index, this);
        }

    }
