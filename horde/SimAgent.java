/*
 * Copyright 2006 by Sean Luke and George Mason University Licensed under the Academic Free License version 3.0 See the
 * file "LICENSE" for more information
 */
package sim.app.horde;

import sim.engine.*;
import sim.util.*;
import sim.portrayal.*;
import sim.app.horde.behaviors.*;

import java.awt.*;
import sim.app.horde.objects.*;

/**
 * SIMAGENT
 */

public class SimAgent extends Agent implements Steppable, Orientable2D, Targetable
{
	private static final long serialVersionUID = 1;
	protected Double2D loc;
	protected Double2D prevLoc;
	protected double orientation = 0;
	protected Double2D orientationVector;
	protected int status = 0;
	protected int targetIndex = -1; // if I am a parameter target(A/B/C), what am I? Else I'm -1
	protected int rank;
	public Stoppable stoppable;
	protected double distanceTraveled;

	/*
	  public SimAgent biggestAttachedAgent = null;

	  public SimAgent getClosestAgent()
	  {
	  return biggestAttachedAgent;
	  }

	  public static SimAgent attachedAgent(SimAgent f)
	  {
	  if (f.controller == null)   // i am the root
	  return f.biggestAttachedAgent;

	  SimAgent a = attachedAgent(f.controller);

	  if (a == null)
	  return f.biggestAttachedAgent;
	  return a;
	  }

	  public void resetBiggest()
	  {
	  biggestBox = -1;
	  biggestAttachedAgent = null;
	  }

	  public int biggestBox = -1;

	  public int getBiggestBox()
	  {
	  return biggestBox;
	  }
	*/

	public boolean getFinished()
	{
		if (getBehavior() == null) return false;
		return ((Macro) getBehavior()).finished;
	}

	public void setFinished(boolean b)
	{
		((Macro) getBehavior()).finished = b;
	}

	public boolean setSelected(LocationWrapper wrapper, boolean selected)
	{
		// When selected, the agent will get this called with 'true', then with 'false'.
		// When deselected, the agent won't have this called at all -- don't ask! :-) It's
		// just for drawing. But we can override it by changing the training agent.
		// What we'll do is trade behaviors with the current training agent
		// Behavior other = horde.trainingAgent.behavior;
		// horde.trainingAgent.behavior = behavior;
		// behavior = other;
		// now we'll claim to be the training agent
		if (horde.trainingLevel == level)
		{
			// I'll allow myself to be selected
			horde.trainingAgent = this;
			return super.setSelected(wrapper, selected);
		}
		else
			return false;
	}

	public void drawBypass(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		super.draw(object, graphics, info);
	}

	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		// first draw basic stuff -- target information, etc.
		if (targetIndex >= 0)
			paint = SimHorde.parameterObjectColor[targetIndex];
		else if (this == horde.getTrainingAgent())
			paint = Color.black;
		else
			paint = Color.gray;
		super.draw(object, graphics, info);
	}

	public void setParameterValue(int index)
	{
		targetIndex = index;
	}

	public boolean getTargetIntersects(Agent agent, Horde horde, Double2D location, double slopSquared)
	{
		return getTargetLocation(agent, horde).distanceSq(location) <= slopSquared;
	}

	public Double2D getTargetLocation(Agent agent, Horde horde)
	{
		return getLocation();
	}

	public void setTargetLocation(Agent agent, Horde horde, Double2D location)
	{
		setLocation(location);
	}

	public void setTargetStatus(Agent agent, Horde horde, int status)
	{
		setStatus(status);
	}

	public void setStatus(int status)
	{
		this.status |= status;
	}

	/*    public void clearStatus(int s)
	      {
	      status &= s;
	      }
	*/

	public int getTargetStatus(Agent agent, Horde horde)
	{
		return getStatus();
	}

	public int getStatus()
	{
		return status;
	}

	public int getTargetRank(Agent agent, Horde horde)
	{
		return getRank();
	}

	public int getRank()
	{
		return rank;
	}

	public void setTargetRank(Agent agent, Horde horde, int rank)
	{
		setRank(rank);
	}

	public void setRank(int rank)
	{
		this.rank = rank;
	}

	public SimAgent(Horde horde)
	{
		this(horde, 0);
	}

	public SimAgent(Horde horde, int l)
	{
		super(horde, l);
		setPose(new Double2D(0, 0), 0);
		rank = ((SimHorde)horde).assignRank();
		resetDistanceTraveled();
		// resetBiggest();
	}

	public void setPose(Double2D location, double orientation)
	{
		setLocation(location);
		setOrientation(orientation);
		prevLoc = loc;
	}

	public Double2D getLocation()
	{
		return loc;
	}

	public Double2D getPrevLocation()
	{
		return prevLoc;
	}

	public double getDistanceTraveled()
	{
		return distanceTraveled;
	}

	public void resetDistanceTraveled()
	{
		distanceTraveled = 0;
	}

	public Interval domOrientation()
	{
		return new Interval(-Math.PI, Math.PI);
	}

	public double getOrientation()
	{
		return orientation;
	}

	public Double2D getOrientationVector()
	{
		return orientationVector;
	}

	public void setLocation(Double2D location)
	{
		loc = location;
		((SimHorde) horde).agents.setObjectLocation(this, location);
	}

	public void setOrientation2D(double val)
	{
		setOrientation(val);
	}

	public void setOrientation(double val)
	{
		double x = Math.cos(val);
		double y = Math.sin(val);
		//orientation = Math.atan2(y, x); // normalize to between -pi and +pi

		// [[ cheaper normalization, saves an atan2 -- Sean]]
		orientation = Utilities.normalizeAngle(val);
		orientationVector = new Double2D(x, y);
	}

	public double orientation2D()
	{
		return orientation;
	}

	public boolean collision(Double2D location)
	{
		return Utilities.collision(horde, location, this);
	}

	boolean stuck;
	public void setStuck(boolean val)
	{
		stuck = val;
	}

	public boolean getStuck()
	{
		return stuck;
	}

	public void restart(Horde horde)
	{
		if (started) resetDistanceTraveled();
		super.restart(horde);
		//resetBiggest();
	}

	public void step(SimState state)
	{
		// System.err.println("--- SimAgent.step(): stepping SimAgent");
		distanceTraveled += this.prevLoc.distance(loc);
		prevLoc = loc;
		super.go(); // does additional "step" stuff that's shared between the humanoid and simulation versions
		nudge((Horde) state, loc, prevLoc);
	}


	// object manipulation

	public Targetable manipulated = null;

	Double2D manipulatedGoalLoc = new Double2D();
	public static final double NUDGE_MULTIPLIER = 0.1;

	public Targetable getManipulated()
	{
		return manipulated;
	}

	public void setManipulated(Targetable m)
	{
		if (manipulated != null && manipulated instanceof Body)
			((Body)manipulated).decrementAttachment();

		manipulated = m;
		if (manipulated != null)
		{
			manipulatedGoalLoc = manipulated.getTargetLocation(this, horde);
			if (manipulated instanceof Body)
				((Body)manipulated).incrementAttachment();
		}

	}

	public void nudge(Horde horde, Double2D agentLoc, Double2D previousAgentLoc)
	{
		if (manipulated == null)  // nothing there
			return;

		manipulatedGoalLoc = manipulatedGoalLoc.add(agentLoc.subtract(previousAgentLoc));

		if (manipulated instanceof Body)
		{
			if (((Body)manipulated).collision(agentLoc))  // don't move yet
				return;
		}

		Double2D curloc = manipulated.getTargetLocation(this, horde);
		Double2D diff = manipulatedGoalLoc.subtract(curloc).multiply(NUDGE_MULTIPLIER);
		manipulated.setTargetLocation(this, horde, curloc.add(diff));
	}
}
