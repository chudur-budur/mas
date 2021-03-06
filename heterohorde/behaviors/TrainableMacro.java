package sim.app.horde.behaviors;

import java.util.logging.*;

import sim.app.horde.transitions.*;
import sim.app.horde.*;
import sim.app.horde.agent.*;
import sim.app.horde.classifiers.*;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;

import java.util.*;
import java.io.*;
import java.util.zip.*;

/** 
    TRAINABLE MACRO
 
    <p>Trainable Macros are Macros which have LEARNABLE TRANSITIONS as their transition
    functions. These transitions are essentially wrappers for classifiers (for now decision
    trees). Trainable Macros gather examples as the user directs them to transition from
    behavior to behavior, then when learn() is called they build LearnableTransitions from
    these examples.
   
    <p>TrainableMacros can then be saved to disk via serialization, at which point they can
    be reloaded and used with their LearnableTransitions as the fixed transitions.
   
    <p>When TrainableMacros are saved, they're saved to a particular directory "learned"
    in the "behaviors" directory. They're saved out with ".trained" extensions.
   
    <p>TrainableMacros are in two modes: training or not training. When in training, they
    gather examples and only transition when the user requests it. When in not training
    (that is, after they've learned), they follow their learned transition functions.
*/

public class TrainableMacro extends Macro implements Serializable
    {
    private static final long serialVersionUID = 1;
    
    /** The extension of trainable macro files. */
    static final String TRAINABLE_MACRO_EXTENSION = ".trained";


    /** All the features of the Horde environment in which the TrainableMacro is learning examples */
    public Feature[] features;
    

    /** The previous behavior performed.  Used to back up during an "undo" */
    int previousBehavior = UNKNOWN_BEHAVIOR;


    //// TRAINING MODES

    /** Is the TrainableMacro in training mode (as opposed to running on its own)? */
    boolean training = true; // matches button: true if the macro is presently in training mode (gathering examples).
    /** Returns whether the TrainableMacro in training mode (as opposed to running on its own)? */
    public boolean isTraining() { return training; }
    /** Sets the training mode as requested.  Only call this if you know what you're doing.
        Otherwise, call userChangedTraining(...) */
    public void setTraining(boolean val) { training = val; } 
    /** Sets the training mode as requested, and learns the examples. */
    public void userChangedTraining(Horde horde, boolean training)
        {
        setTraining(training);
        userAskedForNewBehavior = false;

        if (!training)
            {
            learnAll(horde);
            currentBehavior = INITIAL_BEHAVIOR;
            previousBehavior = INITIAL_BEHAVIOR;
            }
        }


    // USER-REQUESTED OR CONTROLLER-AGENT-REQUESTED BEHAVIOR CHANGES

    /** Did the user ask to switch to a new behavior? */
    boolean userAskedForNewBehavior = false;
    /** What is the new behavior the user asked to switch to? */
    int newBehaviorRequestedByUser = UNKNOWN_BEHAVIOR;
    /** Set to targets the user asked to switch to, or null if no change in targets is requested. */
    Target[] newTargetsRequestedByUser = null;
    /** Set a new behavior requested by the user, including targets to bind to. 
        If targets is null, no new targets are requested. */
    public void setNewBehaviorRequestedByUser(int newBehavior, Target[] targets)
        {
        newTargetsRequestedByUser = targets;
        newBehaviorRequestedByUser = newBehavior;
        userAskedForNewBehavior = true;
        }
    /** Called when the user wishes to change the behavior of the TrainableMacro to a new behavior. 
        Next time go() is called, it'll transition to this behavior. */
    public void userChangedBehavior(Horde horde, int newBehavior)
        {
        setNewBehaviorRequestedByUser(newBehavior, null);

        if (currentBehavior != UNKNOWN_BEHAVIOR && behaviors[currentBehavior].shouldAddExamples())
            {
            addExample(horde, newBehavior);

            if (behaviors[newBehavior].getShouldAddDefaultExample() && currentBehavior != newBehavior)
                {
                // add an example that says "keep going like this new situation"
                int temp = currentBehavior;
                currentBehavior = newBehavior;
                addExample(horde, newBehavior);
                currentBehavior = temp;
                }
            }
        }



    // DEFAULT SAMPLES

    /** When later used as a behavior, should the training macro be treated as a continuous FSA
        (which would benefit from default samples), or as a one-shot FSA (which should not
        have default samples)?*/
    boolean shouldAddDefaultExample = true;
    /** When later used as a behavior, should the training macro be treated as a continuous FSA
        (which would benefit from default samples), or as a one-shot FSA (which should not
        have default samples)?*/
    public boolean getShouldAddDefaultExample() { return shouldAddDefaultExample; }
    /** When later used as a behavior, set whether the training macro should be treated as a continuous FSA
        (which would benefit from default samples), or as a one-shot FSA (which should not
        have default samples)?*/
    public void setShouldAddDefaultExample(boolean val) { shouldAddDefaultExample = val; }



    // SINGLE STATE

    /** Should the training mode be single-state, that is, a policy rather than an FSA? 
        @Override
    */
    public void setSingleState(boolean value) 
        {
        if (value != isSingleState())
            {
            if (value)
                {
                // going single state.  Reduce.
                ArrayList allExamples = new ArrayList();
                        
                for (int i = 0; i < behaviors.length; i++)
                    {
                    if (transitions[i] != null)
                        {
                        LearnedTransition lt = (LearnedTransition) transitions[i];
                        if (lt.getExamples() != null)
                            allExamples.addAll(lt.getExamples());
                        // null out transition
                        if (i > 0) transitions[i] = null;
                        }
                    }
                        
                // build first transition
                if (transitions[0] == null)
                    transitions[0] = new LearnedTransition(//features, 
                        getDomain());
                                        
                // load examples into first transition
                ((LearnedTransition)transitions[0]).setExamples(allExamples);
                }
            else
                {
                // going multi-state.  Hope we know what we're doing.  Null out all transitions, including examples.
                for (int i = 0; i < behaviors.length; i++)
                    {
                    transitions[i] = null;
                    }
                }
            }
        super.setSingleState(value);
        }


    /** Deep-clones the TrainableMacro, except for the Features, which are at present shared. */
    public Object clone()
        {
        TrainableMacro f = (TrainableMacro) (super.clone());

        // clone features, if any were used 
        if (features != null)
            {
            f.features = new Feature[features.length];
            for (int i = 0; i < f.features.length; i++)
                f.features[i] = (Feature)(features[i].clone());
            }

        return f;
        }

    /** Sets the name of the Macro, guaranteeing that it's valid name for saving out to the disk. Legal
        names must be just letters or digits. */
    public void setName(String n)
        {
        // only allow legal names
        for (int i = 0; i < n.length(); i++)
            if (!Character.isLetterOrDigit(n.charAt(i))) 
                throw new RuntimeException("Bad name"); // failed
        name = n; // succeeded
        }

    String[] behaviorNames = null;
    Target[][] behaviorTargets = null;
    
    /** Returns the targets actually used at the moment. */
    public boolean[] getTargetsUsed()
        {
        boolean[] targetsUsed = new boolean[targets.length];

        boolean[] usedBehaviors = new boolean[behaviors.length];
        usedBehaviors[0] = true; // for now we have no way of specifying which behavior is the initial one except that it's behavior 0
        for (int i = 0; i < (isSingleState() ? 0 : behaviors.length); i++)
            if (getTransition(i) != null)
                {
                LearnedTransition lt = (LearnedTransition) getTransition(i);
                if (lt.getExamples() != null)
                    {
                    int s = lt.getExamples().size();
                    for (int j = 0; j < s; j++)
                        usedBehaviors[((Example)(lt.getExamples().get(j))).classification] = true;         // IMPROVE: only include states that the DECISION TREE has as leaf nodes, not the examples
                    }
                }

        for (int i = 0; i < behaviors.length; i++)
            if (usedBehaviors[i])  // we're using this behavior so should extract the targets from it
                for (int j = 0; j < behaviors[i].getTargets().length; j++)
                    if (behaviors[i].getTarget(j) instanceof Wrapper)
                        {
                        Wrapper wrapper = (Wrapper)(behaviors[i].getTarget(j));
                        targetsUsed[wrapper.getIndex()] = true;
                        }

        // likewise search features 
        // IMPROVE: only include features which the DECISION TREE has leaf nodes for
        for (int i = 0; i < features.length; i++)
            for (int j = 0; j < features[i].getNumTargets(); j++)
                if (features[i].getTarget(j) instanceof Wrapper)
                    {
                    Wrapper wrapper = (Wrapper)(features[i].getTarget(j));
                    targetsUsed[wrapper.getIndex()] = true;
                    }
                    
        return targetsUsed;
        }
    
    

    /** Saves out the Trainable Macro. All
        unused behaviors, features, and targets are first replaced with null,
        and domain examples are deleted. Behaviors aren't actually serialized out,
        but rather their names. */
    public void save(Agent agent)
        {
        agent.getHorde().setShouldRebuildJointBehaviorIndices(true);
        
        // first, let's identify which sub-behaviors we never wind up using. This can help us reduce our parameters in the next step
        // as a quick hack we go through all the examples and search for states they transition to. We also add the initial state.
        // A smarter, possibly more sensitive approach would be to identify which states are referenced in the decision trees themselves;
        // this would only be different from the quick hack when the leaf nodes are non-deterministic and have been trimmed so they're
        // no longer 100% classifying the training data. For now we're not doing that so the quick hack is as sensitive as possible anyway.
        boolean[] usedBehaviors = new boolean[behaviors.length];
        usedBehaviors[0] = true; // for now we have no way of specifying which behavior is the initial one except that it's behavior 0
        for (int i = 0; i < (isSingleState() ? 1 : behaviors.length); i++)
            if (getTransition(i) != null)
                {
                LearnedTransition lt = (LearnedTransition) getTransition(i);
                if (lt.getExamples() == null)
                    System.err.println("Examples were null in saving out");
                else
                    {
                    int s = lt.getExamples().size();
                    for (int j = 0; j < s; j++)
                        usedBehaviors[((Example)(lt.getExamples().get(j))).classification] = true;         // IMPROVE: only include states that the DECISION TREE has as leaf nodes, not the examples
                    }
                }

        // before saving, let's reduce our parameters. We'll do this by marking some of them as null temporarily, saving, then setting back
        Target[] backup = new Target[targets.length];
        System.arraycopy(targets, 0, backup, 0, targets.length);

        // set all my parameters to null
        for (int i = 0; i < targets.length; i++)
            targets[i] = null;

        // now set some of them back if we discover we use them
        for (int i = 0; i < behaviors.length; i++)
            if (usedBehaviors[i])  // we're using this behavior so should extract the targets from it
                for (int j = 0; j < behaviors[i].getTargets().length; j++)
                    if (behaviors[i].getTarget(j) instanceof Wrapper)
                        {
                        Wrapper wrapper = (Wrapper)(behaviors[i].getTarget(j));
                        targets[wrapper.getIndex()] = backup[wrapper.getIndex()]; // restore; we're using it
                        }

        // likewise search features 
        // IMPROVE: only include features which the DECISION TREE has leaf nodes for
        for (int i = 0; i < features.length; i++)
            for (int j = 0; j < features[i].getNumTargets(); j++)
                if (features[i].getTarget(j) instanceof Wrapper)
                    {
                    Wrapper wrapper = (Wrapper)(features[i].getTarget(j));
                    targets[wrapper.getIndex()] = backup[wrapper.getIndex()]; // restore; we're using it
                    }
                    
        
        // null out the unused behaviors and transitions
        Behavior[] tempBehaviors = new Behavior[behaviors.length];
        Transition[] tempTransitions = new Transition[behaviors.length];
        for (int i = 0; i < behaviors.length; i++)
            {
            tempBehaviors[i] = behaviors[i];
            if (isSingleState() && i > 0)
                tempTransitions[i] = null;  // all transitions > 0 are null
            else
                tempTransitions[i] = getTransition(i);
            if (!usedBehaviors[i])
                {
                behaviors[i] = null;
                if (i == 0 || !isSingleState())  // don't null out single state transition unless it's 0 because it'll get routed to 0
                    setTransition(i, null);
                }
            }

        // build the behavior names and targets and null out the remaining behaviors
        behaviorNames = new String[behaviors.length];
        behaviorTargets = new Target[behaviors.length][];
        for (int i = 0; i < behaviors.length; i++)
            {
            if (behaviors[i] != null)
                {
                behaviorNames[i] = behaviors[i].getName();
                behaviorTargets[i] = behaviors[i].getTargets();  // we'll just take 'em -- no need to clone, since we're deleting the behavior anyway, it won't them any more
                behaviors[i] = null;
                }
            }

        // now save the macro
        try
            {
            ObjectOutputStream s = 
                new ObjectOutputStream(
                    new GZIPOutputStream(
                        new BufferedOutputStream(
                            new FileOutputStream(Horde.getPathRelativeToClass(Horde.locationRelativeClass, Horde.AGENT_DIRECTORY) + 
                                "agents/" + agent.getName() + "/" + getName() + TRAINABLE_MACRO_EXTENSION))));
            s.writeObject(this);
            s.close();
            System.err.println("Wrote out trainable macro " + agent.getName() + "/" + getName());
            } 
        catch (IOException e)
            {
            throw new RuntimeException("Couldn't write trainable macro " + agent.getName() + "/" + getName() + 
                "\nto file: " + (Horde.getPathRelativeToClass(Horde.locationRelativeClass, Horde.AGENT_DIRECTORY) + 
                    "agents/" + agent.getName() + "/" + getName() + TRAINABLE_MACRO_EXTENSION) , e);
            } 
        finally
            {
            // restore the behaviors and transitions
            System.arraycopy(tempBehaviors, 0, behaviors, 0, behaviors.length);
            System.arraycopy(tempTransitions, 0, getTransitions(), 0, transitions.length);  // if single state it'll just restore a bunch of nulls, that's okay

            // now restore the parameters
            System.arraycopy(backup, 0, targets, 0, backup.length);

            // null out the behavior names and targets to save space
            behaviorNames = null;
            behaviorTargets = null;
            }
        }
                

    /** Loads a trainable macro and sets its training to be false. Assumes the extension is already attached to the filename. */
    static TrainableMacro load(String filename, Agent agent)
        {               
        agent.getHorde().setShouldRebuildJointBehaviorIndices(true);
        if (!filename.endsWith(TRAINABLE_MACRO_EXTENSION))  // we need to check this because we're gonna strip it off below
            {
            throw new RuntimeException("Spurious file " + filename + " in trainable macro directory, does not end with " +
                TRAINABLE_MACRO_EXTENSION);
            }

        try
            {
            ObjectInputStream s = 
                new ObjectInputStream(
                    new GZIPInputStream(
                        new BufferedInputStream(
                            new FileInputStream(Horde.getPathRelativeToClass(Horde.locationRelativeClass, Horde.AGENT_DIRECTORY) +
                                "agents/" + agent.getName() + "/" + filename))));

            TrainableMacro tm = (TrainableMacro) s.readObject();
            s.close();

            if (!tm.getName().equals(filename.substring(0, filename.length() - TRAINABLE_MACRO_EXTENSION.length())))  // uh oh
                throw new RuntimeException("Trainable Macro's name (" + tm.getName() + ") does not match filename (" + filename + ")");

            // restart from the checkpoint
            tm.currentBehavior = UNKNOWN_BEHAVIOR;
            tm.training = false;

            System.err.println(tm.targets.length);
            for(int i = 0; i < tm.targets.length; i++)
                System.err.println(tm.targets[i]);

            return tm;
            } 
        catch (Exception e)
            {
            throw new RuntimeException("Couldn't read trainable macro " + filename, e);
            }
        }
        

    public Domain getDomain()
        {
        return Feature.buildDomain(getName(), features, behaviors);
        }

    /** Resets the TrainableMacro to the current Horde environment, including parameters and names, behaviors, and current features. */
    public void reset(Horde horde, Behavior[] behaviors, Feature[] features)
        {
        horde.setShouldRebuildJointBehaviorIndices(true);
        targets = horde.buildNewParameters();
        targetNames = new String[targets.length];
        for(int i = 0; i < targets.length; i++)
            targetNames[i] = ((Parameter)(targets[i])).getName();
        this.features = features;
        this.behaviors = behaviors;
        this.transitions = new Transition[behaviors.length];           // all transitions are initially null. We'll add some as the user presses buttons
        training = true;

        // as a hack we are not setting currentBehavior to UNKNOWN_BEHAVIOR.
        // This is because it's possible reset() is called and we're NOT STOPPED. In which
        // case the current behavior would be UNKNOWN_BEHAVIOR when the next go() is called which
        // would be BAD. Instead what we do is reset to INITIAL_BEHAVIOR if it's not
        // presently UNKNOWN_BEHAVIOR.
        if (currentBehavior != UNKNOWN_BEHAVIOR)
            {
            int oldBehavior = currentBehavior;
            currentBehavior = INITIAL_BEHAVIOR;
            horde.observer.transitioned(this, oldBehavior, currentBehavior);
            }

        resetFlags();

        // Rebind parameters to new wrappers for behaviors
        for (int i = 0; i < behaviors.length; i++)
            {
            for (int j = 0; j < behaviors[i].getNumTargets(); j++)
                {
                if (behaviors[i].getTarget(j) != null &&              // because it was unused when saved. See save()
                    behaviors[i].getTarget(j) instanceof Parameter)  // needs to be rebound. Assume that the index of horde is the same as the index for this macro
                    {
                    Parameter p = ((Parameter)(behaviors[i].getTarget(j)));
                    behaviors[i].setTarget(j, new Wrapper(getTargetName(p.getIndex()), p.getIndex()));  // we'll use p.getIndex() as the default but it might not be a good choice
                    }
                }
            }

        // Rebind parameters to new wrappers for features. Assume that the index of horde is the same as the index for this macro. These will not be rebound.
        for (int i = 0; i < features.length; i++)
            {
            for (int j = 0; j < features[i].getNumTargets(); j++)
                {
                if (features[i].getTarget(j) instanceof Parameter) // needs to be rebound.
                    {
                    Parameter p = ((Parameter)(features[i].getTarget(j)));
                    features[i].setTarget(j, new Wrapper(getTargetName(p.getIndex()), p.getIndex()));  // we'll use p.getIndex() as the default but it might not be a good choice
                    }
                }
            }
        }

    /** More or less the same as Macro.go() if the agent is not in training mode.  If the agent is
        in training mode, then we transition only when the user requests a transition, then log
        some examples. At present the examples are as follows:
           
        1. [previous behavior, current features, new behavior]
        2. [new behavior, current features, new behavior] (to provide a default)
    */

    public int startBehavior = 0;
    public void go(Agent agent, Macro parent, Horde horde)
        {
        super.goBypass(agent, parent, horde); // call super.super.go(...)
        _parent = parent;

        if (currentBehavior == UNKNOWN_BEHAVIOR) // should never happen
            throw new RuntimeException("go() called on UNKNOWN_BEHAVIOR. This should not be able to happen.  Agent=" + agent + "  Name=" + name );

        boolean iAmTheTrainingMacro = (agent == horde.getTrainingAgent() && agent.getBehavior() == this);

        int newBehavior = currentBehavior;

        if (userAskedForNewBehavior)  // we've pressed a button while training, or our parent told us to change. Make it permanent.
            {
            // System.err.println("User changed behavior: "+ this);
            newBehavior = newBehaviorRequestedByUser;
            if (newTargetsRequestedByUser != null)
                { behaviors[newBehavior].setTargets(newTargetsRequestedByUser); newTargetsRequestedByUser = null; }
            userAskedForNewBehavior = false;
            if (newBehavior == UNKNOWN_BEHAVIOR)
                throw new RuntimeException("2. go() called on UNKNOWN_BEHAVIOR. This should not be able to happen.");
            }
        else if (iAmTheTrainingMacro && training)
            {
            // System.err.println("I am training: " + this);
            // do nothing -- the user didn't ask us to change, so we won't, even
            // if we think we're smart by now...
            }
        else if (startBehavior != 0 && currentBehavior == 0)
            newBehavior = startBehavior;
        else
            {
            // System.err.println("Transitioning: " + this);
            Transition transition = getTransition(currentBehavior);
            if (transition != null)
                {
                newBehavior = transition.change(agent, this, horde);
                if (newBehavior == UNKNOWN_BEHAVIOR)
                    throw new RuntimeException("3. go() called on UNKNOWN_BEHAVIOR. This should not be able to happen.");
                }
            }

        if (behaviors[newBehavior] instanceof Flag) // like "done"
            {
            fireFlag(((Flag)(behaviors[newBehavior])).getFlag(), agent, parent, horde);
            
//            if (horde.getTrainingMacro() != this || horde.getRestartOnFlags())
//                  newBehavior = INITIAL_BEHAVIOR; // immediately transition
            }

        if (newBehavior != currentBehavior)
            {
            // System.err.println("Transition " + this + " " + behaviors[currentBehavior] + " -> " + behaviors[newBehavior]);
            behaviors[currentBehavior].stop(agent, this, horde);
            Transition transition = getTransition(currentBehavior);
            if (transition != null) transition.stop(agent, this, horde);

            // we set the currenBehavor here so that in agent.behaviorBacktrace()
            // or Behavior.behaviorBacktrace() the current behavior is properly
            // listed even if it's called during the start(...) method of the 
            // new behavior.  This often happens if we need to debug the start(...)
            // method when it's the only place in the behavior that anything happens.
                        
            previousBehavior = currentBehavior;
            currentBehavior = newBehavior;

            behaviors[newBehavior].start(agent, this, horde);
            Transition newTransition = getTransition(newBehavior);
            if (newTransition != null) newTransition.start(agent, this, horde);
            if (iAmTheTrainingMacro && horde.observer != null)
                horde.observer.transitioned(this, previousBehavior, newBehavior);
            resetFlags();
            // don't signal done here in the horde (no signalFlag(...))
            }

        // this will now be the new behavior
        behaviors[currentBehavior].go(agent, this, horde);
        }

    /**
     * Removes the last example from the example database ie. "undo"s the last
     * addExample.
     */

    public void removeLastExample()
        {
        if (previousBehavior == UNKNOWN_BEHAVIOR) return;
        if (getTransition(previousBehavior) == null) return;

        LearnedTransition lt = ((LearnedTransition) getTransition(previousBehavior));

        // reset to the previous behavior
        setNewBehaviorRequestedByUser(previousBehavior, null);

        if (lt.getExamples().isEmpty()) return;

        // delete the most recent example
        int idx = lt.getExamples().size();
        Example e = (Example) (lt.getExamples().remove(idx - 1));

        // check for a default example, and remove that too
        lt = ((LearnedTransition) getTransition(currentBehavior));

        for (int i = 0; i < lt.getExamples().size(); i++)
            {
            Example e1 = (Example) (lt.getExamples().get(i));
            if (e1.classification == e.classification && e1.continuation != e.continuation)
                {
                boolean equalValues = true;
                for (int j = 0; j < e1.values.length; j++)
                    {
                    if (e1.values[j] != e.values[j])
                        {
                        equalValues = false;
                        break;
                        }
                    }

                if (equalValues) 
                    lt.getExamples().remove(e1);

                break;
                }
            }
        }
               
    public void dumpExamples()
        {
        if (isSingleState())
            {
            LearnedTransition lt = (LearnedTransition)(transitions[0]);
            if (lt != null)
                lt.dumpExamples();
            }
        else
            {
            for(int i = 0; i < behaviors.length; i++)
                {
                LearnedTransition lt = (LearnedTransition)(transitions[i]);
                if (lt != null)
                    lt.dumpExamples();
                }
            }
        }
                
    /** Moves all examples from one trainable macro to another.  Note that the macros must be
        of the same kind.  This function does not check for that. */
    public void transferExamplesFrom(TrainableMacro from)
        {
        if (this == from)  // duh, don't clear examples
            return;

        clearExamples();
        setSingleState(from.isSingleState());                
        
        for(int i = 0; i < (from.isSingleState() ? 1 : behaviors.length); i++)
            {
            LearnedTransition lt = (LearnedTransition)(getTransition(i));
            LearnedTransition ltf = (LearnedTransition)(from.getTransition(i));
            if (ltf == null)
                {
                if (lt != null)
                    System.err.println("WARNING: Macro transfer from null transition in transferExamplesFrom(), this makes no sense, probably a bug.");
                // else do nothing, nothing to transfer
                }
            else
                {
                if (lt == null)  // can happen when I've made a few examples initially without training
                    {
                    lt = (LearnedTransition)(ltf.clone());  // this clones the examples too
                    setTransition(i, lt);
                    }
                else  // just transfer the examples
                    {
                    lt.setExamples(ltf.getExamples());
                    }
                }
            }
        from.clearExamples();
        }
                
    /** Clears all examples in the TrainableMacro */
    public void clearExamples()
        {
        for(int i = 0; i < behaviors.length; i++)
            {
            LearnedTransition lt = (LearnedTransition)(getTransition(i));
            if (lt != null)
                lt.getExamples().clear();
            }
        }

    public int getNumExamples()
        {
        int count = 0;
                
        for(int i = 0; i < behaviors.length; i++)
            {
            LearnedTransition lt = (LearnedTransition)(getTransition(i));
            if (lt != null)
                count += lt.getExamples().size();
            }
            
        return count;
        }

    /**
       Adds an example based on the current scenario and the desired new behavior.
    */
    protected void addExample(Horde horde, int newBehavior)
        {
        if (currentBehavior == UNKNOWN_BEHAVIOR || newBehavior == UNKNOWN_BEHAVIOR) return;
        if (behaviors[currentBehavior] instanceof Flag) return;
        if (behaviors[newBehavior] instanceof Start) return;

        if (getTransition(currentBehavior) == null)
            setTransition(currentBehavior, new LearnedTransition(//features, 
                    getDomain()));

        LearnedTransition lt = ((LearnedTransition) getTransition(currentBehavior));
        Example e = lt.getExample(horde.getTrainingAgent(), this, horde);

        if (newBehavior == currentBehavior) // strip out non-default features
            for (int i = 0; i < features.length; i++)
                if (features[i] instanceof NonDefaultFeature) 
                    e.values[i] = 0.0;

        e.classification = newBehavior;
        e.continuation = (newBehavior == currentBehavior);
        lt.getExamples().add(e);
        }

    /**
       Generates printed classifiers and displays them..
    */
    public void showClassifiers(Horde horde)
        {
        int count = 0;
        for (int i = 0; i < (isSingleState() ? 1 : behaviors.length); i++)
            {
            try
                {
                Transition t = getTransition(i);
                if (t == null) 
                    continue;
                LearnedTransition lt = (LearnedTransition) t;
                lt.showTransition(i, behaviors[i].toString());
                count++;
                } 
            catch (Throwable ex)
                {
                System.err.println("Error in TrainableMacro.showClassifiers(): " + ex);
                ex.printStackTrace();
                }
            }
        if (count == 0)
            sim.util.gui.Utilities.inform("No Learned Model to Show.", "Check, then uncheck \"training\" first?", null);
        }

    /** Dumps out all the examples to be examined for debugging purposes. */
    public void logExamples(String path, String name, Horde horde)
        {
        try
            {
            File behaviorFile = new File(path, name + ".behavior");
            PrintWriter writer = new PrintWriter(new FileWriter(behaviorFile));
            writeTopLevel(writer);
            writer.close();
            } 
        catch (Exception e) { e.printStackTrace(); }

        LearnedTransition lt = null;
        for (int i = 0; i < behaviors.length; i++)
            {
            Transition t = getTransition(i);
            if (t == null) 
                continue;
            lt = (LearnedTransition) t;
            String behaviorName = behaviors[i].getName();
            lt.logExamples(path, horde, behaviorName);
            }
        LearnedTransition.logDomain(getDomain(), path, name);
        }

    /**
       Learns the classifiers from the current examples stored so far.
    */
    protected void learnAll(Horde horde)
        {
        for (int i = 0; i < behaviors.length; i++)
            {
            Transition t = getTransition(i);
            if (t == null) 
                continue;
            LearnedTransition lt = (LearnedTransition) t;
            lt.learn(horde);
            }
        }



    /** Loads all trainable macros from the "trained" directory and creates instances of them. */
    public static TrainableMacro[] provideAllTrainableMacros(final Agent agent, Behavior[] basic)  // basic might also include joints
        {
        agent.getHorde().setShouldRebuildJointBehaviorIndices(true);
        ArrayList<TrainableMacro> allTrainableMacros = new ArrayList<TrainableMacro>();
        HashMap<String, Behavior> map = new HashMap<String, Behavior>();
                
        for (int i = 0; i < basic.length; i++)
            {
            if (map.containsKey(basic[i].getName()))
                throw new RuntimeException("Behaviors with duplicate names found on loading trainable macro: " + basic[i].getName() + 
                    "\n" + map.get(basic[i].getName()) + "\n" + basic[i]);
            map.put(basic[i].getName(), basic[i]);
            }
                                
        try
            {
            // reload
            FilenameFilter pbmonly = new FilenameFilter()
                {
                public boolean accept(File dir, String name) { return name.endsWith(TRAINABLE_MACRO_EXTENSION); }
                };

            // Find and load all macros with the agent name as a prefix
            String[] list = new File(Horde.getPathRelativeToClass(Horde.locationRelativeClass, Horde.AGENT_DIRECTORY) + 
                "agents/" + agent.getName() + "/").list(pbmonly);
            if (list != null)
                {
                for (int i = 0; i < list.length; i++)
                    {
                    try
                        {
                        TrainableMacro m = load(list[i], agent);
                        if (m != null) 
                            {
                            if (map.containsKey(m.getName()))
                                throw new RuntimeException("Behaviors with duplicate names found on loading trainable macro: " + m.getName() + 
                                    "\n" + map.get(m.getName()) + "\n" + m);
                            m = (TrainableMacro)(m.clone());
                            allTrainableMacros.add(m);
                            map.put(m.getName(), m); 
                            }
                        } 
                    catch (Exception e) { e.printStackTrace(); }
                    }

                // we have now loaded all the trainable macros.  Perform substitution.

                HashSet loaded = new HashSet();
                HashSet failed = new HashSet();
                ArrayList macros = new ArrayList(allTrainableMacros);
                                
                // Perform substitutions and verify cycles
                for (int i = 0; i < macros.size(); i++)
                    {
                    TrainableMacro m = (TrainableMacro)(macros.get(i));
                    if (loaded.contains(m))
                        {
                        // System.err.println("ALREADY ADDED " + m.getName());
                        }
                    else if (m.performSubstitution(map, loaded, failed, new HashSet()))
                        {
                        System.err.println("ALREADY ADDED " + m.getName());
                        // m = (Behavior)(m.clone());  // do we need to do this?
                        }
                    else if (m.performSubstitution(map, loaded, failed, new HashSet()))
                        {
                        // System.err.println("ADDING " + m.getName());
                        }
                    else
                        {
                        System.err.println("Failed to Add " + m.getName());
                        map.remove(m);
                        macros.remove(i);
                        i--; // try again
                        }
                    }
                }
            } 
        catch (Exception e) { e.printStackTrace(); }

        return allTrainableMacros.toArray(new TrainableMacro[allTrainableMacros.size()]);
        }






    /**
     * Go through all behaviors utilized by the macro. Make sure that those
     * behaviors are still available to the agent, that all sub-macros are
     * loaded, that the correct number of behavior targets are available for
     * each sub-macro. Add wrappers for any targets that are parameters
     * 
     * @param map All behaviors available to the agent, hashed by name
     * @param loaded Behaviors already substituted successfully
     * @param failed Behaviors which failed substitution
     * @param cycles A set of behaviors presently recursed into via performSubstitution
     * @return
     */
    boolean performSubstitution(HashMap map, HashSet loaded, HashSet failed, HashSet cycles)
        {
        if (cycles.contains(this)) // uh oh, cycles
            {
            System.err.println("WARNING: Cycles in loading TrainableMacro " + this);
            failed.add(this);
            return false;
            }

        cycles.add(this);

        for (int j = 0; j < behaviorNames.length; j++)
            {
            if (behaviorNames[j] != null)
                {
                Behavior b = (Behavior)(map.get(behaviorNames[j]));
                if (b == null)
                    {
                    System.err.println("WARNING: Cannot load TrainableMacro " + this + " because it expected an unknown behavior called " + behaviorNames[j]);
                    failed.add(this);
                    cycles.remove(this);
                    return false;
                    }
                else
                    {
                    if (b instanceof TrainableMacro)
                        {
                        // System.err.println("TRYING " + b);
                        if (failed.contains(b) ||
                            (!loaded.contains(b) && !((TrainableMacro) b).performSubstitution(map, loaded, failed, cycles))) // recurse
                            {
                            System.err.println("WARNING: Cannot load TrainableMacro " + this + " because it expected a TrainableMacro called " + behaviorNames[j] + " which failed to load.");
                            failed.add(this);
                            cycles.remove(this);
                            return false;
                            }
                        // else fall thru below
                        }

                    // FALL THRU
                    behaviors[j] = (Behavior)(b.clone()); // clone the behavior

                    // copy over the targets
                    if (behaviorTargets[j] == null) // uh oh
                        {
                        System.err.println("WARNING: Cannot load TrainableMacro " + this + "because it expected a behavior called " + behaviorNames[j] + " but is missing targets for it.");
                        failed.add(this);
                        cycles.remove(this);
                        return false;
                        }
                    else if (behaviors[j].getNumTargets() != behaviorTargets[j].length) // uh oh
                        {
                        System.err.println("WARNING: Cannot load TrainableMacro " + this + "because it expected a behavior called " + behaviorNames[j] + " with " + behaviorTargets[j].length + " targets, but it got " + behaviors[j].getNumTargets() + " instead.");
                        failed.add(this);
                        cycles.remove(this);
                        return false;
                        }
                    else
                        {
                        behaviors[j].setTargets(behaviorTargets[j]);
                        }

                    // now convert any parameters to wrappers,
                    // and fix broken wrappers
                    for (int k = 0; k < behaviors[j].getNumTargets(); k++)
                        {
                        Target t = ((Target)(behaviors[j].getTarget(k)));
                        if (t != null &&                 // because it was unused when saved. See save()
                            t instanceof Parameter)  // needs to be rebound.
                            {
                            Parameter p = (Parameter) t;
                            String newName = getTargetName(p.getIndex());
                            behaviors[j].setTarget(k, new Wrapper(newName, p.getIndex()));  // we'll use p.getIndex() as the default but it might not be a good choice
                            }
                        else if (t != null &&                 // because it was unused when saved. See save()
                            t instanceof Wrapper)  // needs to be re-wrapped.
                            {
                            // because behaviors can have their parameter names changed 
                            // right when they're saved out, it's often the case that wrappers have incorrect 
                            // names now.  So we fix them here.
                            Wrapper p = (Wrapper) t;
                            String newName = getTargetName(p.getIndex());
                            p.setName(newName);
                            }
                        }
                    }
                }
            }
        // get rid of behaviorNames and behaviorTargets
        behaviorNames = null;
        behaviorTargets = null;

        loaded.add(this);
        cycles.remove(this);
        return true;
        }


    public void writeAllTargets(PrintWriter writer)
        {
        HashSet t = new HashSet();
        writer.print(" ( targets ");
        String s = writeAllTargetsHelp(t);
        writer.print(t.size() + " " + s);
        writer.print(")\n");
        }


    public String writeAllTargetsHelp(HashSet targetsSoFar)
        {
        String s = super.writeAllTargetsHelp(targetsSoFar);
        for (int i = 0; i < features.length; i++)
            if (features[i] != null) 
                s += features[i].writeAllTargetsHelp(targetsSoFar);
        for (int i = 0; i < behaviors.length; i++)
            if (behaviors[i] != null) 
                s += behaviors[i].writeAllTargetsHelp(targetsSoFar);
        return s;
        }


    public void writeAllFeatures(PrintWriter writer)
        {
        HashSet t = new HashSet();
        writer.print(" ( features ");
        String s = writeAllFeaturesHelp(t);
        writer.print(t.size() + " " + s);
        writer.print(")\n");
        }


    public String writeAllFeaturesHelp(HashSet featuresSoFar)
        {
        String str = "";
        for (int i = 0; i < behaviors.length; i++)
            if (behaviors[i] != null && behaviors[i] instanceof TrainableMacro)
                str += ((TrainableMacro) (behaviors[i])).writeAllFeaturesHelp(featuresSoFar);
        for (int i = 0; i < features.length; i++)
            {
            String s = features[i].writeToString();
            if (!featuresSoFar.contains(s))
                {
                str += (s + " ");
                featuresSoFar.add(s);
                }
            }
        return str;
        }


    public void writeTopLevel(PrintWriter writer)
        {
        // write out all targets
        writeAllTargets(writer);

        // write out all features
        writeAllFeatures(writer);

        // write out the number of behaviors
        writer.print("( behavior-names " + behaviors.length + " ");
        int fsacount = 0;
        for (int i = 0; i < behaviors.length; i++)
            if (behaviors[i] instanceof TrainableMacro) 
                fsacount++;
        writer.print("" + fsacount + " ");
        for (int i = 0; i < behaviors.length; i++)
            writer.print(" " + behaviors[i].getName());
        writer.print(" )\n\n");

        // write out all behaviors
        HashSet behaviorsSoFar = new HashSet();
        for (int i = 0; i < behaviors.length; i++)
            if (behaviors[i] != null)
                {
                writer.print("%%% ----- " + i + " ------\n");
                behaviors[i].write(writer, behaviorsSoFar);
                }
        }

    public void write(PrintWriter writer, HashSet behaviorsSoFar)
        {
        // write out the subsidiary behaviors FIRST
        // for(int i = 0 ; i < behaviors.length; i++)
        // if (behaviors[i] != null)
        // behaviors[i].write(writer, behaviorsSoFar);

        // Now write me
        writer.print(" ( trainable-macro " + name + " ");

        writer.print(" ( targets " + targets.length + " ");
        for (int i = 0; i < targets.length; i++)
            writer.print(targetNames[i] + " ");
        writer.print(")\n");

        writer.print(" ( behaviors " + behaviors.length + " ");
        for (int i = 0; i < behaviors.length; i++)
            if (behaviors[i] != null)
                writer.print(behaviors[i].name + " ");
            else writer.print(" null ");
        writer.print(")\n");

        writer.print(" ( features " + features.length + " ");
        for (int i = 0; i < features.length; i++)
            features[i].write(writer);
        writer.print(")\n");

        getDomain().write(writer);

        writer.print(" ( transitions\n");
        for (int i = 0; i < behaviors.length; i++)
            if (getTransition(i) != null)
                getTransition(i).write(writer, false);
            else writer.print(" null ");
        writer.print(")\n");

        writer.print(")\n\n");
        }

    }
