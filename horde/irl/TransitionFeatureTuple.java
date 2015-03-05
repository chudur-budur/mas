package sim.app.horde.irl ;

import java.util.*;

public class TransitionFeatureTuple
{
	// We do not care about the continuation for the time being
	private int startBehaviorIndex ;
	private String startBehaviorName ;
	private int endBehaviorIndex ;
	private String endBehaviorName ;
	private double[] featureValues ;
	private boolean continuation ;

	public TransitionFeatureTuple(int sindex, String sName, int eindex, String eName,
	                              double[] vals, boolean cont)
	{
		this.startBehaviorIndex  = sindex ;
		this.startBehaviorName = sName ;
		this.endBehaviorIndex = eindex ;
		this.endBehaviorName = eName ;
		this.featureValues = vals ;
		this.continuation = cont ;
	}

	// unique hash code
	public int hashCode()
	{
		return Arrays.hashCode(featureValues)^startBehaviorIndex^endBehaviorIndex;
	}

	// comparator
	public boolean equals(Object o)
	{
		if (!(o instanceof TransitionFeatureTuple))
			return false ;
		TransitionFeatureTuple e = (TransitionFeatureTuple) o;
		if ((e.startBehaviorIndex != this.startBehaviorIndex) ||
		        (e.endBehaviorIndex != this.endBehaviorIndex))
			return false ;
		else
			return true ;
	}

	// stringize
	public String toString()
	{
		String str = String.format("%-20s\t",
		                           this.startBehaviorIndex
		                           + "-" + this.startBehaviorName);
		for(int i = 0 ; i < this.featureValues.length ; i++)
			str += String.format("%.5f", featureValues[i]) + "\t";
		str += String.format("%20s\t",
		                     this.endBehaviorIndex
		                     + "-" + this.endBehaviorName)
		       + " " + this.continuation;
		return str ;
	}

	// getters
	public int getStartBehaviorIndex()
	{
		return this.startBehaviorIndex ;
	}
	public String getStartBehaviorName()
	{
		return this.startBehaviorName ;
	}
	public int getEndBehaviorIndex()
	{
		return this.endBehaviorIndex ;
	}
	public String getEndBehaviorName()
	{
		return this.endBehaviorName ;
	}
	public double[] getFeatureValues()
	{
		double[] val = new double[this.featureValues.length] ;
		System.arraycopy(this.featureValues, 0, val, 0, this.featureValues.length);
		return val ;
	}
	public boolean isContinuation()
	{
		return this.continuation ;
	}

	// setters
	public void setStartBehaviorIndex(int index)
	{
		this.startBehaviorIndex = index ;
	}
	public void setStartBehaviorName(String name)
	{
		this.startBehaviorName = name ;
	}
	public void setEndBehaviorIndex(int index)
	{
		this.endBehaviorIndex = index ;
	}
	public void setEndBehaviorName(String name)
	{
		this.endBehaviorName = name ;
	}
	public void setFeatureValues(double[] vals)
	{
		this.featureValues = vals ;
	}
	public void setContinuation(boolean val)
	{
		this.continuation = val ;
	}

	public TransitionFeatureTuple clone()
	{
		double[] vals = this.getFeatureValues();
		TransitionFeatureTuple t = new TransitionFeatureTuple(
			this.startBehaviorIndex, 
			this.startBehaviorName,
			this.endBehaviorIndex,
			this.endBehaviorName,
			vals,
			this.continuation);
		return t ;
	}
}
