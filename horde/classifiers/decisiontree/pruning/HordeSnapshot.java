package sim.app.horde.classifiers.decisiontree.pruning;

import java.io.*;
import java.util.ArrayList;
import sim.app.horde.transitions.* ;
import sim.app.horde.classifiers.* ;
import sim.app.horde.classifiers.decisiontree.* ;


public class HordeSnapshot
{
	private Classifier classifier = null ;
	private Example[] currentExamles ;
	private ArrayList<TrajectoryExample> allExamples = null ;
	private String currentBehaviorName = "" ; // each classifier wants to have a unique name.
	private DecisionTree dtree = null ;
	private long mtfseed = 0L ;
	private String location = "" ;

	/**
	 * An empty Constructor.
	 */
	public HordeSnapshot()
	{
		try
		{
			java.net.URL url = getClass().getClassLoader().getResource(".");
			this.location = url.toURI().getPath() + "/logs/";
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void newTrajectory()
	{
		this.allExamples = new ArrayList<TrajectoryExample>();
	}

	/**
	 * Constructor with currentBehaviorName
	 */
	public HordeSnapshot(String name)
	{
		this.currentBehaviorName = name ;
		try
		{
			java.net.URL url = getClass().getClassLoader().getResource(".");
			this.location = url.toURI().getPath() + "/logs/";
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Not implemented yet
	 */
	public HordeSnapshot(String name, long mtfseed)
	{
		this.currentBehaviorName = name ;
		this.mtfseed = mtfseed ;
		try
		{
			java.net.URL url = getClass().getClassLoader().getResource(".");
			this.location = url.toURI().getPath() + "/logs/";
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * One of these setLearnedModel()'s must be called to make this class usable.
	 */
	public void setLearnedModel(Classifier classifier, Example[] currentExamles)
	{
		this.classifier = classifier ;
		this.currentExamles = currentExamles ;
		this.dtree = (DecisionTree)this.classifier ;
	}

	/**
	 * Use this function in the TrainableMacro's learnAll() function,
	 * after learn a single decision tree by lt.learn(horde); use this class
	 * like this --
	 * 	protected void learnAll(Horde horde)
	 * 	{
	 * 		sim.app.horde.classifiers.decisiontree.pruning.HordeSnapshot hs =
	 * 			new sim.app.horde.classifiers.decisiontree.pruning.HordeSnapshot();
	 * 		for ,,,,
	 * 		{
	 * 			Transition t = transitions[i];
	 * 			,,,,
	 * 			lt.learn(horde);
	 * 			// after learning let's save the snapshots
	 * 			System.err.println("--- TrainableMacro.learnAll():"
	 * 				+ " saving snapshots for ["
	 * 				+ i + "]: " + behaviors[i].toString());
	 * 			hs.setUniqueName(behaviors[i].toString() + "_[" + i + "]");
	 * 			hs.setLearnedModel(lt);
	 * 			hs.dumpHordeSnapshots();
	 * 			try {
	 * 				hs.saveHordeSnapshots();
	 * 			} catch(Exception e) {
	 * 				e.printStackTrace();
	 * 			}
	 * 		}
	 * 	}
	 */
	public void setLearnedModel(LearnedTransition lt, int macroIndex)
	{
		this.classifier = lt.getClassifier();
		this.currentExamles = new Example[lt.examples.size()];
		lt.examples.toArray(this.currentExamles);
		this.dtree = (DecisionTree)this.classifier ;
		this.currentBehaviorName = macroIndex + "-" + this.currentBehaviorName ;
	}

	public void setLearnedModel(DecisionTree dtree, Example[] currentExamles)
	{
		this.classifier = dtree ;
		this.currentExamles = currentExamles ;
		this.dtree = dtree ;
	}

	public void addTrajectoryExample(Example e, int startBehaviorIndex,
	                                 String startBehaviorName, int endBehaviorIndex, 
					 String endBehaviorName)
	{
		TrajectoryExample de = new TrajectoryExample(e.values, e.classification);
		de.continuation = e.continuation;
		de.setStartBehaviorName(startBehaviorName);
		de.setStartBehaviorIndex(startBehaviorIndex);
		de.setEndBehaviorName(endBehaviorName);
		de.setEndBehaviorIndex(de.classification);
		this.allExamples.add(de);
	}

	public void flushTrajectories()
	{
		this.allExamples = null ;
	}

	public void setCurrentBehaviorName(String name)
	{
		this.currentBehaviorName = name ;
	}

	public String getCurrentBehaviorName()
	{
		return this.currentBehaviorName ;
	}

	/**
	 * To dump the "Domain" information on the terminal,
	 * just for the peace of mind and eye.
	 */
	public String toString(Domain dom)
	{
		String result = "CLASSES\n{\n" ;
		for (int i = 0 ; i < dom.classes.length ; i++)
			result += "\t" + dom.classes[i] + "\n" ;
		result += "}\n";

		String categoStr = "", contStr = "", toroiStr = "" ;
		for(int t = 0 ; t < dom.type.length ; t++)
		{
			switch(dom.type[t])
			{
			case Domain.TYPE_CATEGORICAL:
				categoStr += "\t" + dom.attributes[t] + "\n" ;
				break ;
			case Domain.TYPE_CONTINUOUS:
				contStr += "\t" + dom.attributes[t] + "\n" ;
				break ;
			case Domain.TYPE_TOROIDAL:
				toroiStr += "\t" + dom.attributes[t] + "\n" ;
				break ;
			default:
				throw new RuntimeException("Unknown attribute type: " + dom.type[t]);
			}
		}

		String categoValStr = "" ;
		if(dom.values != null)
		{
			categoValStr = "\t Values: \n" ;
			for(int i = 0 ; i < dom.values.length ; i++)
			{
				if(dom.values[i] != null)
					for(int j = 0 ; j < dom.values[i].length ; j++)
						categoValStr += "\t vals[" + i + "]" + "[" + j + "]: "
						                + dom.values[i][j] + "\n" ;
				else
					categoValStr += "\t vals["+ i + "]:" + dom.values[i] + "\n" ;
			}
		}

		result = "NAME: " + dom.name + "\nCATEGORICAL\n{\n" + categoStr + categoValStr + "}\n"
		         + "CONTINUOUS\n{\n" + contStr + "}\n"
		         + "TOROIDAL\n{\n" + toroiStr + "}\n" ;

		return result ;
	}

	/**
	 * toString() function for easy parsing
	 **/
	private String toParseFriendlyString(Domain dom)
	{
		String result = dom.name + "\n" ;
		result += dom.classes.length + "\n" ;
		for(int i = 0 ; i < dom.classes.length ; i++)
			result += dom.classes[i] + "\n" ;
		result += dom.type.length + "\n" ;
		for(int t = 0 ; t < dom.type.length ; t++)
			result += dom.type[t] + "\n" ;
		result += dom.attributes.length + "\n" ;
		for(int i = 0 ; i < dom.attributes.length ; i++)
		{
			if(dom.type[i] == 0)
				result += dom.attributes[i] + "\n";
			else if(dom.type[i] == 1)
				result += dom.attributes[i] + "\n" ;
			else if(dom.type[i] == 2)
				result += dom.attributes[i] + "\n" ;
		}

		if(dom.values != null)
		{
			result += dom.values.length + ":\n" ;
			for(int i = 0 ; i < dom.values.length ; i++)
			{
				if(dom.values[i] != null)
				{
					result += dom.values[i].length + ":\n" ;
					for(int j = 0 ; j < dom.values[i].length ; j++)
						result += i + ":" + j + ":" + dom.values[i][j] + "\n";
				}
				else
					result += i + ":" + dom.values[i] + "\n" ;
			}
		}
		return result ;
	}

	/**
	 * After a typical HiTAB session, this function dumps
	 * all the simulation snapshots data on the terminal.
	 */
	public void dumpHordeSnapshots()
	{
		if(this.classifier != null && this.currentExamles != null)
		{
			System.out.println("\nDomain for " + this.currentBehaviorName + ":\n"
			                   + this.toString(this.classifier.getDomain()));
			System.out.println("Recorded Examples for " + this.currentBehaviorName + ":");
			for(int i = 0 ; i < this.currentExamles.length ; i++)
				System.out.print(this.currentExamles[i].toString());
			System.out.println("\nBuilt Decision Tree for " + this.currentBehaviorName + ":");
			System.out.println(this.getTreeAsDot(this.dtree.getRoot(),
			                                     this.currentBehaviorName, false));
		}
		else
			System.out.println("Learned models are null! No snapshots to save!!");
	}

	/**
	 * Dump all collected examples on the screen
	 */
	public void dumpAllTrajectoryExamples()
	{
		System.err.println("All collected examples:");
		for(TrajectoryExample exmp : this.allExamples)
			System.err.println(exmp.toString());
	}

	/**
	 * Save them in a file
	 */
	public void saveAllTrajectoryExamples()
	{
		try
		{
			FileWriter file = new FileWriter(this.location
			                                 + "allexamples.xmp");
			PrintWriter out = new PrintWriter(file);
			for(TrajectoryExample exmp : this.allExamples)
				out.print(exmp.toString() + "\n");
			out.close();

		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	/**
	 * Saves the domain object from a typical HiTAB session
	 */
	private void saveDomainSnapshot()
	{
		try
		{
			FileWriter file = new FileWriter(this.location
			                                 + this.currentBehaviorName + ".dom");
			PrintWriter out = new PrintWriter(file);
			out.print(this.toParseFriendlyString(this.classifier.getDomain()));
			out.close();

		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	/**
	 * Saves all the examples from a typical HiTAB session
	 */
	private void saveExamplesSnapshot()
	{
		try
		{
			String exmp = currentExamles.length + "\n" ;
			for(int i = 0 ; i < this.currentExamles.length ; i++)
				exmp += this.currentExamles[i].toString();
			FileWriter file = new FileWriter(this.location
			                                 + this.currentBehaviorName + ".xmp");
			PrintWriter out = new PrintWriter(file);
			out = new PrintWriter(file);
			out.print(exmp);
			out.close();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	/**
	 * Saves the decision tree from a previous HiTAB session,
	 * this tree was needed to implement the pruning code.
	 * -- the name of the tree will be the existing snapshot name
	 */
	public void saveTreeSnapshot(boolean withStats)
	{
		try
		{
			FileWriter file = new FileWriter(this.location
			                                 + this.currentBehaviorName + ".dot");
			PrintWriter out = new PrintWriter(file);
			out.print(this.getTreeAsDot(dtree.getRoot(),
			                            this.currentBehaviorName, withStats));
			out.close();

			Runtime rt = Runtime.getRuntime();
			Process proc = null ;
			try
			{
				proc = rt.exec("dot -Tpdf "
				               + this.location + this.currentBehaviorName + ".dot -o "
				               + this.location + this.currentBehaviorName + ".pdf");
				InputStream output = proc.getInputStream();
				System.out.println("Generated tree pdf file '"
				                   + this.currentBehaviorName + ".pdf' by: " + output);
				proc.waitFor();
			}
			catch(InterruptedException inte)
			{
				inte.printStackTrace();
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	/**
	 * Saves the decision tree from a previous HiTAB session,
	 * this tree was needed to implement the pruning code.
	 *  -- with a new snapshot name.
	 */
	public void saveTreeSnapshot(String snapName, boolean withStats)
	{
		try
		{
			FileWriter file = new FileWriter(this.location + snapName + ".dot");
			PrintWriter out = new PrintWriter(file);
			out.print(this.getTreeAsDot(dtree.getRoot(), snapName, withStats));
			out.close();

			Runtime rt = Runtime.getRuntime();
			Process proc = null ;
			try
			{
				proc = rt.exec("dot -Tpdf "
				               + this.location + snapName + ".dot -o "
				               + this.location + snapName + ".pdf");
				InputStream output = proc.getInputStream();
				System.out.println("Generated tree pdf file '"
				                   + snapName + ".pdf' by: " + output);
				proc.waitFor();
			}
			catch(InterruptedException inte)
			{
				inte.printStackTrace();
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	/**
	 * Gets a decision tree in dot file format string.
	 */
	private String getTreeAsDot(Node node, String label, boolean withStats)
	{
		Node.resetUniqueInteger();
		return "digraph G {\nTreeLabel [shape=plaintext, label=\""
		       + label + "\"]\n" + this.nodeToDot(node, withStats) + "}";
	}

	/**
	 * Returns the dot format string representation of each node
	 */
	private String nodeToDot(Node node, boolean withStats)
	{
		node.updateUniqueInteger();
		String s = "" ;
		String stats = "" ;

		if(withStats) // if we want to include statistics for pruning
		{
			if(node.statistics != null)  // if there is a NodeStatitics object
				stats = node.statistics.toString(node);
		}

		if(node instanceof CategoricalNode)
		{
			s = "\t" + node.getUniqueInteger()
			    + " [ label=\"" + node.toString()
			    + stats
			    + "\", shape=box ];\n";
			for(int i = 0 ; i < node.successors.length ; i++)
			{
				s += this.nodeToDot(node.successors[i], withStats);
				s += "\t" + node.getUniqueInteger()
				     + "->" + node.successors[i].getUniqueInteger()
				     + " [ label=\""
				     + this.classifier.getDomain().values[node.attribute][i] + "\"];\n";
			}
			return s ;
		}
		else if(node instanceof ContinuousNode)
		{
			s = "\t" + node.getUniqueInteger()
			    + " [ label=\"" + node.toString()
			    + stats
			    + "\", shape=box ];\n";
			s += this.nodeToDot(node.successors[0], withStats);
			s += this.nodeToDot(node.successors[1], withStats);
			s += "\t" + node.getUniqueInteger() + "->"
			     + node.successors[0].getUniqueInteger() + " [ label=\"In\" ];\n";
			s += "\t" + node.getUniqueInteger() + "->"
			     + node.successors[1].getUniqueInteger() + " [ label=\"Out\" ];\n";
			return s ;
		}
		else
		{
			return "\t" + node.getUniqueInteger()
			       + " [ label=\"" + node.toString()
			       + stats
			       + "\", shape=ellipse ];\n";
		}
	}

	/**
	 * This function saves all simulation artifacts from
	 * a typical HiTAB session in the '/horde/logs' folder.
	 */
	public void saveHordeSnapshots() throws Exception
	{
		System.out.println("\nSaving snapshots for " + this.currentBehaviorName);
		boolean exist = (new File(this.location)).exists();
		if (exist)
		{
			if(this.classifier != null && this.currentExamles != null)
			{
				saveDomainSnapshot();
				saveExamplesSnapshot();
				saveTreeSnapshot(false);
			}
			else
				System.out.println("Learned models are null! No snapshots to save!!");
		}
		else
		{
			// File or directory does not exist, make it
			boolean success = (new File(this.location)).mkdirs();
			if (!success)
			{
				System.err.println("Couldn't create '"
				                   + this.location + "' directory!");
				return ;
			}
			else
			{
				if(this.classifier != null && this.currentExamles != null)
				{
					saveDomainSnapshot();
					saveExamplesSnapshot();
					saveTreeSnapshot(false);
				}
				else
					System.out.println("Learned models are null! "
					                   + "No snapshots to save!!");
			}
		}
	}

	/**
	 * Loads the Domain from a saved HiTAB session
	 */
	public Domain loadHordeSnapshotDomain(String path, String currentBehaviorName)
	{
		String domName = "", classNames[] = null, attNames[] = null, vals[][] = null ;
		int classLength = 0, typeLength = 0, domTypes[] = null, attLength = 0 ;
		String delims = "[:]+" ;
		this.currentBehaviorName = currentBehaviorName ;
		String fileName = path + currentBehaviorName + ".dom" ;
		try
		{
			FileInputStream fstream = new FileInputStream(fileName);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			domName = br.readLine() ;

			classLength = Integer.parseInt(br.readLine());
			classNames = new String[classLength];
			for(int i = 0 ; i < classLength ; i++)
				classNames[i] = br.readLine();

			typeLength = Integer.parseInt(br.readLine());
			domTypes = new int[typeLength];
			for(int i = 0 ; i < typeLength ; i++)
				domTypes[i] = Integer.parseInt(br.readLine());

			attLength = Integer.parseInt(br.readLine());
			attNames = new String[attLength];
			for(int i = 0 ; i < attLength ; i++)
				attNames[i] = br.readLine();

			String valStr = br.readLine() ;
			if(valStr != null)
			{
				String valexpr[] = valStr.split(delims);
				int valLength = Integer.parseInt(valexpr[0]);
				vals = new String [valLength][] ;
				int i = -1 ;
				while(true)
				{
					String line = br.readLine() ;
					if(line == null)
						break ;
					else
					{
						String[] expr = line.split(delims);
						if( expr.length == 1 )
						{
							i++ ;
							int len = Integer.parseInt(expr[0]);
							vals[i] = new String[len] ;
						}
						else if( expr.length == 2)
						{
							i++ ;
							continue ;
						}
						else if(expr.length == 3)
							vals[Integer.parseInt(expr[0])]
							[Integer.parseInt(expr[1])]
							= expr[2] ;
						else
							throw new UnsupportedOperationException(
							    "\n\t\tYou are trying to put K "
							    + "numbers of (M x N) arrays in"
							    + "\n\t\tsome of the entries of "
							    + "another (P x Q) array !!"
							    + "\n\t\tAre you really sure of what"
							    + " exactly you are trying to do?");
					}
				}
			}
			in.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return new Domain(domName, attNames, vals, classNames, domTypes) ;
	}

	/**
	 * Loads the examples used in a previous HiTAB simulation session
	 */
	public Example[] loadHordeSnapshotExamples(String path, String currentBehaviorName)
	{
		String fileName = path + currentBehaviorName + ".xmp" ;
		Example[] examples = null ;
		try
		{
			FileInputStream fstream = new FileInputStream(fileName);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			int size = Integer.parseInt(br.readLine());
			examples = new Example[size] ;
			for(int i = 0 ; i < size ; i++)
			{
				String delims = "[ ]+" ;
				String[] tokens = br.readLine().split(delims);
				double val[] = new double[tokens.length - 2] ;
				for(int t = 1 ; t < tokens.length - 1 ; t++ )
					val[t-1] = Double.parseDouble(tokens[t]);
				examples[i] = new Example(val,
				                          Integer.parseInt(tokens[tokens.length - 1]));
				examples[i].continuation = (tokens[0].equals("+") ? true : false);
			}
			in.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return examples ;
	}
}

class TrajectoryExample extends Example
{
	private String startBehaviorName = "" ;
	private int startBehaviorIndex = -1 ;
	private String endBehaviorName = "" ;
	private int endBehaviorIndex  = -1 ;

	public TrajectoryExample(double[] val)
	{
		super(val);
	}
	public TrajectoryExample(double[] val, int classification)
	{
		super(val, classification);
	}

	// Getters
	public String getStartBehaviorName()
	{
		return this.startBehaviorName;
	}
	public int getStartBehaviorIndex()
	{
		return this.startBehaviorIndex;
	}
	public String getEndBehaviorName()
	{
		return this.endBehaviorName;
	}
	public int getEndBehaviorIndex()
	{
		return this.endBehaviorIndex;
	}

	// Setters
	public void setStartBehaviorName(String name)
	{
		this.startBehaviorName = name ;
	}
	public void setStartBehaviorIndex(int index)
	{
		this.startBehaviorIndex = index ;
	}
	public void setEndBehaviorName(String name)
	{
		this.endBehaviorName = name ;
	}
	public void setEndBehaviorIndex(int index)
	{
		this.endBehaviorIndex = index ;
	}

	public String toString()
	{
		String str = String.format("%-20s\t",
		                           this.startBehaviorIndex + "-" + this.startBehaviorName);
		for(int i = 0 ; i < values.length ; i++)
			str += String.format("%.5f",values[i]) + "\t";
		str += String.format("%20s\t",
		                     this.endBehaviorIndex + "-" + this.endBehaviorName)
		       + (continuation ? "true" : "false");
		return str ;
	}
}
