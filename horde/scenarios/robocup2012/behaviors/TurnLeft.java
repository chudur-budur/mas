package sim.app.horde.scenarios.robocup2012.behaviors;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.scenarios.robocup2012.HumanoidAgent;

public class TurnLeft extends Behavior
{
	private static final long serialVersionUID = 1L;

	public TurnLeft()
	{
		name = "Turn-Left";
	}

	public boolean getShouldAddDefaultExample()
	{
		return false;
	}


	public void start(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent, parent, horde);

		HumanoidAgent hAgent = (HumanoidAgent)agent;

		System.out.println("Turn Left");

		((HumanoidAgent)agent).humanoid.doMotion(HumanoidAgent.RCB4_MOT_LEFT_TURN_BUTTON);

		/*
		          if (hAgent.isTraining())
		          hAgent.humanoid.doMotion(HumanoidAgent.RCB4_MOT_LEFT_TURN_BUTTON);
		          else
		          hAgent.humanoid.pushButton(HumanoidAgent.LEFT_TURN_BUTTON);
		*/
	}

}
