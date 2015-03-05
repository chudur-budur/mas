/*
 * Copyright 2006 by Sean Luke and George Mason University Licensed under the Academic Free License version 3.0 See the
 * file "LICENSE" for more information
 */
package sim.app.horde;

import sim.app.horde.behaviors.*;
import sim.app.horde.features.Feature;
import sim.portrayal.simple.*;

/**
 * AGENT
 */

public class Agent extends OvalPortrayal2D /* unavoidable */
{
	private static final long serialVersionUID = 1L;
	public Horde horde;
	// private Behavior behavior;
	public Behavior behavior;
	public boolean started = false;
	public ControllerAgent controller = null;

	public int getBasicCounter()
	{
		if (behavior == null) return 0;
		if (!(behavior instanceof Macro)) return 0;
		return ((Macro)behavior).getCounter(Macro.COUNTER_BASIC);
	}

	public int level;

	public Agent(Horde horde)
	{
		this(horde, 0);
	}

	public Agent(Horde horde, int l)
	{
		this.horde = horde;
		level = l;
	}

	public Behavior getBehavior()
	{
		return behavior;
	}

	public Behavior getUnderlyingBehavior()
	{
		if (behavior == null || !(behavior instanceof Macro)) return null;
		Macro macro = ((Macro) behavior);
		if (macro.currentBehavior == Macro.UNKNOWN_BEHAVIOR)
			return null;
		else
			return macro.behaviors[macro.currentBehavior];
	}

	public Behavior[] getUnderlyingBehaviorArray()
	{
		if (behavior == null || !(behavior instanceof Macro)) return null;
		Macro macro = ((Macro) behavior);
		return macro.behaviors;
	}

	public boolean isTheTrainingAgent()
	{
		return horde.getTrainingAgent() == this;
	}

	public boolean isTraining()
	{
		if (behavior instanceof TrainableMacro)
			return isTheTrainingAgent() && ((TrainableMacro) behavior).isRecording();
		return false;
	}

	public void restart(Horde horde)
	{
		System.err.println("--- Agent.restart() : calling behavior.stop()");
		if (started) behavior.stop(this, null, horde);
		started = false;
	}

	public void go()
	{
		if (!started)
		{
			System.err.println("--- Agent.go() : staring "
			                   + behavior.toString());
			behavior.start(this, null, horde);
			started = true;
		}
		behavior.go(this, null, horde);
	}

	/**
	 * Returns the index of the provided behavior in the current training macro,
	 * Throws an exception if the provided behavior does not exist.
	 */
	public int indexOfBehavior(Class behavior)
	{
		Behavior[] behaviors = horde.getTrainingMacro().behaviors;
		for (int i = 0; i < behaviors.length; i++)
			if (behaviors[i].getClass() == behavior) return i;
		throw new RuntimeException("Class not found among behaviors: "
		                           + behavior);
	}

	/**
	 * Returns the index of the provided feature in the current training macro,
	 * Throws an exception if the provided feature does not exist.
	 */
	public int indexOfFeature(Class feature)
	{
		Feature[] features = horde.getTrainingMacro().features;
		for (int i = 0; i < features.length; i++)
			if (features[i].getClass() == feature) return i;
		throw new RuntimeException("Class not found among behaviors: " + feature);
	}

	public void setBehavior(Behavior behavior)
	{
		System.err.println("--- Agent.setBehavior() :"
		                   + " setting behaviour = "
		                   + behavior.toString());
		this.behavior = behavior;
	}
}
