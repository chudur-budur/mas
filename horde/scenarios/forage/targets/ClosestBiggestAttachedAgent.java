package sim.app.horde.scenarios.forage.targets;

import sim.app.horde.*;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.targets.*;

public class ClosestBiggestAttachedAgent extends Target
{
	private static final long serialVersionUID = 1L;

	public Targetable getTargetable(Agent agent, Macro parent, Horde horde)
	{
		return SimAgent.attachedAgent((SimAgent)agent);
	}

	public String toString()
	{
		return "Biggest Attached Agent in Swarm";
	}
}
