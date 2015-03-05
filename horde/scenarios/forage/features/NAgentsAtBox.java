package sim.app.horde.scenarios.forage.features;

import sim.app.horde.Agent;
import sim.app.horde.Horde;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.features.*;
import sim.app.horde.scenarios.forage.Forager;
import sim.app.horde.scenarios.forage.Supervisor;
import sim.app.horde.targets.Target;

public class NAgentsAtBox extends Feature
{
	private static final long serialVersionUID = 1L;

	int N;

	public NAgentsAtBox()
	{
		this(10);
	}

	public NAgentsAtBox(int n)
	{
		super("NAgentsAtBox("+n+")");
		N = n ;
		targets = new Target[0];
		targetNames = new String[0];
		level = 1;
	}

	public double getValue(Agent agent, Macro parent, Horde horde)
	{
		int cnt=0;
		Supervisor s = (Supervisor)agent;
		for (int i =0; i < s.subsidiaryAgents.size(); i++)
		{
			if (((Forager)s.subsidiaryAgents.get(i)).getStatus() == Forager.ATTACHED_STATUS)
			{
				cnt++;
			}
		}

		if (cnt >= N) return 1;
		return 0;
	}
}


