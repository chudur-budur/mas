package sim.app.horde.behaviors;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;

import sim.app.horde.*;

import sim.app.horde.irl.*;

/** BEHAVIOR

    <p>The superclass of all behaviors of the agent, which manipulate the robot and
    also which may appear as states in a macro FSA behavior.

    <p>Each Behavior has some number of TARGETS which need to be specified for the
    Behavior to work.  Behaviors also have names, names for the buttons on which
    they appear in Horde, and also optional associated keystrokes.

    <p>The default behavior d oes nothing at all.
*/

public class Behavior extends Targeting // SEE NOTE BELOW
	// in fact we only want TrainableMacros to be serializable.  However since
	// TrainableMacro is a subclass of Behavior, if Behavior isn't serializable,
	// then Behavior's default constructor is called (see the documentation for
	// java.io.Serializable).  This results in unexpected behavior -- such as
	// deserialized TrainableMacros having the name "Composed".
{
	static Class[] basicBehaviorClasses;  // stores all basic behavior classes

	/**
	 * By default returns true -- override this to return false
	 * if you want to declare that when transitioning TO this behavior,
	 * no examples should be added to Horde's examples list.
	 */
	public boolean shouldAddExamples()
	{
		return true;
	}

	/**
	 * By default returns true -- override this to return false
	 * if you want to declare that when transitioning TO this behavior,
	 * no DEFAULT example should be added to Horde's examples list.
	 */
	public boolean getShouldAddDefaultExample()
	{
		return true;
	}

	private static final long serialVersionUID = 1;
	protected String name = "Behavior";
	// likely immutable so we don't have to clone it
	KeyStroke stroke = null;

	// Various convenience constants for up/down/left/right.
	public static final KeyStroke KS_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
	public static final KeyStroke KS_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);
	public static final KeyStroke KS_LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false);
	public static final KeyStroke KS_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false);

	KeyStroke keyStroke(char val)
	{
		return KeyStroke.getKeyStroke(val);
	}

	/** Sets the keystroke. */
	public void setKeyStroke(KeyStroke val)
	{
		stroke = val;
	}
	/** Sets the keystroke. */
	public void setKeyStroke(char val)
	{
		stroke = keyStroke(val);
	}

	/**
	 * Returns the Behavior's keystroke,
	 * or null if there is no associated keystroke.
	 */
	public KeyStroke getKeyStroke()
	{
		return stroke;
	}

	public String getName()
	{
		return name;
	}

	protected String getUniqueName()
	{
		return "B_" + getName() + "_" + getClass().toString();
	}

	protected Macro parent = null;

	/**
	 * Returns the Macro which presently owns and/or has
	 * called start() on this behavior, This value may be null
	 * if the Macro has never called start() on the behavior yet,
	 * getParent() is used by wrappers to determine the parent of
	 * a Macro or other behavior when computing their wrapped target value.
	 */
	public Macro getParent()
	{
		return parent;
	}

	/**
	 * Sets the parent and also calls start() on all targets,
	 * When overriding, be sure to call super()
	 */
	public void start(Agent agent, Macro parent, Horde horde)
	{
		System.err.println("--- Behavior.start(): " + toString());
		this.parent = parent;
		super.startTargets(agent, parent, horde);
	}

	/**
	 * Sets the parent and also calls stop() on all targets,
	 * When overriding, be sure to call super()
	 */
	public void stop(Agent agent, Macro parent, Horde horde)
	{
		System.err.println("--- Behavior.stop(): " + toString());
		this.parent = parent;
		super.stopTargets(agent, parent, horde);
	}

	/**
	 * Sets the parent, When overriding, be sure to call super()
	 */
	public boolean talked = false ;
	public void go(Agent agent, Macro parent, Horde horde)
	{
		//new Exception().printStackTrace(System.err);
		if(!talked)
		{
			System.err.println("--- Behavior.go() : go()");
			talked = true ;
		}
		this.parent = parent;
	}

	/**
	 * By default returns the name of the behavior.
	 */
	public String toString()
	{
		return name;
	}

	/** Returns what to display on the button,
	 * May be different than toString() if you like.
	 */
	public String getButtonName()
	{
		return name;    // what to display on the button
	}

	/**
	 * Returns all behaviors, Note that the behaviors' parameters
	 * have not yet been converted to wrappers.
	 */
	public static Behavior[] provideAllBehaviors()
	{
		return provideAllBehaviors(0);
	}

	public static Behavior[] provideAllBehaviors(int level)
	{
		// grab the macros from disk
		TrainableMacro[] tm = TrainableMacro.provideAllTrainableMacros();

		// make instances of the basic behaviors
		Behavior[] b = new Behavior[tm.length + basicBehaviorClasses.length];
		for(int i = 0; i < basicBehaviorClasses.length; i++)
			try
			{
				b[i+tm.length] =
				    (Behavior)(basicBehaviorClasses[i].newInstance());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

		// fold in the macros
		System.arraycopy(tm, 0, b, 0, tm.length);

		// filter out behaviors based on level
		ArrayList<Behavior> tmp = new ArrayList<Behavior>();
		for (int i=0; i < b.length; i++)
		{

			if (b[i] instanceof Start ||
			        b[i] instanceof Flag ||
			        b[i].level == level)
			{
				tmp.add(b[i]);
			}
			else if (b[i].level == (level - 1)) // && b[i] instanceof TrainableMacro)
			{
				tmp.add( new LevelBehavior(b[i]));
			}
		}
		b = new Behavior[tmp.size()];
		System.arraycopy(tmp.toArray(), 0, b, 0, tmp.size());

		// "Start" is always the first behavior
		for(int i = 0; i < b.length; i++)
		{
			if (b[i] instanceof Start)
			{
				// swap to front
				Behavior temp = b[i];
				b[i] = b[0];
				b[0] = temp;
				return b;
			}
		}

		System.err.println("WARNING: No \"Start\" Behavior Loaded");
		return b;
	}

	/**
	 * Loads all basic behavior classes from basic.behaviors
	 */
	static
	{
		System.err.println("--- Behaviour.static{}: loading basic behavior classes.");
		// load the basic behaviors
		ArrayList list = new ArrayList();
		Scanner scanner = new Scanner(Horde.class.getResourceAsStream(
		                                  Horde.BASIC_BEHAVIORS_LOCATION));
		while(scanner.hasNextLine())
		{
			String s = scanner.nextLine().trim();
			if (s.startsWith("#") || s.length() == 0) continue;
			try
			{
				list.add(Class.forName(s));
			}
			catch (ClassNotFoundException e)
			{
				System.err.println("Couldn't find Behavior: " + s);
			}
		}
		scanner.close();
		basicBehaviorClasses = new Class[list.size()];
		System.arraycopy(list.toArray(), 0, basicBehaviorClasses, 0, list.size());
		// this is for IRL stuffs
		ExpertDemo.BEHAVIOUR_COUNT = basicBehaviorClasses.length ;
		System.err.println("--- Behaviour.static{} : ExpertDemo.BEHAVIOUR_COUNT == "
		                   + ExpertDemo.BEHAVIOUR_COUNT);
	}

	public void write(PrintWriter writer, HashSet<String> behaviorsSoFar)
	{
		if (!behaviorsSoFar.contains(name))
		{
			writer.print(" ( behavior " + name + " " + level + " )\n\n" );
			behaviorsSoFar.add(name);
		}
	}
}
