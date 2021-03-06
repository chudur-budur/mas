package sim.app.horde.behaviors;
import sim.app.horde.*;
import sim.app.horde.agent.Agent;

public class Spiral extends Curve
    {
    private static final long serialVersionUID = 1;

    // Unless otherwise specified, types are global
    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }

    int t;
    double lastTSqrt;
    double rotationalRate;
    public Spiral() { this(0.1, 50); }  // good defaults for roomba
        
    public Spiral(double forwardSpeed, double rotationalRate)
        {
        super(forwardSpeed, 1);  // 1 doesn't matter
        name = "Spiral[" + forwardSpeed + "," + rotationalRate + "]";
        this.rotationalRate = rotationalRate;
        }
                
    public void start(Agent agent, Macro parent, Horde horde)
        {
        super.start(agent, parent, horde);
        // System.out.println("spiral " + horde.schedule.getTime());
        rotate.speed = 1;  // doesn't matter
        t = 0;
        lastTSqrt = 0;
        }
                
    public void go(Agent agent, Macro parent, Horde horde)
        {
        t++;
        double newTSqrt = Math.sqrt(t);
        rotate.speed = (newTSqrt - lastTSqrt) * rotationalRate;
        lastTSqrt = newTSqrt;
        super.go(agent, parent, horde);
        }
    }
