package sim.app.horde.scenarios.robot.darwin.agent;

import java.awt.Paint;
import java.util.Scanner;
import sim.app.horde.Horde;
import sim.app.horde.SimHorde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.util.Double2D;

public class VirtualAgent extends RealMarker implements Real
    {
    private static final long serialVersionUID = 1;

    public int robotNumber = 0;
    public double orientation = 0;
   
    public VirtualAgent(SimHorde horde, String params)
        {
        super("");
        Scanner vscan = new Scanner(params);
        robotNumber = vscan.nextInt();
        Paint paint = horde.scanPaint(vscan.next());
        String name = vscan.nextLine().trim();  // the rest of the line is the name
        setLabel(name);
        setDefaultPaint(paint);
        horde.markerTable.add(this);
        horde.markers.setObjectLocation(this, new Double2D(60,40));
        }

    public Double2D getRealTargetLocation(Agent agent, Macro parent, Horde horde)
        {
        Double2D d = ((SimHorde)horde).markers.getObjectLocation(this);
        return new Double2D((d.x - 30) * 0.1, (d.y - 20) * 0.1);
        }
   
    public double getOrientation() { return orientation; }
    public double getOrientation(Agent agent, Macro parent, Horde horde) { return orientation; }

    }
