package sim.app.horde.scenarios.forage;

import java.awt.Color;
import java.awt.Graphics2D;

import sim.app.horde.*;
import sim.portrayal.DrawInfo2D;
import sim.util.*;
import sim.app.horde.scenarios.forage.behaviors.*;
import sim.app.horde.behaviors.*;
import sim.engine.SimState;

public class Forager extends SimAgent
{
	private static final long serialVersionUID = 1L;

	public final static Color HAS_FOOD_PAINT = Color.green;
	public final static Color HAS_NO_FOOD_PAINT = Color.blue;

	public double MINIMUM_DEPOSIT_DISTANCE = 5;

	public static final int NUM_STATUSES = 3;
	public static final int FREE_STATUS = 0;
	public static final int ATTACHED_STATUS = 4;
	public static final int SEE_BOX_STATUS = 2;

	// am i close enough to home? used by CodedAgent and Box
	public static final int HOME_RANGE = 5;

	public static final int RANGE = 10; // how far can the agent see a box?

	public Forager(ForageHorde horde)
	{
		super(horde);
		paint = Color.red;
		setPose(new Double2D(horde.width / 2, horde.height / 2), horde.random.nextDouble() * Math.PI * 2);
	}

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		super.drawBypass(object, graphics, info);

		/*if (biggestAttachedAgent != null) {
		  Paint tmpPaint = paint;
		  paint = Color.BLACK;
		  Double2D l = biggestAttachedAgent.getLocation();
		  double dx = l.x - loc.x;
		  double dy = l.y - loc.y;
		  int x2 = (int) (info.draw.x + info.draw.width * dx);
		  int y2 = (int) (info.draw.y + info.draw.height * dy);

		  graphics.drawLine((int) info.draw.x, (int) info.draw.y, x2, y2);
		  paint = tmpPaint;
		  } */

	}

	public int getManipulatedStatus()
	{
		if (manipulated != null) return manipulated.getTargetStatus(this, horde);
		return -1;
	}

	public void setManiupatedStatus(int s)
	{
		if (manipulated != null && s > 0) manipulated.setTargetStatus(this, horde, s);
	}

	public boolean getAttachToBox()
	{
		return (manipulated != null);
	}

	public void setAttachToBox(boolean val)
	{
		if (val)
			new Grab().go(this, (Macro) (this.getBehavior()), horde);
		else
			new Release().go(this, (Macro) (this.getBehavior()), horde);
	}

	public void step(SimState state)
	{
		if (controller != null) biggestAttachedAgent = controller.biggestAttachedAgent;
		if (manipulated != null) biggestBox = manipulated.getMinimumAttachments();

		SimAgent attachedAgent = SimAgent.attachedAgent(this);

		if (attachedAgent != null)
		{
			if (attachedAgent.manipulated == null)
				informParent();
			else if (manipulated != null && attachedAgent.manipulated.loc != manipulated.loc)
			{
				setAttachToBox(false);
				biggestAttachedAgent = attachedAgent;
				biggestBox = attachedAgent.biggestBox;
			}
		}

		super.step(state);

		/*String s = getUnderlyingBehavior().getName();
		  if (s != "Start" && s != "Done") {
		  System.out.println(getUnderlyingBehavior());
		  System.exit(-1);
		  } */

	}

	public void informParent()
	{
		SimAgent parent = getParent(this);
		parent.resetBiggest();
	}

	SimAgent getParent(SimAgent f)
	{
		if (f.controller == null || f.controller.biggestAttachedAgent == null
		        || (f.biggestBox > 0 && f.biggestBox < f.controller.biggestBox)) return f;
		f.resetBiggest();
		return getParent(f.controller);
	}
}
