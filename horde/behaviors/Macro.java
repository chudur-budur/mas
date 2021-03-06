package sim.app.horde.behaviors;

import sim.app.horde.transitions.*;
import sim.app.horde.targets.*;
import sim.app.horde.*;

import java.io.*;
import java.util.*;


/** MACRO

    <p>A Behavior which is a simple composition of other Behaviors arranged in
    a finite-state automaton (the behaviors are states in the automaton).  Transitions
    are TRANSITION objects which you may specify.  There is one transition object
    for each behavior in the automaton.

    <p>You can hard-code a Macro by simply setting up behaviors and transitions
    as you like.  However most Macros are learned, using a subclass called TRAINABLEMACRO.

    <p>Macros have a special flag called 'done' which signals that the Macro believes
    that it is done.  There is a feature called DONE which senses this flag and can be
    used in parent macros to move away from this Macro now that it's done.  It's up to the
    user to take advantage of this or not.

    <p>Macros begin with the current behavior set at UNKNOWN_BEHAVIOR.  When start() is called,
    the macro's current behavior is then set to INITIAL_BEHAVIOR.  Then start() is called
    on INITIAL_BEHAVIOR.

    <p>When go() is called, the Macro works as follows.  First, it calls the transition associated
    with the current behavior.  This transition function tells it where to transition to.  It then
    sets the current behavior to this new value.  If the new current behavior differs from the old
    current behavior, stop() is called on the old behavior and start() is called on the new behavior.
    Then it calls go() on the new current behavior.  This means that INITIAL_BEHAVIOR may never
    have go() called on it -- we could immediately transition to a different behavior.

    <p>When stop() is called, the Macro calls stop() on its underlying current behavior.
*/


public class Macro extends Behavior
{
	private static final long serialVersionUID = 1;

	// Flags
	public static final int NUM_FLAGS = 2;
	public static final int FLAG_DONE = 0;
	public static final int FLAG_FAILED = 1;
	boolean[] flags = new boolean[NUM_FLAGS];
	public boolean getFlag(int flag)
	{
		return flags[flag];
	}
	public void setFlag(int flag, boolean val)
	{
		flags[flag] = val;
	}
	public void resetFlags()
	{
		for(int i = 0; i < NUM_FLAGS; i++) flags[i] = false;
	}


	// Counters
	public static final int NUM_COUNTERS = 1;
	public static final int COUNTER_BASIC = 0;
	int[] counters = new int[NUM_COUNTERS];
	public int getCounter(int counter)
	{
		return counters[counter];
	}
	public void setCounter(int counter, int val)
	{
		counters[counter] = val;
	}
	public void incrementCounter(int counter, int delta)
	{
		counters[counter]+= delta;
	}
	public void resetCounters()
	{
		for(int i = 0; i < NUM_COUNTERS; i++) counters[i] = 0;
	}


	// Associated objects
	// If the target is null, it's assumed to be the "default target" -- Me, for example
	public static final int NUM_ASSOCIATED_OBJECTS = 1;
	public static final int ASSOCIATED_OBJECT_BASIC = 0;
	Targetable associatedObjects[] = new Targetable[NUM_ASSOCIATED_OBJECTS];
	public Targetable getAssociatedObject(int assoc)
	{
		return associatedObjects[assoc];
	}
	public void setAssociatedObject(int assoc, Targetable val)
	{
		associatedObjects[assoc] = val;
	}
	public void resetAssociatedObjects()
	{
		for(int i = 0; i < NUM_ASSOCIATED_OBJECTS; i++) associatedObjects[i] = null;
	}


	public Behavior[] behaviors;
	public Transition[] transitions;
	public int currentBehavior = UNKNOWN_BEHAVIOR;

	public Object clone()
	{
		Macro f = (Macro)(super.clone());

		// clone behaviors
		f.behaviors = new Behavior[behaviors.length];
		for(int i = 0 ; i < f.behaviors.length; i++)
			if (behaviors[i] != null)
				// && (behaviors[i] instanceof Macro ||
				// behaviors[i] instanceof LevelBehavior))
				// oops, even basic behaviors need to be cloned,
				// because they may have different targets
			{
				f.behaviors[i] = (Behavior)(behaviors[i].clone());
			}
		// else f.behaviors[i] = behaviors[i];  // just copy over

		// clone transitions
		f.transitions = new Transition[transitions.length];
		for(int i = 0 ; i < f.transitions.length; i++)
			f.transitions[i] = (transitions[i] == null ? null :
			                    (Transition)(transitions[i].clone()));

		// clone flags, counters, associatedObjects
		f.flags = (boolean[])(flags.clone());
		f.counters = (int[])(counters.clone());

		if (associatedObjects != null)
			// we don't clone the target
			f.associatedObjects = (Targetable[])(associatedObjects.clone());
		return f;
	}

	// this will be "START"
	public static final int INITIAL_BEHAVIOR = 0;
	public static final int UNKNOWN_BEHAVIOR = -1;

	public Behavior[] getBehaviors()
	{
		return behaviors;
	}
	public Transition[] getTransitions()
	{
		return transitions;
	}
	public int getCurrentBehavior()
	{
		return currentBehavior;
	}
	protected String getUniqueName()
	{
		return "M_" + getName();
	}

	public Macro()
	{
		// dummy initial behaviors and transitions
		this(new Behavior[] { new Behavior() },
		new Transition[] { new Transition() });
	}

	public Macro(Behavior[] behaviors, Transition[] transitions)
	{
		super();
		this.behaviors = behaviors;
		this.transitions = transitions;
		name = "Macro";
		currentBehavior = UNKNOWN_BEHAVIOR;
	}

	/**
	 * Searches through the behavior array for a behavior which matches
	 * the given name, and returns its behavior index number.
	 */
	public int indexOfBehaviorNamed(String name)
	{
		for(int i = 0 ; i < behaviors.length; i++)
			if (behaviors[i].getName().equalsIgnoreCase(name)) return i;
		return -1;
	}

	public void start(Agent agent, Macro parent, Horde horde)
	{
		super.start(agent, parent, horde);
		System.err.println("--- Macro.start(): starting.");
		currentBehavior = INITIAL_BEHAVIOR;
		resetCounters();
		resetAssociatedObjects();
		if (behaviors[currentBehavior] !=null && behaviors[currentBehavior] instanceof Flag)
			throw new RuntimeException("INITIAL BEHAVIOR == "
			                           + behaviors[currentBehavior]
			                           + "!  This should never be allowed to happen.");
		resetFlags();
		behaviors[currentBehavior].start(agent, this, horde);
		Transition transition = transitions[currentBehavior];
		if (transition != null) transition.start(agent, this, horde);
		if (horde.observer != null) horde.observer.transitioned(this, UNKNOWN_BEHAVIOR,
			        INITIAL_BEHAVIOR);
		finished = false;
	}

	public void stop(Agent agent, Macro parent, Horde horde)
	{
		super.stop(agent, parent, horde);
		System.err.println("--- Macro.stop(): stopping.");
		if (currentBehavior != UNKNOWN_BEHAVIOR)
		{
			behaviors[currentBehavior].stop(agent, this, horde);
			Transition transition = transitions[currentBehavior];
			if (transition != null) transition.stop(agent, this, horde);
		}
		int oldBehavior = currentBehavior;
		currentBehavior = UNKNOWN_BEHAVIOR;
		if (horde.observer != null) horde.observer.transitioned(
			    this, oldBehavior, currentBehavior);
		finished = true;
	}

	/**
	 * Calls super.go() but doesn't do any Macro.go() code,
	 * Used by TrainableMacro to do its own go() method but
	 * still use Behavior.go() code as well.
	 */
	public boolean talkedgbp = false ;
	protected final void goBypass(Agent agent, Macro parent, Horde horde)
	{
		if(!talkedgbp)
		{
			System.err.println("--- Macro.goBypass() : by-passing go()'s.");
			talkedgbp = true ;
		}
		super.go(agent, parent, horde);
	}

	public boolean talkedgo = false ;
	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.go(agent, parent, horde);
		if(!talkedgo)
		{
			System.err.println("--- Macro.go() : Macro's go()");
		}
		if (currentBehavior == UNKNOWN_BEHAVIOR) // should never happen
			throw new RuntimeException("go() called on UNKNOWN_BEHAVIOR."
			                           + "This should not be able to happen.");
		int newBehavior = currentBehavior;
		Transition transition = transitions[currentBehavior];
		if (transition != null) newBehavior = transition.change(agent, this, horde);
		if (behaviors[newBehavior] instanceof Flag)  // "done"
		{
			fireFlag(((Flag)(behaviors[newBehavior])).getFlag(), agent, parent, horde);
			newBehavior = INITIAL_BEHAVIOR;     // immediately transition
		}
		if (newBehavior != currentBehavior)
		{
			behaviors[currentBehavior].stop(agent, this, horde);
			if (transition != null) transition.stop(agent, this, horde);

			behaviors[newBehavior].start(agent, this, horde);
			Transition newTransition = transitions[newBehavior];
			if (newTransition != null) newTransition.start(agent, this, horde);
			resetFlags();
		}
		currentBehavior = newBehavior;
		behaviors[currentBehavior].go(agent, this, horde);
	}

	/** Signals Done and sets it in the parent */
	public void fireFlag(int flag, Agent agent, Macro parent, Horde horde)
	{
		if (parent != null) parent.setFlag(flag, true);
	}

	public void write(PrintWriter writer, HashSet<String> behaviorsSoFar) { }

	public boolean finished = false;

	public void performTransition(int newBehavior, Agent agent, Horde horde)
	{
		if (currentBehavior > -1)
		{
			behaviors[currentBehavior].stop(agent, this, horde);
			Transition trans = transitions[currentBehavior];
			if (trans != null) trans.stop(agent, this, horde);
		}

		behaviors[newBehavior].start(agent, this, horde);
		Transition newTransition = transitions[newBehavior];
		if (newTransition != null) newTransition.start(agent, this, horde);

		currentBehavior = newBehavior;
		resetFlags();
	}
}
