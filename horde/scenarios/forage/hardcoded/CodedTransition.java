package sim.app.horde.scenarios.forage.hardcoded;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.features.*;
import sim.app.horde.transitions.*;

public class CodedTransition extends Transition
{
	private static final long serialVersionUID = 1L;

	Feature[] features;
	double[] currentFeatureVector;

	public CodedTransition(Feature[] f)
	{
		features = f;
		currentFeatureVector = new double[features.length];
	}

	public int change(Agent agent, Macro parent, Horde horde)
	{
		if (agent instanceof CodedAgent)
		{
			CodedAgent a = (CodedAgent) agent;

			SimAgent attachedAgent = SimAgent.attachedAgent(a);

			if (attachedAgent != null)
			{
				if (attachedAgent.manipulated == null)
					a.informParent();
				else if (a.manipulated != null && attachedAgent.manipulated.loc != a.manipulated.loc)
				{
					a.setAttachToBox(false);
					a.biggestAttachedAgent = attachedAgent;
					a.biggestBox = attachedAgent.biggestBox;
				}
			}
		}

		for (int i = 0; i < features.length; i++)
			currentFeatureVector[i] = features[i].getValue(agent, parent, horde);

		return ((CodedBehavior) parent).getNewBehavior(agent, horde, currentFeatureVector);
	}

	public void start(Agent agent, Macro parent, Horde horde)
	{
		for (int i = 0; i < features.length; i++)
			features[i].start(agent, parent, horde);
	}

	public void stop(Agent agent, Macro parent, Horde horde)
	{
		for (int i = 0; i < features.length; i++)
			features[i].stop(agent, parent, horde);
	}

}
