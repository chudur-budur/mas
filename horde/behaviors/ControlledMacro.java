package sim.app.horde.behaviors;


import sim.app.horde.*;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class ControlledMacro extends TrainableMacro
{
	private static final long serialVersionUID = 1L;
	public ControlledMacro()
	{
		super();
	}

	/** Saves out the Trainable Macro. */
	public void save()
	{
		System.err.println("Cannot save the ControlledMacro");
	}
	/** Loads a trainable macro and sets its training to be false.  Assumes the extension is already attached to the filename.  */
	public static TrainableMacro load(String filename)
	{
		throw new RuntimeException("Cannot load a ControlledMacro");
	}

	/** Resets the TrainableMacro to the current Horde environment, including parameters and names, behaviors, and current features. */
	public TrainableMacro reset(Horde horde, Target[] parameters, String[] parameterNames, Behavior[] behaviors, Feature[] features)
	{
		return super.reset(horde, parameters, parameterNames, behaviors, features);
	}

	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.goBypass(agent, parent, horde);

		System.err.println("AGENT: " + agent);

		if (currentBehavior == UNKNOWN_BEHAVIOR) // should never happen
			throw new RuntimeException("go() called on UNKNOWN_BEHAVIOR. This should not be able to happen.");

		int newBehavior = currentBehavior;

		synchronized(lock)
		{
			newBehavior = indexOfBehaviorNamed(desiredBehavior);
			if (newBehavior == -1)
			{
				System.err.println("ERROR: No such behavior called " + desiredBehavior);
				newBehavior = currentBehavior;
			}
		}

		if (behaviors[newBehavior] instanceof Flag)  // like "done"
		{
			fireFlag(((Flag)(behaviors[newBehavior])).getFlag(), agent, parent, horde);
			newBehavior = INITIAL_BEHAVIOR;     // immediately transition
		}
		if (newBehavior != currentBehavior)
		{
			behaviors[currentBehavior].stop(agent, this, horde);

			behaviors[newBehavior].start(agent, this, horde);
			resetFlags();
			// don't signal done here in the horde (no signalFlag(...))
		}
		currentBehavior = newBehavior;

		behaviors[currentBehavior].go(agent, this, horde);
	}

	public static String desiredBehavior = "Stop";
	public static Object lock = new Object[0];  // arrays are serializable, Objects are not

	public static final String HOST = "localhost";
	public final static int PORT = 6000;
	public static final int NUM_FEATURES = 4;
	public static PrintStream toSocket = null;
	static
	{
		try
		{
			Socket sock = new Socket(HOST, PORT);
			final InputStream i = sock.getInputStream();
			final OutputStream o = sock.getOutputStream();

			// build the input stream to read incoming desired behavior
			new Thread(new Runnable()
			{
				public void run()
				{
					Scanner scan = new Scanner(i);
					while(true)
					{
						String val = scan.nextLine();
						synchronized(lock)
						{
							desiredBehavior = val;
						}
					}
				}
			}).start();

			// build the output stream
			toSocket = new PrintStream(o);
		}
		catch (IOException e)
		{
			throw new RuntimeException("FAILED TO OPEN AND SET UP SOCKET", e);
		}
	}
}


