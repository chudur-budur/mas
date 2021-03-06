package sim.app.horde.scenarios.pioneer.behaviors;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.scenarios.pioneer.PioneerAgent;

public class Forward extends Behavior
{
	private static final long serialVersionUID = 1L;

	public Forward()
	{
		name = "Forward";
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);
		((PioneerAgent)agent).move(PioneerAgent.FORWARD);
	}
}
