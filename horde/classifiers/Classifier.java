package sim.app.horde.classifiers;

import java.io.*;
import ec.util.MersenneTwisterFast;

public abstract class Classifier implements Serializable, Cloneable
{
	private static final long serialVersionUID = 1L;

	// Domain associated with this Classifier
	protected Domain domain;

	// name of the file holding the dot input of this classifier, assumes same file system location
	// as this Java file
	public final String modelFileName = "model.dot";

	// name of the script to run dot, assumes same file system location
	// as this Java file
	final String displayScript = "display.sh";

	public void setDomain(Domain d)
	{
		domain = d;
	}

	public Domain getDomain()
	{
		return domain;
	}

	public Object clone()
	{
		try
		{
			Classifier f = (Classifier) (super.clone());

			// clone domain
			f.domain = domain;
			return f;
		}
		catch (CloneNotSupportedException e)
		{
			return null; /* never happens */
		}
	}

	public Classifier()
	{
		domain = null;
	}

	public Classifier(Domain d)
	{
		domain = d;
	}

	/**
	 * Returns the pathname of a file in the same directory as this class,
	 * regardless of operating system.
	 */
	public static String getPathInDirectory(String s)
	{
		return Classifier.class.getResource("").getPath() + "/" + s;
	}

	/**
	 * Returns the classification of the given example. Does not examine nor
	 * modify e.classification.
	 */
	public abstract int classify(Example e, MersenneTwisterFast random);

	/**
	 * Builds the classifier based on the given examples.
	 */
	public abstract void learn(Example[] examples, MersenneTwisterFast random);

	/**
	 * Writes a textual representation of this classifier.  Used to send
	 * classifier over networks.
	 */
	public abstract void write(PrintWriter writer);

	/**
	 * Launches a script to display a classifier. Subclasses should override
	 * this to populate Classifier.modelFileName with appropriate data for
	 * running dot.
	 */
	public void show(int index, String label) throws IOException, InterruptedException
	{
		System.err.println("--- Classifier.show()");
		String cmd = Classifier.getPathInDirectory(displayScript);
		String file = Classifier.getPathInDirectory(modelFileName);
		Runtime run = Runtime.getRuntime();
		System.err.println("Launching:  " + cmd + " " + file + " " + index);
		Process pr = run.exec(new String[] { cmd, file, "" + index });

		System.err.println("Waiting...");
		pr.waitFor();
	}

	/**
	 * Writes out the classifier in a DOT file format.
	 */
	public abstract void writeDotFile(String file, String label) throws IOException;

	/**
	 * If writedomain is false, we don't bother writing it.
	 */
	public abstract void writeClassifier(PrintWriter writer, boolean writeDomain);

	/**
	 * String representation of this classifier.
	 */
	public abstract String toString();




}
