package sim.app.horde.behaviors;

import java.util.logging.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;

import sim.app.horde.transitions.*;
import sim.app.horde.*;
import sim.app.horde.classifiers.Domain;
import sim.app.horde.classifiers.Example;
import sim.app.horde.features.*;
import sim.app.horde.targets.*;

import sim.app.horde.irl.* ;

/**
    TRAINABLE MACRO

    <p>Trainable Macros are Macros which have LEARNABLE TRANSITIONS as their transition
    functions.  These transitions are essentially wrappers for classifiers (for now decision
    trees).  Trainable Macros gather exemplars as the user directs them to transition from
    behavior to behavior, then when learn() is called they build LearnableTransitions from
    these exemplars.

    <p>TrainableMacros can then be saved to disk via serialization, at which point they can
    be reloaded and used with their LearnableTransitions as the fixed transitions.

    <p>When TrainableMacros are saved, they're saved to a particular directory "learned"
    in the "behaviors" directory.  They're saved out with ".trained" extensions.

    <p>TrainableMacros are in two modes: training or not training.  When in training, they
    gather examples and only transition when the user requests it.  When in not training
    (that is, after they've learned), they follow their learned transition functions.
*/

public class TrainableMacro extends Macro implements Serializable
{
	private static final long serialVersionUID = 1;
	static final String TRAINABLE_MACRO_EXTENSION = ".trained";

	int previousBehavior = UNKNOWN_BEHAVIOR;

	public TrainableMacro()
	{
		super();
		finished = false;
	}

	// am I a slave of a parent controller agent?
	boolean iAmSlave = false;
	public void setIAmSlave(boolean val)
	{
		iAmSlave = val;
	}
	public boolean getIAmSlave(boolean val)
	{
		return iAmSlave;
	}

	// matches button:  true if the macro is presently
	// in training mode (gathering exemplars).
	boolean isRecording = true;
	public boolean isRecording()
	{
		return isRecording;
	}

	// this junk is for specifying whether or not a training macro is designed to be
	// one-shot or continuous, that is, using default examples when uses in a higher-level FSA.
	boolean shouldAddDefaultExample = true;
	public boolean getShouldAddDefaultExample()
	{
		return shouldAddDefaultExample;
	}
	public void setShouldAddDefaultExample(boolean val)
	{
		shouldAddDefaultExample = val;
	}

	// when this flag is on, there will be no recording of examples
	// no learning and no classifying, the agent will just
	// move according to the user command
	// if isHitab == false then
	// 	!learnPolicy()
	// 	!addExample()
	// 	!change()
	boolean isHitab = true ;
	boolean isManualDriving = false ;
	public boolean isManualDriving()
	{
		return isManualDriving;
	}
	public void setManualDriving(boolean val)
	{
		isManualDriving = val ;
		isHitab = !val ;
		System.err.println("--- TrainableMacro.setManualDriving() :"
		                   + " isManualDriving == " + isManualDriving
		                   + ", isHitab == " + isHitab);
	}

	// if are we going to save detailed snapshots
	boolean isSaveSnapshots = false ;
	public boolean isSaveSnapshots()
	{
		return isSaveSnapshots ;
	}
	public void setSaveSnapshots(boolean val)
	{
		isSaveSnapshots = val ;
		System.err.println("--- TrainableMacro.setSaveSnapshots() :"
		                   + " isSaveSnapshots == " + isSaveSnapshots);
	}

	// if we are doing IRL
	boolean isIrlMode = false ;
	public boolean isIrlMode()
	{
		return isIrlMode ;
	}
	public void setIrlMode(boolean val)
	{
		isIrlMode = val ;
		isHitab = !val ;
		System.err.println("--- TrainableMacro.setIrlMode() :"
		                   + " isIrlMode == " + isIrlMode
		                   + ", isHitab == " + isHitab);
	}

	/** Sets the training mode as requested, and learns the examples. */
	public void learnPolicy(Horde horde, boolean isRecording)
	{
		if(isHitab)
		{
			System.err.println("--- TrainableMacro.learnPolicy() : isRecording = " + isRecording);
			this.isRecording = isRecording;
			userAskedForNewBehavior = false;
			if (!isRecording)
			{
				System.err.println("--- TrainableMacro.learnPolicy() : "
				                   + "recording done, so building the model now.");
				learnAll(horde);
				currentBehavior = 0;
				previousBehavior = 0;
			}
		}
		else
			System.err.println("--- TrainableMacro.learnPolicy() : "
			                   + "isHitab == " + isHitab
			                   + ", so doing nothing.");
	}


	/** IRL stuffs */
	RewardFunction rfunc = null ;
	QTable qtable = null ;
	/** IRL stuffs */
	public void applyIrl(Horde horde)
	{
		if(isIrlMode)
		{
			// note that ExpertDemo.BEHAVIOUR_COUNT is
			// already updated in the Behaviour.java
			System.err.println("--- TrainbaleMacro.applyIrl(): "
			                   + "computing reward function from the expertdemos,"
			                   + " with ExpertDemo.BEHAVIOUR_COUNT == "
			                   + ExpertDemo.BEHAVIOUR_COUNT);
			if(horde.demo != null && horde.demo[0] != null)
			{
				rfunc = new RewardFunction();
				//rfunc.computeWithQCLP(horde.demo, RewardFunction.QCLP_MEAN);
				rfunc.computeWithQclpKmeans(horde.demo, 4, RewardFunction.QCLP_MEAN);
				qtable = new QTable(rfunc);
				// qtable.populateWithRandomValues();
				System.err.println("--- TrainableMacro.applyIrl(): \n"
				                   + "--- Reward Matrix ---\n" + rfunc.toString(false) + "\n"
				                   + "--- Q-Table ---\n" + qtable.toString());
				userAskedForNewBehavior = false ;
				currentBehavior = 0 ;
				previousBehavior = 0 ;
			}
			else
				System.err.println("--- TrainableMacro.applyIrl(): "
				                   + "could not compute reward function,"
				                   + " may be the demos are empty.");
		}
	}

	public void showPerspectiveReward(Horde horde)
	{
		/*System.err.println("--- TrainableMacro.showPerspectiveReward() :"
		                   + " computing perspective reward ...");
		try
		{
			LearnedTransition lt = new LearnedTransition(features, domain);
			Example e = lt.getExample(horde.getTrainingAgent(), this, horde);
			String str = "" ;
			for(int i = 0 ; i < e.values.length ; i++)
				str += e.values[i] + " " ;
			System.err.println("\t --- " + str);
			RewardFunction rf = new RewardFunction();
			rf.computeWithQCLP(horde.demo, RewardFunction.QCLP_MEAN);
			System.err.println("\t --- \n" + rf.toString(false));
			QTable q = new QTable();
			if(e.values.length > 0)
			{
				str = "" ;
				for(int i = 0 ; i < ExpertDemo.BEHAVIOUR_COUNT ; i++)
					for(int j = 1 ; j < ExpertDemo.BEHAVIOUR_COUNT ; j++)
						q.set(i, j, rf.getReward(i, j, e.values)) ;
				System.err.println("\t --- \n" + q.toString());
			}
		}
		catch(Exception e)
		{
			System.err.println("--- TrainableMacro.showPerspectiveReward() :"
			                   + " something went wrong !!");
			e.printStackTrace();
		}*/
	}

	boolean userAskedForNewBehavior = false;
	public void setUserAskedForNewBehavior(boolean f)
	{
		System.out.println("setUser");
		userAskedForNewBehavior = f;
	}
	public boolean getUserAskedForNewBehavior()
	{
		return userAskedForNewBehavior;
	}

	int newBehaviorRequestedByUser = UNKNOWN_BEHAVIOR;
	public void setNewBehaviorRequestedByUser(int b)
	{
		if (b >= 0) newBehaviorRequestedByUser = b;
	}
	public int getNewBehaviorRequestedByUser()
	{
		return newBehaviorRequestedByUser;
	}

	// the domain of the TrainableMacro during training
	protected Domain domain;

	public Domain getDomain()
	{
		return domain;
	}
	// all features of the Horde environment in which the TrainableMacro is learning examples
	public Feature[] features;

	/**
	 * Deep-clones the TrainableMacro, except for the
	 * Domain and Features, which are at present shared.
	 */
	public Object clone()
	{
		TrainableMacro f = (TrainableMacro)(super.clone());

		// clone features
		f.features = new Feature[features.length];
		for(int i = 0 ; i < f.features.length; i++)
			f.features[i] = features[i];

		// don't bother cloning the domain
		f.domain = domain;

		return f;
	}

	/**
	 * Called when the user wishes to change the
	 * behavior of the TrainableMacro to a new behavior,
	 * Next time go() is called, it'll transition to this behavior.
	 */
	public void userChangedBehavior(Horde horde, int newBehavior)
	{
		System.err.println("--- TrainableMacro.userChangedBehavoir() :"
		                   + " behavior changed by user.\n");
		userAskedForNewBehavior = true;
		newBehaviorRequestedByUser = newBehavior;
		if (isRecording && currentBehavior != UNKNOWN_BEHAVIOR &&
		        behaviors[currentBehavior].shouldAddExamples())
		{
			addExample(horde, newBehavior);
			if (behaviors[newBehavior].getShouldAddDefaultExample() &&
			        currentBehavior != newBehavior)
			{
				// add an example that says "keep going like this new situation"
				int temp = currentBehavior;
				currentBehavior = newBehavior;
				addExample(horde, newBehavior);
				currentBehavior = temp;
			}
		}
	}

	/**
	 * Sets the name of the Macro, guaranteeing that it's
	 * valid name for saving out to the disk;
	 * Legal names must be just letters or digits.
	 */
	public void setName(String n)
	{
		// only allow legal names
		for(int i =0; i < n.length(); i++)
			if (!Character.isLetterOrDigit(n.charAt(i)))
				throw new RuntimeException("Bad name"); // failed
		name = n;  // succeeded
	}


	String[] behaviorNames = null;
	Target[][] behaviorTargets = null;

	static final boolean TRIM_BEFORE_SAVING = true;

	/**
	 * Saves out the Trainable Macro, If TRIM_BEFORE_SAVING is true, then
	 * unused behaviors, features, and targets are first replaced with null,
	 * and domain examples are deleted; Behaviors aren't actually serialized out,
	 * but rather their names.
	 */
	public void save()
	{
		// first, let's identify which subbehaviors we never wind up using. This can help
		// us reduce our parameters in the next step as a quick hack we go through all the
		// exemplars and search for states they transition to.  We also add the initial state.
		// A smarter, possibly more sensitive approach would be to identify which states are
		// referenced in the decision trees themselves; this would only be different from the
		// quick hack when the leaf nodes are non-deterministic and have been trimmed so they're
		// no longer 100% classifying the training data.  For now we're not doing that so
		// the quick hack is as sensitive as possible anyway.
		boolean[] usedBehaviors = new boolean[behaviors.length];
		usedBehaviors[0] = true;	// for now we have no way of specifying which behavior
		// is the initial one except that it's behavior 0

		for(int i = 0; i < transitions.length; i++)
			if (transitions[i] != null)
			{
				LearnedTransition lt = (LearnedTransition) transitions[i];
				if (lt.examples == null)
					System.err.println("Examples were null in saving out");
				else
				{
					int s = lt.examples.size();
					for(int j = 0; j < s; j++)
						// IMPROVE: only include states that the DECISION TREE
						// has as leaf nodes, not the examples
						usedBehaviors[((Example)(lt.examples.get(j))).
						              classification] = true;
				}
			}

		// let's temporarily remove all the examples
		// so we don't save them out.  we'll put them back in a second
		ArrayList[] tempExamples = new ArrayList[transitions.length];
		for(int i = 0; i < transitions.length; i++)
			if (transitions[i] != null)
			{
				LearnedTransition lt = (LearnedTransition) transitions[i];
				tempExamples[i] = lt.examples;
				if (TRIM_BEFORE_SAVING) lt.examples = null;
			}

		// before saving, let's reduce our parameters. We'll do this by marking
		// some of them as null temporarily, saving, then setting back
		Target[] backup = new Target[targets.length];
		System.arraycopy(targets, 0, backup, 0, targets.length);

		// set all my parameters to null
		if (TRIM_BEFORE_SAVING)
		{
			// trim out
			for(int i = 0; i < targets.length; i++)
				targets[i] = null;

			// now set some of them back if we discover we use them
			for(int i = 0; i < behaviors.length; i++)
				if (usedBehaviors[i])	// we're using this behavior so
					// should extract the targets from it
					for(int j = 0; j < behaviors[i].getTargets().length; j++)
						if (behaviors[i].getTarget(j) instanceof Wrapper)
						{
							Wrapper wrapper =
							    (Wrapper)(behaviors[i].getTarget(j));
							// restore; we're using it
							targets[wrapper.getIndex()] =
							    backup[wrapper.getIndex()];
						}

			// likewise search features,
			// IMPROVE: only include features which the DECISION TREE has leaf nodes for
			for(int i = 0; i < features.length; i++)
				for(int j = 0; j < features[i].getNumTargets(); j++)
					if (features[i].getTarget(j) instanceof Wrapper)
					{
						Wrapper wrapper = (Wrapper)(features[i].getTarget(j));
						// restore; we're using it
						targets[wrapper.getIndex()] = backup[wrapper.getIndex()];
					}
		}

		// null out the unused behaviors and transitions
		Behavior[] tempBehaviors = new Behavior[behaviors.length];
		Transition[] tempTransitions = new Transition[behaviors.length];
		for(int i = 0; i < behaviors.length; i++)
		{
			tempBehaviors[i] = behaviors[i];
			tempTransitions[i] = transitions[i];
			if (!usedBehaviors[i] && TRIM_BEFORE_SAVING)
			{
				behaviors[i] = null;
				transitions[i] = null;
			}
		}

		// build the behavior names and targets
		// and null out the remaining behaviors
		behaviorNames = new String[behaviors.length];
		behaviorTargets = new Target[behaviors.length][];
		for(int i = 0; i < behaviors.length; i++)
		{
			if (behaviors[i] != null)
			{
				behaviorNames[i] = behaviors[i].getUniqueName();
				behaviorTargets[i] = behaviors[i].getTargets();
				// we'll just take 'em -- no need to clone, since we're
				// deleting the behavior anyway, it won't need them any more
				behaviors[i] = null;
			}
		}

		// now save the macro
		try
		{
			ObjectOutputStream s =
			    new ObjectOutputStream(
			    new GZIPOutputStream (
			        new BufferedOutputStream(
			            new FileOutputStream (Horde.TRAINED_MACRO_DIRECTORY
			                                  + getName() + TRAINABLE_MACRO_EXTENSION))));
			s.writeObject(this);
			s.close();
			System.err.println("Wrote out trainable macro " + getName());
		}
		catch (IOException e)
		{
			throw new RuntimeException("Couldn't write trainable macro " + getName(), e);
		}
		finally
		{
			// restore the behaviors and transitions
			System.arraycopy(tempBehaviors, 0, behaviors, 0, behaviors.length);
			System.arraycopy(tempTransitions, 0, transitions, 0, transitions.length);

			// now restore the parameters
			System.arraycopy(backup, 0, targets, 0, backup.length);

			// and restore the examples
			// let's temporarily remove all the examples so we don't save them out.
			// we'll put them back in a second
			for(int i = 0; i < transitions.length; i++)
				if (transitions[i] != null)
				{
					LearnedTransition lt = (LearnedTransition) transitions[i];
					lt.examples = tempExamples[i];
				}

			// null out the behavior names and targets to save space
			behaviorNames = null;
			behaviorTargets = null;
		}
	}

	/**
	 * Loads a trainable macro and sets its training to be false,
	 * Assumes the extension is already attached to the filename.
	 */
	public static TrainableMacro load(String filename)
	{
		// we need to check this because we're gonna strip it off below
		if (!filename.endsWith(TRAINABLE_MACRO_EXTENSION))
		{
			throw new RuntimeException("Spurious file " + filename +
			                           " in trainable macro directory, does not end with " +
			                           TRAINABLE_MACRO_EXTENSION);
		}
		try
		{
			ObjectInputStream s =
			    new ObjectInputStream(
			    new GZIPInputStream (
			        new BufferedInputStream (
			            new FileInputStream (Horde.TRAINED_MACRO_DIRECTORY
			                                 + filename /* + TRAINABLE_MACRO_EXTENSION */))));

			TrainableMacro tm = (TrainableMacro) s.readObject();
			//System.err.println("Read trainable macro " + tm.getName() + "\t" + tm.level);
			s.close();

			String fname = filename.substring(0,
			                                  filename.length() - TRAINABLE_MACRO_EXTENSION.length());
			if (!fname.equals(tm.getName()))  // uh oh
			{
				System.err.println("WARNING: Trainable Macro " + tm.getName()
				                   + " was located in a file named " + filename
				                   + ".  Renaming the Trainable Macro to " + fname);
				tm.setName(fname);
			}

			// restart from the checkpoint
			tm.currentBehavior = UNKNOWN_BEHAVIOR;
			tm.isRecording = false;
			tm.finished = false;

			return tm;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't read trainable macro " + filename, e);
		}
	}


	/**
	 * Resets the TrainableMacro to the current Horde environment,
	 * including parameters and names, behaviors, and current features.
	 */
	public TrainableMacro reset(Horde horde, Target[] parameters, String[] parameterNames,
	                            Behavior[] behaviors, Feature[] features)
	{
		this.targets = parameters;
		this.targetNames = parameterNames;
		this.features = features;
		this.behaviors = behaviors;
		// all transitions are initially null.  We'll add some as the user presses buttons
		this.transitions = new Transition[behaviors.length];
		domain = Feature.buildDomain(getName(), features, behaviors);
		isRecording = true;

		finished =false;

		// as a hack we are not setting currentBehavior to UNKNOWN_BEHAVIOR.
		// This is because it's possible reset() is called and we're NOT STOPPED.  In which
		// case the current behavior would be UNKNOWN_BEHAVIOR when the next go() is called which
		// would be BAD.  Instead what we do is reset to INITIAL_BEHAVIOR if it's not
		// presently UNKNOWN_BEHAVIOR.
		if (currentBehavior != UNKNOWN_BEHAVIOR)
		{
			int oldBehavior = currentBehavior;
			currentBehavior = INITIAL_BEHAVIOR;
			horde.observer.transitioned(this, oldBehavior, currentBehavior);
		}

		resetFlags();
		// so the agent changes color or whatever, although it'll never happen
		// because we reset before setting the new behavior...
		// horde.signalResetFlags(this);
		// Rebind parameters to new wrappers for behaviors
		for(int i = 0; i < behaviors.length; i++)
		{
			for(int j = 0; j < behaviors[i].getNumTargets(); j++)
			{
				// because it was unused when saved.  See save()
				// needs to be rebound. Assume that the index of horde
				// is the same as the index for this macro
				if (behaviors[i].getTarget(j) != null &&
				        behaviors[i].getTarget(j) instanceof Parameter)
				{
					Parameter p = ((Parameter)(behaviors[i].getTarget(j)));
					// we'll use p.getIndex() as the default but it might
					// not be a good choice
					behaviors[i].setTarget(j, new Wrapper(p.getName(), p.getIndex()));
				}
			}
		}

		// Rebind parameters to new wrappers for features. Assume that the
		// index of horde is the same as the index for this macro.
		// These will not be rebound.
		for(int i = 0; i < features.length; i++)
		{
			for(int j = 0; j < features[i].getNumTargets(); j++)
			{
				// needs to be rebound.
				if (features[i].getTarget(j) instanceof Parameter)
				{
					Parameter p = ((Parameter)(features[i].getTarget(j)));
					// we'll use p.getIndex() as the default but it might not
					// be a good choice
					features[i].setTarget(j, new Wrapper(p.getName(), p.getIndex()));
				}
			}
		}
		return this;
	}

	/**
	 * More or less the same as Macro.go() if the agent is not in training mode,
	 * If the agent is in training mode, then we transition only when the user
	 * requests a transition, then log some examples;
	 * 	At present the examples are as follows:
	 * 		1) [previous behavior, current features, new behavior]
	 * 		2) [new behavior, current features, new behavior] (to provide a default)
	 */
	public int startBehavior = 0;

	/**
	 * Here the agent follows the policy
	 */
	public boolean if1st = false ; // spit out the debug message just for once.
	public boolean if2nd = false ; // spit out the debug message just for once.
	public boolean if3rd = false ; // spit out the debug message just for once.
	public boolean if4th = false ; // spit out the debug message just for once.
	public boolean if5th = false ; // spit out the debug message just for once.
	public boolean if6th = false ; // spit out the debug message just for once.
	static int qCount = 0 ;
	static int sampleCount = 0 ;
	public void go(Agent agent, Macro parent, Horde horde)
	{
		super.goBypass(agent, parent, horde);  // call super.super.go(...)

		if (currentBehavior == UNKNOWN_BEHAVIOR) // should never happen
			throw new RuntimeException("go() called on UNKNOWN_BEHAVIOR. "
			                           + "This should not be able to happen.");

		boolean iAmTheTrainingMacro = (agent == horde.getTrainingAgent() &&
		                               agent.getBehavior() == this);

		int newBehavior = currentBehavior;

		// we've pressed a button while training,
		// or our parent told us to change.  Make it permanent.
		if (//iAmTheTrainingMacro &&
		    userAskedForNewBehavior)
		{
			if(!if1st)
			{
				System.err.println("--- TrainableMacro.go(): 1st if,"
				                   + " user changed behavior: " + this);
				if1st = true ;
			}
			newBehavior = newBehaviorRequestedByUser;
			userAskedForNewBehavior = false;
			if (newBehavior == UNKNOWN_BEHAVIOR)
				throw new RuntimeException("2. go() called on UNKNOWN_BEHAVIOR. "
				                           + "This should not be able to happen.");
		}
		else if (iAmTheTrainingMacro && isRecording && !isIrlMode)
		{
			if(!if2nd)
			{
				System.err.println("--- TrainableMacro.go(): 2nd if,"
				                   + " user changed behavior: " + this);
				if2nd = true ;
			}
			// System.err.println("I am training: "  + this);
			// do nothing -- the user didn't ask us to change, so we won't, even
			// if we think we're smart by now...
		}
		else if (iAmSlave)
		{
			if(!if3rd)
			{
				System.err.println("--- TrainableMacro.go(): 3rd if,"
				                   + " user changed behavior: " + this);
				if3rd = true ;
			}
			// System.err.println("I am slave: " + this);
			// do nothing
		}
		else if (startBehavior != 0 && currentBehavior == 0)
		{
			if(!if4th)
			{
				System.err.println("--- TrainableMacro.go(): 4th if,"
				                   + " user changed behavior: " + this);
				if4th = true ;
			}
			newBehavior = startBehavior;
		}
		else if(isHitab)
		{
			Transition transition = transitions[currentBehavior];
			if (transition != null)
			{
				if(!if5th)
				{
					System.err.println("--- TrainableMacro.go() : 5th if, "
					                   + "getting samples and classifying.\n");
					// new Exception().printStackTrace(System.err);
					if5th = true;
				}
				newBehavior = transition.change(agent, this, horde);
				if (newBehavior == UNKNOWN_BEHAVIOR)
					throw new RuntimeException("3. go() called on UNKNOWN_BEHAVIOR. "
					                           + "This should not be able to happen.");
			}
		}
		else if(isIrlMode)
		{
			LearnedTransition lt = new LearnedTransition(features, domain);
			Example e = lt.getExample(horde.getTrainingAgent(), this, horde);
			System.err.println("--- TrainableMacro.go() : if(isIrlMode), " + e.toString());
			if(!if6th)
			{
				System.err.println("--- TrainableMacro.go() : 6th if, "
				                   + "getting samples and learning the Q-table.");
				if6th =  false ;
			}
			if(qCount == 0)
			{
				newBehavior = qtable.learnQTableModelFree(currentBehavior, e.values, rfunc, 4);
				//newBehavior = qtable.learnQTableModelFree(currentBehavior, e.values, rfunc);
				//newBehavior = qtable.getNextRandomState(currentBehavior);
				//newBehavior = qtable.getNextBestState(currentBehavior);
				++qCount ;
			}
			else if(qCount > 20)
			{
				newBehavior = qtable.learnQTableModelFree(currentBehavior, e.values, rfunc, 4);
				// newBehavior = qtable.learnQTableModelFree(currentBehavior, e.values, rfunc);
				//newBehavior = qtable.getNextRandomState(currentBehavior);
				//newBehavior = qtable.getNextBestState(currentBehavior);
				qCount = 0 ;
			}
			else
			{
				newBehavior = currentBehavior ;
				++qCount ;
			}
			System.err.println("--- TrainableMacro.go() : "
			                   + newBehavior + "-" + behaviors[newBehavior] + " = "
			                   + "qtable.getNextQvalueState("
			                   + currentBehavior + "-" + behaviors[currentBehavior] + ").");
			System.err.println("---\n" + qtable.toString());
			// newBehavior = transition.change(agent, this, horde);
			if (newBehavior == UNKNOWN_BEHAVIOR)
				throw new RuntimeException("3. go() called on UNKNOWN_BEHAVIOR. "
				                           + "This should not be able to happen.");
		}

		if (behaviors[newBehavior] instanceof Flag || finished)  // like "done"
		{
			fireFlag(((Flag)(behaviors[newBehavior])).getFlag(), agent, parent, horde);
			newBehavior = INITIAL_BEHAVIOR;     // immediately transition
			finished = false;

		}

		if (newBehavior != currentBehavior)
		{
			System.err.println("--- TrainableMacro.go() : Transition "
			                   + this + " "  + currentBehavior + "-" + behaviors[currentBehavior]
			                   + " -> " + newBehavior + "-" + behaviors[newBehavior] + "\n");
			//new Exception().printStackTrace(System.err);

			if1st = false ; // spit out the debug message just for once.
			if2nd = false ; // spit out the debug message just for once.
			if3rd = false ; // spit out the debug message just for once.
			if4th = false ; // spit out the debug message just for once.
			if5th = false ; // spit out the debug message just for once.
			if6th = false ; // spit out the debug message just for once.
			behaviors[currentBehavior].stop(agent, this, horde);
			Transition transition = transitions[currentBehavior];
			if (transition != null) transition.stop(agent, this, horde);

			behaviors[newBehavior].start(agent, this, horde);
			Transition newTransition = transitions[newBehavior];
			if (newTransition != null) newTransition.start(agent, this, horde);
			if (iAmTheTrainingMacro && horde.observer != null)
				horde.observer.transitioned(this, currentBehavior, newBehavior);
			resetFlags();
			// don't signal done here in the horde (no signalFlag(...))
			previousBehavior = currentBehavior;
		}
		currentBehavior = newBehavior;
		behaviors[currentBehavior].go(agent, this, horde);
	}

	/**
	 * Removes the last example from the example database
	 * ie, "undo"s the last addExample.
	 */
	public void removeLastExample()
	{
		if (previousBehavior == UNKNOWN_BEHAVIOR ) return;
		if (transitions[previousBehavior] == null) return;

		LearnedTransition lt = ((LearnedTransition) transitions[previousBehavior]);

		// reset the behavior
		userAskedForNewBehavior = true;
		newBehaviorRequestedByUser = previousBehavior;

		if (lt.examples.isEmpty()) return ;

		// delete the most recent example
		int idx = lt.examples.size();
		Example e = (Example)(lt.examples.remove(idx-1));


		// check for a default example, and remove that too
		lt = ((LearnedTransition) transitions[currentBehavior]);

		for (int i=0; i < lt.examples.size(); i++)
		{
			Example e1 = (Example)(lt.examples.get(i));
			if (e1.classification == e.classification && e1.continuation != e.continuation)
			{
				boolean equalValues = true;
				for (int j=0; j < e1.values.length; j++)
				{
					if (e1.values[j] != e.values[j])
					{
						equalValues = false ;
						break;
					}
				}
				if (equalValues)
					lt.examples.remove(e1);
				break;
			}
		}
	}

	/**
	 * Adds an example based on the current
	 * scenario and the desired new behavior.
	 */
	protected void addExample(Horde horde, int newBehavior)
	{
		if (!isHitab) return ;
		if (currentBehavior == UNKNOWN_BEHAVIOR || newBehavior == UNKNOWN_BEHAVIOR) return;
		if (behaviors[currentBehavior] instanceof Flag) return;
		if (behaviors[newBehavior] instanceof Start) return;

		if (transitions[currentBehavior] == null)
			transitions[currentBehavior] = new LearnedTransition(features, domain);

		LearnedTransition lt = ((LearnedTransition) transitions[currentBehavior]);
		Example e = lt.getExample(horde.getTrainingAgent(), this, horde);

		if (newBehavior == currentBehavior)     // strip out non-default features
		{
			Feature[] f = lt.getFeatures();
			for(int i = 0; i < f.length; i++)
				if (f[i] instanceof NonDefaultFeature)
					e.values[i] = 0.0;
		}

		e.classification = newBehavior;
		e.continuation = (newBehavior == currentBehavior);
		System.err.println("--- TrainableMacro.addExample() : adding "
		                   + e.toString());
		lt.examples.add(e);
		// need to store example here
		if(isSaveSnapshots)
			horde.hs.addTrajectoryExample(e,
			                              currentBehavior,
			                              behaviors[currentBehavior].toString(),
			                              newBehavior,
			                              behaviors[newBehavior].toString());

		if (horde.getSingleState())
		{
			for (int i = 0; i < transitions.length; i++)
				transitions[i]=lt;
		}
	}

	/**
	 * Generates printed classifiers and displays them.
	 */
	public void showClassifiers(Horde horde)
	{
		System.err.println("--- TrainableMacro.showClassifiers()");
		System.err.println("--- TrainbaleMacro.transitions.length = " + transitions.length);
		for (int i = 0; i < transitions.length; i++)
		{
			try
			{
				Transition t = transitions[i];
				if (t == null)
				{
					System.err.println("------ TrainableMacro.showClassifiers() : "
					                   + "transitions[" + i + "] == null");
					continue;
				}
				LearnedTransition lt = (LearnedTransition) t;
				System.err.println("------ TrainableMacro.showClassifiers() : "
				                   + "showing behavior "
				                   + behaviors[i].toString());
				lt.showTransition(i, behaviors[i].toString());
				if (horde.getSingleState())
					return;
			}
			catch (IOException ex)
			{
				Logger.getLogger(TrainableMacro.class.getName()).log(Level.SEVERE, null, ex);
			}
			catch (InterruptedException ex)
			{
				Logger.getLogger(TrainableMacro.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Dumps out all the exemplars to be examined for debugging purposes
	 */
	public void logExemplars(String path, String name, Horde horde)
	{
		try
		{
			File behaviorFile = new File(path, name + ".behavior");
			PrintWriter writer = new PrintWriter(new FileWriter(behaviorFile));
			writeTopLevel(writer);
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		LearnedTransition lt=null;
		for (int i = 0; i < transitions.length; i++)
		{
			Transition t = transitions[i];
			if (t == null)
				continue;
			lt = (LearnedTransition) t;
			String behaviorName = behaviors[i].getName();
			lt.logExemplars(path, horde, behaviorName);
		}
		LearnedTransition.logDomain(domain,path,name);
	}


	/**
	 * Learns the classifiers from the current examples stored so far.
	 */
	protected void learnAll(Horde horde)
	{
		for (int i = 0; i < transitions.length; i++)
		{
			Transition t = transitions[i];
			if (t == null)
				continue;
			LearnedTransition lt = (LearnedTransition) t;
			System.err.println("--- TrainableMacro.learnAll() : learning "
			                   + behaviors[i].toString());
			lt.learn(horde);
			// after learning let's save the snapshots
			if(isSaveSnapshots)
			{
				System.err.println("--- TrainableMacro.learnAll() :"
				                   + " saving snapshots for ["
				                   + i + "]: " + behaviors[i].toString());
				horde.hs.setCurrentBehaviorName(behaviors[i].toString());
				horde.hs.setLearnedModel(lt, i);
				horde.hs.dumpHordeSnapshots();
				try
				{
					horde.hs.saveHordeSnapshots();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		if(isSaveSnapshots)
		{
			horde.hs.dumpAllTrajectoryExamples();
			horde.hs.saveAllTrajectoryExamples();
		}
	}

	protected String getUniqueName()
	{
		return "T_" + getName();
	}

	/** Loads all trainable macros from the "trained" directory and creates instances of them. */
	public static TrainableMacro[] provideAllTrainableMacros()
	{
		System.err.println("--- TrainableMacro.provideAllTrainableMacros(): providing ...");
		ArrayList<TrainableMacro> a = new ArrayList<TrainableMacro>();
		try
		{
			// reload
			FilenameFilter pbmonly = new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.endsWith(TRAINABLE_MACRO_EXTENSION);
				}
			};

			String[] list = new File(Horde.TRAINED_MACRO_DIRECTORY).list(pbmonly);
			if (list!=null)
			{
				for(int i = 0; i < list.length; i++)
				{
					try
					{
						TrainableMacro m = load(list[i]);
						if (m!=null) a.add(m);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}

				// Key all behaviors by their names for O(1) lookup
				HashMap map = new HashMap();
				HashSet loaded = new HashSet();
				HashSet failed = new HashSet();
				for(int i = 0; i < Behavior.basicBehaviorClasses.length; i++)
				{
					try
					{
						Behavior b = (Behavior)(Behavior.basicBehaviorClasses[i].
						                        newInstance());
						System.err.println("Adding " + b.getUniqueName());
						if (map.get(b.getUniqueName()) != null)
						{
							System.err.println("WARNING: Multiple behaviors"
							                   + " called " + b.toString());
						}
						else map.put(b.getUniqueName(), b);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				for(int i = 0; i < a.size(); i++)
				{
					try
					{
						TrainableMacro m = (TrainableMacro)(a.get(i).clone());
						System.err.println("Adding " + m.getUniqueName());
						if (map.get(m.getUniqueName()) != null)
						{
							System.err.println("WARNING: Multiple behaviors"
							                   + " called " + m.toString());
						}
						else
						{
							map.put(m.getUniqueName(), m);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}

				// Perform substitutions and verify cycles
				for(int i = 0; i < a.size(); i++)
				{
					//System.err.println("Substituting " + a.get(i));
					if (!((TrainableMacro)(a.get(i))).performSubstitution(map, loaded,
					        failed, new HashSet()))  // failed
					{
						a.remove(i);
						i--;  // try again
					}
				}
				// Clone
				try
				{
					for(int i = 0; i < a.size(); i++)
						a.set(i, (TrainableMacro)(a.get(i).clone()));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		TrainableMacro[] tm = new TrainableMacro[a.size()];
		System.arraycopy(a.toArray(), 0, tm, 0, a.size());
		return tm;
	}

	boolean performSubstitution(HashMap map, HashSet loaded, HashSet failed, HashSet cycles)
	{
		System.err.println("Performing Substitution for " + this);

		if (cycles.contains(this))  // uh oh, cycles
		{
			System.err.println("WARNING: Cycles in loading TrainableMacro " + this);
			failed.add(this);
			return false;
		}

		cycles.add(this);
		for(int j = 0; j < behaviorNames.length; j++)
		{
			if (behaviorNames[j] != null)
			{
				Behavior b = (Behavior)(map.get(behaviorNames[j]));
				if (b == null)
				{
					System.err.println("WARNING: Cannot load TrainableMacro " + this
					                   + " because it expected an unknown behavior called "
					                   + behaviorNames[j]);
					failed.add(this);
					cycles.remove(this);
					return false;
				}
				else
				{
					if (b instanceof TrainableMacro)
					{
						if (failed.contains(b) ||
						        (!loaded.contains(b) &&
						         !((TrainableMacro)b).performSubstitution(map, loaded,
						                 failed, cycles)))  // recurse
						{
							System.err.println("WARNING: Cannot load "
							                   + "TrainableMacro " + this
							                   + " because it expected a "
							                   + "TrainableMacro called "
							                   + behaviorNames[j]
							                   + " which failed to load.");
							failed.add(this);
							cycles.remove(this);
							return false;
						}
						// else fall thru below
					}

					// FALL THRU
					behaviors[j] = (Behavior)(b.clone());  // clone the behavior

					// copy over the targets
					if (behaviorTargets[j] == null) // uh oh
					{
						System.err.println("WARNING: Cannot load"
						                   + " TrainableMacro " + this
						                   + "because it expected a "
						                   + "behavior called "
						                   + behaviorNames[j]
						                   + " but is missing targets for it.");
						failed.add(this);
						cycles.remove(this);
						return false;
					}
					else if (behaviors[j].getNumTargets() !=
					         behaviorTargets[j].length) // uh oh
					{
						System.err.println("WARNING: Cannot load"
						                   + " TrainableMacro " + this
						                   + "because it expected a behavior called "
						                   + behaviorNames[j] + " with "
						                   + behaviorTargets[j].length
						                   + " targets, but it got "
						                   + behaviors[j].getNumTargets() + " instead.");
						failed.add(this);
						cycles.remove(this);
						return false;
					}
					else
					{
						behaviors[j].setTargets(behaviorTargets[j]);
					}

					// now convert any parameters to wrappers
					for(int k = 0; k < behaviors[j].getNumTargets(); k++)
					{
						Target t = ((Target)(behaviors[j].getTarget(k)));
						// because it was unused when saved.  See save()
						if (t != null && t instanceof Parameter)
							// needs to be rebound. Assume that the index of horde
							// is the same as the index for this macro
						{
							Parameter p = (Parameter) t;
							// System.err.println("     Substituting Wrapper for"
							// + "behavior parameter "
							// + behaviors[j] + " : " + k + " " + p);
							// we'll use p.getIndex() as the default but it
							// might not be a good choice
							behaviors[j].setTarget(k, new Wrapper(p.getName(),
							                                      p.getIndex()));
						}
						//else if (t != null)
						//    System.err.println("Non-substituted behavior target "
						//    + behaviors[j] + " : " + k + " " + t);
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
		for(int i = 0 ; i < features.length; i++)
			if (features[i] != null)
				s += features[i].writeAllTargetsHelp(targetsSoFar) ;
		for(int i = 0 ; i < behaviors.length; i++)
			if (behaviors[i] != null)
				s += behaviors[i].writeAllTargetsHelp(targetsSoFar) ;
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
		for(int i = 0 ; i < behaviors.length; i++)
			if (behaviors[i] != null && behaviors[i] instanceof TrainableMacro)
				str += ((TrainableMacro)(behaviors[i])).
				       writeAllFeaturesHelp(featuresSoFar);
		for(int i = 0 ; i < features.length; i++)
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
		for(int i = 0 ; i < behaviors.length; i++)
			if (behaviors[i] instanceof TrainableMacro)
				fsacount++;
		writer.print("" + fsacount + " ");
		for(int i = 0 ; i < behaviors.length; i++)
			writer.print(" " + behaviors[i].getName());
		writer.print(" )\n\n");

		// write out all behaviors
		HashSet behaviorsSoFar = new HashSet();
		for(int i = 0 ; i < behaviors.length; i++)
			if (behaviors[i] != null)
			{
				writer.print("%%% ----- " + i + " ------\n");
				behaviors[i].write(writer, behaviorsSoFar);
			}

		// we don't write out the top-level behavior

		/*
		// write out the global classifier with behavior map,
		// feature map, and learned transitions
		writer.print(" ( behaviors " + behaviors.length + " ");
		for(int i = 0 ; i < behaviors.length; i++)
		if (behaviors[i] != null)
		writer.print(behaviors[i].name + " ");
		else writer.print(" null ");
		writer.print(")\n");

		writer.print(" ( features " + features.length + " ");
		for(int i = 0 ; i < features.length; i++)
		features[i].write(writer);
		writer.print(")\n");

		writer.print(" ( transitions\n");
		for(int i = 0 ; i < behaviors.length; i++)
		if (transitions[i] != null)
		transitions[i].write(writer, false);
		else writer.print(" null ");
		writer.print(")\n");
		*/
	}

	public void write(PrintWriter writer, HashSet behaviorsSoFar)
	{
		// write out the subsidiary behaviors FIRST
		//for(int i = 0 ; i < behaviors.length; i++)
		//    if (behaviors[i] != null)
		//        behaviors[i].write(writer, behaviorsSoFar);

		// Now write me
		writer.print(" ( trainable-macro " + name + " " + level + " ");

		writer.print(" ( targets " + targets.length + " ");
		for(int i = 0 ; i < targets.length; i++)
			writer.print(targetNames[i] + " ");
		writer.print(")\n");

		writer.print(" ( behaviors " + behaviors.length + " ");
		for(int i = 0 ; i < behaviors.length; i++)
			if (behaviors[i] != null)
				writer.print(behaviors[i].name + " ");
			else writer.print(" null ");
		writer.print(")\n");

		writer.print(" ( features " + features.length + " ");
		for(int i = 0 ; i < features.length; i++)
			features[i].write(writer);
		writer.print(")\n");

		domain.write(writer);

		writer.print(" ( transitions\n");
		for(int i = 0 ; i < behaviors.length; i++)
			if (transitions[i] != null)
				transitions[i].write(writer, false);
			else writer.print(" null ");
		writer.print(")\n");

		writer.print(")\n\n");
	}
}
