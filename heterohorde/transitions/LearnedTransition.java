package sim.app.horde.transitions;

import java.io.*;
import java.util.ArrayList;
import sim.app.horde.Horde;
import sim.app.horde.agent.Agent;
import sim.app.horde.behaviors.Macro;
import sim.app.horde.classifiers.*;
import sim.app.horde.classifiers.decisiontree.pruning.* ; /** added by khaled **/
import sim.app.horde.features.Feature;
import sim.app.horde.behaviors.TrainableMacro;

/** 
    LEARNED TRANSITION

    A transition which makes its decisions based on a classifier (typically a decision tree).
    LearnedTransitions hold a classifier (possibly null), a set of features, a set of
    examples, and a classification domain.
        
    * @author vittorio
    */
 
public class LearnedTransition extends Transition
    {
    private static final long serialVersionUID = 1;
        
    Classifier classifier;
    
    /** Returns the classifier of the transition, or null if no classifier right now. */
    public Classifier getClassifier() { return classifier; }

    // Feature[] feature;
    /** Returns the features of the transition. */
    // public Feature[] getFeatures() { return feature; }
    
    transient ArrayList examples = new ArrayList();
    
    /** Returns the examples stored in the transition right now, from which it will build a classifier. */
    public ArrayList getExamples() 
        {
        if (examples == null) examples = new ArrayList();
        return examples; 
        }  
    
    /** Loads the provided examples into the internal examples ArrayList.  */
    public void setExamples(ArrayList examples) 
        {
        if (examples == null) this.examples = null;  // yes, I notice the difference with getExamples()
        else
            {
            if (this.examples == null) this.examples = new ArrayList();
            this.examples.clear(); 
            this.examples.addAll(examples);
            }
        }
    
    Domain domain;
    /** Returns the domain of the transition. */
    public Domain getDomain() { return domain; }
 
    public Object clone()
        {
        LearnedTransition f = (LearnedTransition)(super.clone());
                        
        // don't bother cloning the classifier
        f.classifier = classifier;
                                
        // don't bother cloning the features
        // f.feature = feature; 
                                
        // don't bother cloning the domain
        f.domain = domain;
                        
        // Clone the examples.  This will be costly but is cleaner
        if (examples != null)
            {
            f.examples = new ArrayList(examples);
            for(int i = 0; i < f.examples.size(); i++)
                f.examples.set(i, f.examples.get(i) == null ? null : ((Example)(f.examples.get(i))).clone());
            }
                
        return f;
        }



    public LearnedTransition(//Feature[] f, 
        Domain d)
        {
        //this.feature = f;
        this.classifier = null;
        domain=d;
        }

    /** Builds an example from the current environment and features. */
    public Example getExample(Agent agent, Macro parent, Horde horde)
        {
        Feature[] feature = ((TrainableMacro)parent).features;
        double[] val = new double[feature.length];        
        for (int x = 0; x < feature.length; x++)
            {
            val[x] =  feature[x].getValue(agent, parent, horde);
            }
        return new Example(val);
        }

    /** Creates an example and classifies it, then returns that classification as the desired
        change in behavior.  */
    public int change(Agent agent, Macro parent, Horde horde)
        {
        if (classifier==null) { return parent.currentBehavior; } // returns done if not learned yet

        // build example
        Example e = getExample(agent, parent, horde);
        try{
            int i = classifier.classify(e, horde.random);
            return i;
            }
        catch(RuntimeException ex)
            {
            if(ex.getMessage().equals("Cannot classify with null classifier!"))
                return parent.currentBehavior;
            else throw new RuntimeException(ex.getMessage());
            }
        }

    /** Builds the decision classifier from the current examples.  */
    public void learn(Horde horde)
        {
        if (getExamples()==null || getExamples().size()==0) throw new RuntimeException("No examples to learn from!");
        Example[] a = new Example[getExamples().size()];
        getExamples().toArray(a);
                
        // tweak to ordinary examples?
        if (!horde.getDefaultExamplesAreSpecial())
            {
            for(int i = 0; i < a.length; i++)
                a[i].continuation = false;
            }

        // build the classifier
        classifier = horde.makeNewClassifier(); 
               
        classifier.setDomain(getDomain()); 
        classifier.learn(a, horde.random);
        }

    /** Displays the classifier associated with this LearnedTransition by writing it out as a dot file, then launching
        a script to compile and display the dot file.  */
    public void showTransition(int index, String label) throws IOException, InterruptedException
        {
        classifier.show(index, label); 
        }
    
    public void start(Agent agent, Macro parent, Horde horde)
        {
        Feature[] feature = ((TrainableMacro)parent).features;
        for(int i = 0; i < feature.length; i++) 
            feature[i].start(agent, parent, horde); 
        }
                
    public void stop(Agent agent, Macro parent, Horde horde) 
        {
        Feature[] feature = ((TrainableMacro)parent).features;
        for(int i = 0; i < feature.length; i++)
            feature[i].stop(agent, parent, horde);
        }

    public void dumpExamples()
        {
        for (int i=0; i < getExamples().size(); i++) 
            System.err.println(getExamples().get(i));
        }

    /** Dumps all the examples for this transition, as a debugging measure.  */
    public void logExamples(String path, Horde horde, String associatedBehaviorName)
        {
        if (getExamples()==null || getExamples().size()==0) throw new RuntimeException("No examples to save!");  
        try{
            // Create file
            System.out.println("Logging:"+path+associatedBehaviorName+".examples");
            FileWriter fstream = new FileWriter(path+associatedBehaviorName+".examples");
            BufferedWriter out = new BufferedWriter(fstream);
            for (int i=0; i < getExamples().size(); i++) 
                {
                out.write(((Example)getExamples().get(i)).toString());
                out.write("\n");
                }
           
            //Close the output stream
            out.close();
            }
        catch (Exception e)
            {
            throw new RuntimeException("Error: " + e.getMessage());
            }
        }

    /** Dumps the domain for this transition, as a debugging measure.  */
    public static void logDomain(Domain domain, String path, String name)
        {
        try
            {
            // Create file
            System.out.println("Logging Domain:"+path+name+".dom");
            FileWriter fstream = new FileWriter(path+name+".dom",false);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(domain.toString());

            //Close the output stream
            out.close();
            } 
        catch (Exception e) //Catch exception if any
            {
            throw new RuntimeException("Error: " + e.getMessage());
            }
        }
                
                

    public void write(PrintWriter writer, boolean writeDomain)
        {
        writer.print(" ( learned-transition ");
        if (writeDomain) domain.write(writer);
        writer.print("\n");
        if (classifier == null) writer.print(" null\n");
        else classifier.writeClassifier(writer, false);  // Classifier's  don't write the domain
        //writer.print(" ( features " + feature.length + " ");
        //for(int i = 0; i < feature.length; i++)
        //    feature[i].write(writer);
        //writer.print(")");
        writer.println("\n)\n\n");
        }
                
    }
    
