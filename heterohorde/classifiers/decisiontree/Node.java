package sim.app.horde.classifiers.decisiontree;
import sim.app.horde.classifiers.*;
import sim.app.horde.classifiers.decisiontree.pruning.*; /** added by khaled **/

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import ec.util.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author vittorio
 */
 
 
 
/** A Node is an abstract node in a Decision Tree.  Node.java contains most of the machinery to build
    and interpret Decision Trees */
        
public abstract class Node implements java.io.Serializable, Cloneable
    {
    private static final long serialVersionUID = 1;
        
    /** Returns the pathname of a file in the same directory as this class, regardless of operating system. */
    public static String getPathInDirectory(String s)
        {
        return Node.class.getResource("").getPath()+"/" +s;
        }

    /** The domain of the problem for which this Node was built. */
    protected Domain dom;
                
    /** The particular attribute on which this Node is splitting.  If this Node is a Leaf, this variable holds the classification of the Leaf. */ 
    public int attribute;
                
    public Node parent; 
    public abstract Node getParent(Example e); 
    public abstract Example[] getExamples(Example e); 
    
    public abstract void updateNode(Node n, Example e); 
    public abstract Node updateSplit(Example e); 
        
    public int getDepth()
        {
        int max=-1; 
        for (int i=0; i < successors.length; i++) 
            {
            int tmp = successors[i].getDepth();
            if (tmp > max) 
                max = tmp; 
            }
        return max+1; 
        }
    
    Example [] myExamples; 
    
    public void setMyExamples(Example[] givenExamples)
        {
        myExamples = new Example[givenExamples.length]; 
        System.arraycopy(givenExamples, 0, myExamples, 0, givenExamples.length); 
        }
    
    /** The children to the node, in order of values split (in the Domain).  If this node is a Leaf, this variable is unused. */
    public Node[] successors;  // null by default

    /** A NodeStatistics object to hold all relevant statistics information for pruning -- khaled **/
    public NodeStatistics statistics = null ;   
    
    
    public Example[] gatherExamples() 
        {
        ArrayList<Example> tmpExamples = new ArrayList<Example>() ; 
        
        if (successors != null) 
            {
            for (int i=0; i < successors.length; i++) 
                {
                Example [] e = null; 
                if (successors[i] instanceof Leaf) 
                    e = ((Leaf)successors[i]).getExamples(); 
                else 
                    e = successors[i].gatherExamples(); 
                for (int j=0; j < e.length; j++) 
                    tmpExamples.add((Example)e[j].clone()); 
                }
                        
            }
        Example[] e = new Example[tmpExamples.size()]; 
        for (int i=0; i < e.length; i++) 
            e[i] = (Example)tmpExamples.get(i).clone(); 
        
        return e; 
        }
    
    public Object clone()
        {
        try 
            {
            Node f = (Node)(super.clone());
                        
            // clone domain
            f.dom = dom == null ? null : (Domain)(dom.clone());
                                
            // copy successors
            if (successors != null)
                {
                f.successors = new Node[successors.length];
                for(int i = 0 ; i < f.successors.length; i++)
                    f.successors[i] = successors[i] == null ? null : (Node)(successors[i].clone());
                }


            return f;
            }
        catch (CloneNotSupportedException e) { return null; /* never happens */ }
        }

    /** Creates a node.  */
    public Node(Domain dom, int attr)
        {
        this.dom = dom;
        this.attribute = attr;
        }
    
    public Node copy() {
        System.out.println("You shouldn't be doing that!!!");
        return this; //hack alert: if done right, this should never be called 
        }
   

    /** Writes the attribute name of the node.  Leaf nodes must override this of course, since it's going to be wrong. */
    public abstract String toString();
                
    /** For each class in the Domain, builds a count of how often that class appears in the examples.  
        Returns the counts as a double[] rather than an int[] so they can be normalized later.
    */
    public static double[] buildClassCounts(Domain dom, Example[] examples)
        {
        return buildClassCounts(dom, examples, 0, examples.length, true);
        }
                
    /** For each class in the Domain, builds a count of how often that class appears in the examples, only considering the examples
        from examples[min] to examples[max], not including examples[max].  It must be the case that max >= min.  
        To instead compute all examples outside of this range, set computeInner to false.
        Returns the counts as a double[] rather than an int[] so they can be normalized later.
    */
    public static double[] buildClassCounts(Domain dom, Example[] examples, int min, int max, boolean computeInner)
        {
        double[] counts = new double[dom.classes.length];

        if (computeInner)
            {
            for(int i=min; i < max; i++)
                counts[examples[i].classification]++;
            }
        else // if (min > max) // May happen in toroidal situations
            {
            /*for(int i=min; i < examples.length; i++)
              counts[examples[i].classification]++;
              for(int i=0; i < max; i++)
              counts[examples[i].classification]++;
            */
            for(int i=max; i < examples.length; i++)
                counts[examples[i].classification]++;
            for(int i=0; i < min; i++)
                counts[examples[i].classification]++;
            }
        return counts;
        }
        
    /** Normalizes a double[] into a distribution.  If all the values are 0, they're normalized to a uniform distribution. */
    public static double[] normalize(double[] distribution)
        {
        // normalize distribution
        int sum = 0;
        for (int i = 0; i < distribution.length; i++)
            sum += distribution[i];
        if (sum == 0)       // there were no counts at all, make it uniform
            for (int i = 0; i < distribution.length; i++)
                { 
                distribution[i] = 1.0 / (double)distribution.length;
                }
        else
            for (int i = 0; i < distribution.length; i++)
                {
                distribution[i] = (double)distribution[i] / (double) sum;
                }
        return distribution;
        }

    /** Returns the class, from the Domain, which appears the most often among the examples, breaking ties randomly. */
    public static int majorityValue(Domain dom, Example[] examples, MersenneTwisterFast random)
        {
        // build a count array
        double[] counts = buildClassCounts(dom, examples);              // will always be ints but I'm lazy

        // return the highest from among that array, breaking ties randomly
        int ticks = 1;
        double max_count = -1;
        int max_class = -1;
        for (int i = 0; i < dom.classes.length; i++)
            {
            if (counts[i] > max_count)
                {
                ticks = 1;
                max_count = counts[i];
                max_class = i;
                }
            else if (counts[i] == max_count && random.nextBoolean(1.0 / ++ticks))
                {
                max_count = counts[i];
                max_class = i;
                }
            }
        return max_class;
        }

    /** Returns true if all the examples have identical value arrays.  If the examples array is empty, throws an exception. */
    public static boolean allSameValues(Example[] examples)
        {
        if (examples.length == 0)
            throw new RuntimeException("Empty examples set!");
        double last_value;
        for(int val=0; val<examples[0].values.length;val++)
            {
            last_value = examples[0].values[val];
            for (int idx = 1; idx < examples.length; idx++)
                if( last_value != examples[idx].values[val])
                    return false;
            }
        return true;
        }

    /** Returns true if the examples have identical classes.  If the examples array is empty, throws an exception. */
    public static boolean allSameClassification(Example[] examples)
        {
        if (examples.length == 0)
            throw new RuntimeException("Empty examples set!");
        int classification = examples[0].classification;
        for (int idx = 1; idx < examples.length; idx++)
            {
            if (examples[idx].classification != classification)
                return false;
            }
        return true;
        }


    /** Builds a tree based on the given examples.  The tree will not exceed the maximum depth. */
    public static Node learn(Domain dom, Example[] examples, int maxDepth, MersenneTwisterFast random)
        {
        // initialize with all attributes
        int[] attributes = new int[dom.attributes.length];
        for(int i = 0; i < attributes.length; i++) attributes[i] = i;
                
        // we throw an exeption here so we can pass in null at the top level for defaultLeaf
        if (examples.length == 0) throw new RuntimeException("Top-level tree learned with empty examples!");
        return learn(dom, examples, attributes, null, maxDepth, 0, random);
        }

    /** Returns the value considered to be zero information.  */
    public static double MINIMUM_VALUE = -1; //FIXME
    
    // Indicates that a split was horrible
    public static int NO_ATTRIBUTE = -1; 

    /** Builds a subtree based on the given examples and the current default leaf.  This is the recursive call; 
        the syntactic sugar top-level call is Learn(dom, examples, attributes).  */
    protected static Node learn(Domain dom, Example[] examples, int[] attributes, Leaf defaultLeaf, int maxDepth, int depth, MersenneTwisterFast random)
        {
        if (maxDepth == depth) return new Leaf(dom, examples, examples);
        else if (examples.length == 0) return defaultLeaf;
        else if (allSameClassification(examples) || allSameValues(examples)) { return new Leaf(dom, examples, examples); }
        else if (attributes.length == 0) return new Leaf(dom, examples, examples);
        else
            {
            int bestAttribute = chooseAttribute(dom, attributes, examples, random);
            if (bestAttribute == NO_ATTRIBUTE) // all splits had bad information, so insert a leaf instead of splitting 
                return new Leaf(dom, examples, examples);
            switch (dom.type[bestAttribute])
                {
                case Domain.TYPE_CATEGORICAL: return CategoricalNode.provideNode(dom, bestAttribute, examples, attributes, maxDepth, depth, new Leaf(dom, examples, examples), random); 
                case Domain.TYPE_CONTINUOUS:  return ContinuousNode.provideNode(dom, bestAttribute, examples, attributes, maxDepth, depth, new Leaf(dom, examples, examples), random);
                case Domain.TYPE_TOROIDAL:  return ToroidalNode.provideNode(dom, bestAttribute, examples, attributes, maxDepth, depth, new Leaf(dom, examples, examples), random);
                default: throw new RuntimeException("No such node type " + dom.type[bestAttribute]);  // better not happen!
                }
            }
        }
        
        
    /** Returns a new array where the given attribute has been removed.  Grotesquely expensive and stupid hack.  */
    // this is obviously not the right way to do this -- O(n)
    public int[] except(int[] attributes, int attribute)
        {
        int[] ex = new int[attributes.length - 1];
        int removed = -1;
        int count = 0;
        for(int i = 0; i < attributes.length; i++)
            if (attributes[i] != attribute)
                ex[count++] = attributes[i];
            else
                {
                if (removed != -1)  // uh oh
                    throw new RuntimeException("BAD BAD BAD " + attributes[i] + " vs " + attributes[removed]);
                else
                    removed = i;
                }
        return ex;
        }
       
    /** Returns null if there is no useful split */
    public static Example[][] doSplit(Domain dom, final int attribute, Example[] examples, MersenneTwisterFast random)
        {
        switch (dom.type[attribute])
            {
            case Domain.TYPE_CATEGORICAL:
                return CategoricalNode.split(dom, attribute, examples, random);
            case Domain.TYPE_CONTINUOUS:
                return ContinuousNode.split(dom, attribute, examples, random);
            case Domain.TYPE_TOROIDAL:
                return ToroidalNode.split(dom, attribute, examples, random);
            default:
                throw new RuntimeException("Impossible attribute type to split on : " + dom.type[attribute]);
            }
        }

    static final double invertlog2 = 1.0 / Math.log(2.0);  // bein' a little clever, not sure if Java converts constant divides into multiplies automatically
        
    /** Log base 2 */
    public static double lg(double val) { return Math.log(val) * invertlog2; }  // Ask Sean: log base 2 required?  -- Not sure, probabably not -- Sean
        
        
    /** Computes the information among the classes in the given examples. */
    public static double information(Domain dom, Example[] examples) { return information (dom, examples, 0, examples.length, true); }

    /** Computes the information among the classes in the given examples, only considering the values between min and max. 
        max must be <= min.  To compute the value outside of min and max, set computeInner to false.
        Returns the counts. */
    public static double information(Domain dom, Example[] examples, int min, int max, boolean computeInner)
        {
        if(examples.length==0) return 0;
        double result = 0;
        double[] counts = buildClassCounts(dom, examples, min, max, computeInner);
                
        for (int i = 0; i < counts.length; i++)
            {
            double el = (counts[i] / (double) examples.length);
            if (el != 0) result += el * lg(el); 
            }
        return 0 - result;
        }

    /** Computes the GAIN in information due to splitting the examples in a given way. */
    public static double gain(Domain dom, Example[] examples, Example[][] split)
        {
        double result = 0;

        for (int i = 0; i < split.length; i++)
            {
            //if(split[i].length==0) return Double.NEGATIVE_INFINITY;  // COMMENTED OUT BY SEAN -- THIS APPEARS TO BE WRONG
            result += ((double) split[i].length / (double) examples.length) * information(dom, split[i]);
            }  
            
        //System.out.println("Gain= " + (information(dom, examples) - result));
        return information(dom, examples) - result;
        }

    /** Computes the GAIN in information due to splitting the examples in a given way.
        We assume that there are only two arrays in split[], 
        the same size as examples[], and restrict the split to be such that 
        split[0] is from min to max and split[1] is from max to min. 
        max must be <= min.  */
    public static double gain(Domain dom, Example[] examples, Example[][] split, int min, int max)
        {
        // two chunks
        // System.out.println("Max: " + max + " Min: " + min + " Length: " + split[1].length);
        double result = ((max - min) / (double) examples.length) * information(dom, split[0], min, max, true);
        //FIXME : changed the order of min and max in call to information() 
        result += ((examples.length - (max - min)) / (double) examples.length) * information(dom, split[1], min, max, false); 
        // System.out.println(result);
        return information(dom, examples) - result;
        }


    /** Computes the POTENTIAL P(S,B) in information due to splitting the examples in a given way.  
        Used to temper the "gain" metric for situations with lots of classes.  
        See bottom of page 4, http://ai.stanford.edu/~ronnyk/treesHB.pdf */
    public static double potential(Example[] examples, Example[][] split)
        {
        double result = 0;
        for (int i = 0; i < split.length; i++)
            {
            double ratio = split[i].length / (double) examples.length;
            if(ratio!=0) result += ratio * lg(ratio);
            }
        //System.out.println("Non t potential="+(-result));
        return 0 - result;
        }

    /** Computes the POTENTIAL P(S,B) in information due to splitting the examples in a given way.  
        Used to temper the "gain" metric for situations with lots of classes.  
        See bottom of page 4, http://ai.stanford.edu/~ronnyk/treesHB.pdf 
        We assume that there are only two arrays in split[], the same size as examples[], and restrict the split to be such that 
        split[0] is from min to max and split[1] is from max to min.   max must be <= min.  
    */
    public static double potential(Example[] examples, Example[][] split, int min, int max)
        {
        double result = 0;
        double ratio = 0;
                
        ratio = (max - min) / (double) examples.length;
        if(ratio!=0) result += ratio * lg(ratio);
                
        ratio = ((split[1].length) - max + (min - 0)) / (double) examples.length;
        if(ratio!=0) result += ratio * lg(ratio);

        //System.out.println("t potential="+(-result));
        return 0 - result;
        }

    /** Computes the VALUE (my name) of a given split, defined as the gain divided by the potential.  We want a
        split which maximizes this value. */
    public static double value(Domain dom, Example[] examples, Example[][] split)
        {
        double gain = gain(dom, examples, split);
        if (gain == 0.0) return gain;  // sucks
        else return gain / potential(examples, split);
        }
        
    /** Computes the VALUE (my name) of a given split, defined as the gain divided by the potential.  We want a
        split which maximizes this value.
        We assume that there are only two arrays in split[], the same size as examples[], 
        and restrict the split to be such that split[0] is from min to max and split[1] is from max to min.
        It's NOT assumed that min <= max.  */
    public static double value(Domain dom, Example[] examples, Example[][] split, int min, int max)
        {
        double gain = gain(dom, examples, split, min, max);
        if (gain == 0.0) return gain;  // sucks
        else return gain / potential(examples, split, min, max);
        }

    /** Chooses an attribute to split the examples, by picking the one with the highest VALUE.  */
    public static int chooseAttribute(Domain dom, int[] attributes, Example[] examples, MersenneTwisterFast random)
        {
        if (attributes.length == 0)
            {
            String error = "no attributes to choose!";
            throw new RuntimeException(error);
            }

        // Quinlan picks the best gain/ratio among elements which have a gain value over average.

        // Compute average gain
        double averageGain=0;
        double[] gains = new double[attributes.length];
        Example[][][]splits = new Example[attributes.length][][];
             
        int numOfSplits = attributes.length;
        ArrayList<Integer> nonSplitAttribute = new ArrayList<Integer>(); 
        for(int i=0;i<attributes.length;i++)
            {
            splits[i] = doSplit(dom, attributes[i], examples, random);
            if(splits[i] == null) {
                nonSplitAttribute.add(i);
                }
            else {
                averageGain += (gains[i] = gain(dom, examples, splits[i]));     
                }            
            }
                
        // numerical error can result in the average being higher than ALL the
        // examples if the examples are all the same.  So instead we do something like
        // the average being 9/10 of the average.
       
        // FIXME : added parenthesis in denometer 
        averageGain = (0.9) * averageGain / (attributes.length - nonSplitAttribute.size());
         
        
        // FIXME 
        if(averageGain == 0) {
            averageGain -= .000001;
            }
        // Best value above gain avg
        double max_value=Double.NEGATIVE_INFINITY;
        int max_idx=-1;
        int count=0;
        System.out.println("ATTR: " + attributes.length + " " + averageGain); 
        for(int i=0;i<attributes.length;i++)
            {
            if(nonSplitAttribute.contains(i))
                { System.err.println("Non Split Attribute " + i); continue; }
                
            if(gains[i] < averageGain) 
                { System.err.println ("gain " + gains[i] + " < " + averageGain + " for " + i); continue; } // below average
                        
            
            /*
              Example[][] sp = splits[i]; 
              System.out.println("LEFT"); 
              for (int k=0; k < sp[0].length; k++)
              System.out.print(sp[0][k]); 
              System.out.println("RIGHT"); 
              for (int k=0; k < sp[1].length; k++)
              System.out.print(sp[1][k]); 
                
            */ 
            double value = value(dom, examples, splits[i]);
                        
            System.out.println("Value of " + i + ": " + value + "\t" + max_value + "\t" + count); 
            
            if((value>max_value && (count=1)==1) ||
                (value==max_value && random.nextBoolean(1.0/(++count))))
                {
                max_value=value;
                max_idx=i;
                }
  
            }

        //System.out.println("******"); 
        
        // All splits have horrible information, so don't perform a split
        if (max_value <= 0) 
            return NO_ATTRIBUTE; 
        
        return attributes[max_idx];
        }

    /** Returns the classification of the given example according to the subtree rooted at this node.  Does not
        examine nor modify e.classification. */
    public abstract int classify(Example e, MersenneTwisterFast random); 
                
        
    /** Returns the entire distribution of the leaf which classifies this example.*/
    public abstract double[] provideDistribution(Example e);
        

    /** Writes a Dot file, naming the tree with "label" */
    public void writeDotFile(String file, String label) throws IOException
        {
        PrintWriter fop = new PrintWriter(new FileWriter(new File(file)));
        fop.println(treeToDot(label));
        fop.close();
        }

    /** Returns the Dot expression for the given node. */
    public abstract String nodeToDot();
        
    /** Generates a tree with the given label. */
    public String treeToDot(String label)
        {
        // reset the unique integers
        privateCounter = 0;
        return "digraph G {\nTreeLabel [shape=plaintext, label=\"" + label + "\"]\n" + nodeToDot() + "\n}";
        }
    
    private static long privateCounter = 0; 
    private long uniqueInteger = 0;

    /** These procedures are made public by khaled, previously they used to be protected **/    
    /** Associates a unique integer with the Node. */
    public void updateUniqueInteger() { uniqueInteger = privateCounter++; }
        
    /** Returns the unique integer associated with the Node. */
    public long getUniqueInteger() { return uniqueInteger; }

    /** resets the unique integer -- added by khaled **/    
    public static void resetUniqueInteger() { privateCounter = 0; }        
        
        
    /***** PRUNING CODE.  More code found in Leaf.java ******/
        
    // constants from p. 665 of Bishop.  The C is the value of C(T) we need to be less than in order to retain the subtree. */
    // public final static double LAMBDA = 0.05;
    // public final static double C = 3.0;  // say

    // how many leaves are in my subtree?
    int totalLeaves;
    // What is the total error among the leaves in my subtree?
    double totalError;
        
        
    /** Computes the number of leaves and the total error.  NOT recursive -- this needs to be done bottom-up.  Overridden by Leaf.java.  */
    protected void preprocessForPruning()
        {
        for(int i = 0 ; i < successors.length; i++)
            {
            totalLeaves += successors[i].totalLeaves;
            totalError += successors[i].totalError;
            }
        }
        
    /** Returns how many classes were found among examples that were in my subtree.
        This is done by adding up the leaf counts of my children.  Leaf.java overrides
        this to take its distribution and multiply it by its count variable. */
    protected double[] getLeafCounts(double[] addInto)
        {
        for(int i = 0 ; i < successors.length; i++)
            successors[i].getLeafCounts(addInto);
        return addInto;
        }
        
    /** Possibly prunes the node, replacing it with a Leaf node.  Also recursively prunes the kids (first). */
    public Node prune(Domain dom, double c, double lambda)
        {
        // prune kids
        for(int i = 0 ; i < successors.length; i++)
            successors[i] = successors[i].prune(dom, c, lambda);

        // preprocess me
        preprocessForPruning();

        System.out.println("" + c + " <? " + totalError + " + " + lambda + " * " + totalLeaves);
                
        // determine if I should be pruned
        if (c < totalError + lambda * totalLeaves)
            {       // prune me to a leaf node
                        
            System.out.println("PRUNING: " + totalLeaves);
                        
            // first build the distribution for the leaf.  We pdf-ify the CDFs of our
            // kids, multiply them by their counts, add 'em up, and re-CDF-ify.
            double[] d = getLeafCounts(new double[dom.classes.length]);
            int leafcount = 0;
            for(int i = 0; i < d.length; i++) leafcount += d[i];
            //makeCDF(d);
            d = normalize(d);
                        
            // now make the leaf
            Leaf l = new Leaf(dom, d, leafcount);
                        
            // re-preprocess the new Leaf and return it
            l.preprocessForPruning();
            return l;
            }
        else return this;  // no pruning
        }


    public abstract void write(PrintWriter writer);
        
    /** If writedomain is false, we don't bother writing it. */
    public void writeTree(PrintWriter writer, boolean writeDomain)
        {
        writer.print(" ( tree ");
        if (writeDomain) dom.write(writer);
        writer.print("\n");
        write(writer);
        writer.print(" )\n\n");
        }
        
    // doesn't read the first '(' so it can be checked for by the parent
    public static Node read(Scanner scanner, Domain domain)
        {
        String name = token(scanner);
        if (name.equals("leaf"))
            return new Leaf(scanner, domain);
        else if (name.equals("continuous"))
            return new ContinuousNode(scanner, domain);
        else if (name.equals("categorical"))
            return new CategoricalNode(scanner, domain);
        else if (name.equals("toroidal"))
            return new ToroidalNode(scanner, domain);
        else generateTokenError(scanner, new String[] {"continuous", "categorical", "toroidal"});
        return null;  // never happens
        }
        
    /** If domain is null, we read one from the file. */
    public static Node readTree(Scanner scanner, Domain domain)
        {
        token(scanner, "(");
        token(scanner, "tree");
        if (domain == null) domain = new Domain(scanner);
        token(scanner, "(");
        Node val = read(scanner, domain);
        token(scanner, ")");
        return val;
        }

    public Node(Scanner scanner, Domain domain) { dom = domain; }
        
        
        
        
        
        
        
        
        
        
        
        
    protected  static String token(Scanner scanner) { return token(scanner, null); }
    protected  static String token(Scanner scanner, String expect)
        {
        if (scanner.hasNext())
            {
            String token = scanner.next();
            if (expect == null || token.equals(expect))
                {
                return token;
                }
            else generateTokenError(scanner, expect );
            }
        else throw new RuntimeException("Out of tokens for " + scanner);
        return null;  // never happens
        }
                
    protected  static void generateTokenError(Scanner scanner, String expect) { generateTokenError(scanner, new String[] { expect }); }
    protected  static void generateTokenError(Scanner scanner, String expect1, String expect2) { generateTokenError(scanner, new String[] { expect1,  expect2 }); }
    protected  static void generateTokenError(Scanner scanner, String[] expect)
        {
        String token = scanner.match().group();  // I think this is what we want
        String s = new String("Mismatched token, got " + token + " but expected any of\n" );
        for(int i = 0; i < expect.length; i++)
            s += "\t" + expect[i] + "\n";
        s += "for scanner: " + scanner + "\njust before: ";
        try
            {
            for(int i = 0; i < 3; i++)
                s = s + scanner.nextLine();
            }
        catch (Exception e) { }
        throw new RuntimeException(s);
        }
                
    protected int[] getInts(ArrayList a)
        {
        int[] ints = new int[a.size()];
        for(int i = 0; i < ints.length; i++)
            ints[i] = ((Integer)(a.get(i))).intValue();
        return ints;
        }
        
    protected String[] getStrings(ArrayList a)
        {
        String[] strings = new String[a.size()];
        for(int i = 0; i < strings.length; i++)
            strings[i] = (String)(a.get(i));
        return strings;
        }

    protected Node[] getNodes(ArrayList a)
        {
        Node[] nodes = new Node[a.size()];
        for(int i = 0; i < nodes.length; i++)
            nodes[i] = (Node)(a.get(i));
        return nodes;
        }

    protected String[][] getStringArrays(ArrayList a)
        {
        String[][] strings = new String[a.size()][];
        for(int i = 0; i < strings.length; i++)
            strings[i] = (String[])(a.get(i));
        return strings;
        }

    }
