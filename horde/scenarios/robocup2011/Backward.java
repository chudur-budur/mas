package sim.app.horde.scenarios.robocup2011;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;

public class Backward extends Behavior
{
	private static final long serialVersionUID = 1L;

	public Backward()
	{
		name = "Backward";
	}
	public void start(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent, parent, horde);
		((HumanoidAgent)agent).pushButton(HumanoidAgent.BACKWARD_BUTTON);
	}
}
