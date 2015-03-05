package sim.app.horde.features;

import sim.app.horde.*;
import sim.app.horde.targets.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.agent.SimAgent;
import sim.app.horde.behaviors.*;

/**
 * Returns the orientation in radians of the of the target relative to the 
 * current orientation of the agent.  A value of 0 (or 2*pi) means the agent is
 * oriented directly toward the target.  A value of pi means the agent is oriented
 * in the opposite direction.  This is a toroidal feature, so the value will go
 * from almost 2*pi to zero as the agent turns to the left.
 * 
 * @author vittorio
 */
public class TargetDirection extends ToroidalFeature
    {
    private static final long serialVersionUID = 1;

    public static String getType() { return sim.app.horde.agent.SimAgent.TYPE_SIMAGENT; }

    public TargetDirection(Target t)
        {
        this();
        targets[0] = t;
        }

    public TargetDirection()
        {
        super("DirectionTo");
        targets = new Target[1];
        targets[0] = new Me();  //  default
        targetNames = new String[]{ "X" };
        }
                
    public double getValue(Agent agent, Macro parent, Horde horde)
        {
        SimAgent simagent = (SimAgent) agent;

        if (agent == horde.getTrainingAgent())
            targets[0].getTargetable(agent, parent, horde);
        
        double a =  Utilities.relativeAngle(simagent.getLocation(),
            simagent.orientation2D(), targets[0].getLocation(simagent, parent, horde));

        return a;         
        }
    }
