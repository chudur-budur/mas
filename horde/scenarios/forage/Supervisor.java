package sim.app.horde.scenarios.forage;

import java.awt.Graphics2D;

import sim.app.horde.*;
import sim.engine.*;
import sim.portrayal.DrawInfo2D;
import sim.util.Double2D;
import sim.app.horde.behaviors.*;
import sim.app.horde.features.Feature;

public class Supervisor extends ControllerAgent
{
	private static final long serialVersionUID = 1L;

	public SimAgent[] agentStatus = new SimAgent[Forager.NUM_STATUSES];
	public int[] agentStatusCount = new int[Forager.NUM_STATUSES];

	public Agent doneAgent = null;
	public int doneCount = 0;
	protected boolean coded = false;
	public int numAgentsBelowMe = -1;

	public int getNumAgentsBelowMe()
	{
		return numAgentsBelowMe;
	}

	public Supervisor(Horde horde, int level)
	{
		super(horde, level);

		for (int i = 0; i < Forager.NUM_STATUSES; i++)
			agentStatus[i] = null;

		numAgentsBelowMe = ((ForageHorde) horde).getNumAgentsBelowMe(level);

		/*ArrayList<Behavior> behav = new ArrayList<Behavior>();

		  behav.add(new Supervise());
		  if (level > 1) {
		  Behavior b = new Supervise();
		  for (int i = 0; i < level - 1; i++)
		  b = new LevelBehavior(b);
		  behav.add(new LevelBehavior(new Supervise()));
		  }
		  else
		  behav.add(new LevelBehavior(new GrabBox()));

		  Behavior[] behaviors = behav.toArray(new Behavior[0]);

		  setBehavior(new CodedBehavior(new Feature[] {}, behaviors)); */
	}

	public SimAgent getAgentStatus(int status)
	{
		return agentStatus[getIndex(status)];
	}

	int getIndex(int value)
	{
		return (value == 0) ? 0 : (int) (Math.log10(value) / Math.log10(2));
	}

	public void step(SimState state)
	{
		super.step(state);

		for (int i = 0; i < Forager.NUM_STATUSES; i++)
		{
			agentStatus[i] = null;
			agentStatusCount[i] = 0;
		}

		doneAgent = null;
		doneCount = 0;

		int tmpBox = biggestBox;
		SimAgent tmpAgent = biggestAttachedAgent;

		resetBiggest();

		for (int i = 0; i < subsidiaryAgents.size(); i++)
		{
			SimAgent f = (SimAgent) subsidiaryAgents.get(i);

			if (f.getFinished())
			{
				setFinished(true);
				((Macro) getBehavior()).setFlag(Macro.FLAG_DONE, true);
			}

			// load status
			int idx = getIndex(f.getStatus());
			agentStatusCount[idx]++;
			if (agentStatus[idx] == null) agentStatus[idx] = f;

			// load done
			if (((Macro) (f.getBehavior())).getFlag(Macro.FLAG_DONE))
			{
				doneCount++;
				doneAgent = f;
			}

			if (f.biggestBox > biggestBox)   // a child knows of a bigger box
			{
				if (f instanceof ControllerAgent)
				{
					Supervisor s = (Supervisor) f;
					if (f.biggestBox > s.numAgentsBelowMe)
					{
						biggestBox = f.biggestBox;
						biggestAttachedAgent = s.biggestAttachedAgent;
					}
				}
				else
				{
					biggestBox = f.biggestBox;
					biggestAttachedAgent = f;
				}

			}


		}

		if (tmpBox > biggestBox)   // my parent knows of a bigger box than my children
		{
			biggestBox = tmpBox;
			biggestAttachedAgent = tmpAgent;
			restart(horde);
		}

		// if i know of a box, let everyone below me know
		if (biggestBox > -1) updateBiggest();
	}

	void updateBiggest()
	{
		for (int i = 0; i < subsidiaryAgents.size(); i++)
		{
			SimAgent f = (SimAgent) subsidiaryAgents.get(i);

			if (f instanceof ControllerAgent)
			{
				f.biggestBox = biggestBox;
				f.biggestAttachedAgent = biggestAttachedAgent;
				((Supervisor) f).updateBiggest();
			}
		}
	}

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		if (biggestAttachedAgent != null)
		{
			Double2D l = biggestAttachedAgent.getLocation();
			double dx = l.x - loc.x;
			double dy = l.y - loc.y;
			int x2 = (int) (info.draw.x + info.draw.width * dx);
			int y2 = (int) (info.draw.y + info.draw.height * dy);
			graphics.drawLine((int) info.draw.x, (int) info.draw.y, x2, y2);
		}
		super.drawBypass(object, graphics, info);
	}

}
