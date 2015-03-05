package sim.app.horde.scenarios.forage.features;

import sim.app.horde.Agent;
import sim.app.horde.Horde;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.*;
import sim.app.horde.scenarios.forage.Forager;
import sim.app.horde.scenarios.forage.Supervisor;
import sim.app.horde.targets.*;

public class SomeoneAttached extends Feature
{

	private static final long serialVersionUID = 1L;

	public SomeoneAttached()
	{
		super("SomeoneAttached");
		targets = new Target[0];
		targetNames = new String[0];
		level = 1;
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		Supervisor s = (Supervisor)agent;
		if (s.getAgentStatus(Forager.ATTACHED_STATUS) != null) return 1;
		return 0;
	}

}
