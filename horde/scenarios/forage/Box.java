package sim.app.horde.scenarios.forage;

import sim.app.horde.*;
import sim.app.horde.behaviors.*;
import sim.app.horde.objects.*;
import sim.field.continuous.*;
import sim.util.*;

public class Box extends CircularBody
{
	private static final long serialVersionUID = 1L;
	Horde horde;

	public Box(double scale, Continuous2D field, Double2D loc2)
	{
		super(scale, field, loc2);
	}

	public Box(Horde horde)
	{
		this(5, ((ForageHorde) horde).boxesField, new Double2D(0, 0));
		this.horde = horde;
		newRandomLocation();
	}

	protected void setup()
	{
		defaultPaint = paint = new java.awt.Color(64, 64, 128, 128);
		setMinimumAttachments(2);
	}

	public void newRandomLocation()
	{
		loc = new Double2D(horde.random.nextDouble() * field.getWidth(), horde.random.nextDouble() * field.getHeight());

		// set a random size, limited to the number of agents in the system
		//setMinimumAttachments(horde.random.nextInt(ForageHorde.totalNumAgents));

		field.setObjectLocation(this, loc);
	}

	public Double2D getTargetLocation(Agent agent, Horde horde)
	{
		return loc;
	}

	public void decrementAttachment()
	{
		super.decrementAttachment();
		Double2D pos = field.getObjectLocation(this);
		double dist = pos.distance(((ForageHorde) horde).getHomeBase().getTargetLocation(null, horde));
		if (getAttachments() == 0 || dist <= 2 * Forager.HOME_RANGE)   // move to new random location
		{
			ForageHorde.collectedBoxes++;
			newRandomLocation();

			while (getAttachments() > 0)
				super.decrementAttachment();

			setTargetStatus(null, null, 0);

			ForageHorde cHorde = (ForageHorde) horde;
			for (int i = 0; i < cHorde.agentList.size(); i++)
			{
				SimAgent a = (SimAgent) cHorde.agentList.get(i);
				if (a.manipulated == this)
				{
					a.manipulated = null;
					a.resetBiggest();
					a.clearStatus(0);
					if (a instanceof Forager)
						((Forager)a).informParent();
					Macro cb = (Macro) a.getBehavior();
					cb.finished = true;
					cb.setFlag(Macro.FLAG_DONE, true);

				}
			}
		}
	}
}
