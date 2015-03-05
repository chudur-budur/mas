package sim.app.horde.behaviors;

import sim.app.horde.*;
import sim.app.horde.targets.Target;

public class LevelBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;
	public String toString()
	{
		String str = "";
		for (int i=0; i < level; i++)
		{
			str += "LB: ";
		}
		return str + name;
	}


	int behaviorIndex = -1;                         // the location of the underlying behavior in the subsidiary's trainablemacro
	boolean reloadTargets = false;          // do I need to reset the targets because something has changed?

	public void setTarget(int i, Target t)
	{
		super.setTarget(i,t);
		reloadTargets = true;
	}

	public void loadTargets(ControllerAgent agent)
	{
		int s = agent.subsidiaryAgents.size();
		for (int i = 0; i < s; i++)
		{
			Agent a = (Agent)(agent.subsidiaryAgents.get(i));
			TrainableMacro m = (TrainableMacro)(a.getBehavior());
			m.behaviors[behaviorIndex].loadTargetCopiesFrom(this);          // will this break wrappers?
		}
	}

	public LevelBehavior(Behavior b)
	{
		name = b.getName();
		setKeyStroke(b.getKeyStroke());
		level = b.level + 1;

		loadTargetCopiesFrom(b);
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);
		if (reloadTargets)
		{
			ControllerAgent cAgent = (ControllerAgent) agent;
			loadTargets(cAgent);
			reloadTargets = false;
		}
	}

	// make sure all my subsidiary agents are running the same behavior
	public void start(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent, parent, horde);

		ControllerAgent cAgent = (ControllerAgent) agent;
		if (reloadTargets)
		{
			loadTargets(cAgent);
			reloadTargets = false;
		}

		if (behaviorIndex == -1)  // need to set up behavior index, use first subsidiary agent
		{
			Agent a = (Agent)(cAgent.subsidiaryAgents.get(0));
			Macro m = (Macro)(a.getBehavior());
			behaviorIndex = m.indexOfBehaviorNamed(name);
		}

		int s = cAgent.subsidiaryAgents.size();
		for (int i = 0; i < s; i++)
		{
			Agent a = (Agent)(cAgent.subsidiaryAgents.get(i));
			Macro m = (Macro)(a.getBehavior());

			if (m instanceof TrainableMacro)
			{
				TrainableMacro tm = (TrainableMacro)m;
				tm.userChangedBehavior(horde, behaviorIndex);
				tm.setIAmSlave(true);                   // so he doesn't try to transition
			}
			else
			{
				m.performTransition(behaviorIndex, a, horde);
				m.finished = false;
			}


		}
	}
}
