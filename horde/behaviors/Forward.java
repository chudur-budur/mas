package sim.app.horde.behaviors;

import sim.app.horde.*;
import sim.util.*;
//import sim.app.horde.agent.Agent;
//import sim.app.horde.agent.SimAgent;
import sim.app.horde.objects.*;

public class Forward extends Behavior
{
	private static final long serialVersionUID = 1;
	public double speed;

	public static final String getType()
	{
		return "Basic";
	}

	public Forward()
	{
		this(0.1);
	} // good default

	public Forward(double speed)
	{
		this.speed = speed;
		name = "Forward[" + speed + "]";
		setKeyStroke(KS_UP);
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);

		SimAgent simagent = (SimAgent) agent;
		Double2D loc = simagent.getLocation();
		Double2D d = simagent.getOrientationVector();

		Double2D newLoc = new Double2D(loc.x + speed * d.x, loc.y + speed * d.y);

		// modify orientation vector if we're too far away
		SimHorde sm = (SimHorde)horde;
		if (newLoc.x < 0 || newLoc.y < 0 || newLoc.x > sm.width || newLoc.y > sm.height)
		{
			double angle = new Double2D(sm.width/2, sm.height/2).subtract(newLoc).angle();
			simagent.setOrientation(angle);
			d = simagent.getOrientationVector();
			MutableDouble2D _newLoc = new MutableDouble2D(loc.x + speed * d.x, loc.y + speed * d.y);
			if (_newLoc.x < 0) _newLoc.x = 0;
			if (_newLoc.y < 0) _newLoc.y = 0;
			if (_newLoc.x > sm.width) _newLoc.x = sm.width;
			if (_newLoc.y > sm.height) _newLoc.y = sm.height;
			newLoc = new Double2D(_newLoc);
		}

		if (simagent.collision(newLoc))
		{
			simagent.setStuck(true);
		}
		else if (simagent.manipulated != null && // we've attached to someone
		         simagent.manipulated instanceof Body &&
		         !((Body)simagent.manipulated).isEnoughAttachments()) // not enough people yet
		{
			// don't do anything -- we're not really stuck
		}
		else
		{
			simagent.setStuck(false);
			simagent.setLocation(newLoc);
		}
	}
}
