package sim.app.horde.scenarios.forage.features;

import sim.app.horde.*;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.forage.*;
import sim.app.horde.targets.Target;
import sim.app.horde.features.*;

public class SomeoneNeedsHelp extends Feature
{
	private static final long serialVersionUID = 1L;

	public SomeoneNeedsHelp()
	{
		super("NeedsHelp");
		targets = new Target[0];
		targetNames = new String[0];
		level = 2;
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		Supervisor s = (Supervisor) agent;

		for (int i = 0; i < s.subsidiaryAgents.size(); i++)
		{

			SimAgent a = (SimAgent)s.subsidiaryAgents.get(i);

			if (a instanceof ControllerAgent)
			{
				Supervisor sup = (Supervisor) a;
				if (sup.biggestBox > sup.numAgentsBelowMe)
					return 1;
			}
		}
		return 0;
	}
}
