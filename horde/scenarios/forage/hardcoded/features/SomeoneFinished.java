package sim.app.horde.scenarios.forage.hardcoded.features;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.scenarios.forage.Supervisor;
import sim.app.horde.targets.*;
import sim.app.horde.features.*;

public class SomeoneFinished extends Feature
{
	private static final long serialVersionUID = 1L;

	public SomeoneFinished()
	{
		super("SomeoneFinished");
		targets = new Target[0];
		targetNames = new String[0];
		level = 1;
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		Supervisor s = (Supervisor)agent;

		for (int i=0; i < s.subsidiaryAgents.size(); i++)
		{
			Agent a = (Agent)s.subsidiaryAgents.get(i);
			Macro b = (Macro)a.getBehavior();
			if (b.finished)
				return 1;
		}
		return 0;
	}


}
