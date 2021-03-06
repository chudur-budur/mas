package sim.app.horde.scenarios.forage.features;

import sim.app.horde.*;
import sim.app.horde.agent.Agent;
import sim.app.horde.agent.SimAgent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.*;
import sim.app.horde.targets.Target;

public class IsAttached extends Feature
    {
    private static final long serialVersionUID = 1L;
    
    public static final String getType() { return "forager"; }

    public IsAttached()
        {
        super("IsAttached");
        targets = new Target[0];
        targetNames = new String[0];
        }

    public double getValue(Agent agent, Macro parent, Horde horde)
        {
        SimAgent simagent = (SimAgent) agent;
        if (simagent.manipulated != null) return 1;
        return 0;
        }

    }
