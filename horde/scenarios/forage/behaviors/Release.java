package sim.app.horde.scenarios.forage.behaviors;

import sim.app.horde.behaviors.*;
import sim.app.horde.scenarios.forage.Forager;
import sim.app.horde.*;

public class Release extends Behavior
{
	private static final long serialVersionUID = 1;

	public Release()
	{
		name = "Release";
		setKeyStroke('r');
	}

	public boolean shouldAddDefaultExample()
	{
		return false;
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);

		SimAgent simagent = (SimAgent) agent;

		if (simagent.manipulated != null)
		{
			int status = simagent.manipulated.getTargetStatus(simagent, horde);
			if (status > 0) status--;
			simagent.manipulated.setTargetStatus(simagent, horde, status);
			simagent.manipulated.decrementAttachment();
			simagent.manipulated = null;

			parent.finished = true;
		}

		simagent.resetBiggest();
		((Forager)simagent).informParent();

		simagent.clearStatus(0);
	}

	public void stop(Agent agent, Macro parent, Horde horde)
	{
		super.stop(agent, parent, horde);
		parent.finished = true;
	}
}
