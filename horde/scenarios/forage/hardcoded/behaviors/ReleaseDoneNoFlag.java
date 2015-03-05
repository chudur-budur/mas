package sim.app.horde.scenarios.forage.hardcoded.behaviors;

import sim.app.horde.Agent;
import sim.app.horde.Horde;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.scenarios.forage.hardcoded.CodedAgent;
import sim.app.horde.scenarios.forage.hardcoded.CodedBehavior;
import sim.app.horde.scenarios.forage.behaviors.*;

public class ReleaseDoneNoFlag extends Release
{

	private static final long serialVersionUID = 1L;

	public ReleaseDoneNoFlag()
	{
		name = "ReleaseDoneNoFlag";    // good default
		setKeyStroke('R');
	}

	public boolean shouldAddDefaultExample()
	{
		return false;
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);

		CodedBehavior b = (CodedBehavior)agent.getBehavior();
		b.performTransition(CodedAgent.FORAGE, agent, horde);
		//b.finished = false;
	}

}
