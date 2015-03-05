package sim.app.horde.irl ;

import java.util.* ;
import sim.app.horde.* ;
import sim.app.horde.optimization.cobyla.*;

/**
 * This class does the follow --
 * 	a) Load the transition-feature-tuples from N expert demos
 * 	b) Divide them into two equal size sets, Dl and Dv
 * 	d) Merge all feature values in Dv w,r,t individual transitions
 * 	e) Now our target is to find the feature weights for Dl w,r,t Dv
 * 	f) Pick one transition from Dl, say t1; consider all feature values under t1 as fl
 * 	g) Pick the same feature values under transition t1 from Dv, say the feature sets are fv
 * 	h) Apply k-means clustering on fv, divide them into clusters fv1, fv2, ,,, fvn etc
 * 	i) Classify fl using the cluster means of fvi's, now say fl is clustered into fl1, fl2, ,,, fln etc
 * 	j) Apply NLP to find the weights for fli from corresponding flv's, 
 * 	   you will have multiple set of weights wi for single fli, consider the mean of wi as the final 
 * 	   weight that captures the relation between fli and flv  
 */
public class RewardFunction
{
	public static int QCLP_MAX = 0 ;
	public static int QCLP_MEAN = 1 ;

	private double[][][] rMatrix = null ;

	// this is for debugging and toString()
	private HashMap<String, ArrayList<Vector<Double>>> rf = null ;

	public RewardFunction()
	{
		;
	}

	public double getReward(int r, int c, final double[] fvals)
	{
		// no, reward is r = ∑i w[i] * fvals[i]
		double sum = 0.0 ;
		for(int i = 0 ; i < this.rMatrix[r][c].length ; i++)
			sum += fvals[i] * this.rMatrix[r][c][i];
		return sum ;	
	}

	public int getNumRows() 
	{ 
		if(this.rMatrix != null) 
			return this.rMatrix.length ;
		else
			return -1 ;
	}

	public int getNumCols()
	{
		if(this.rMatrix != null) 
			return this.rMatrix[0].length ;
		else
			return -1 ;
	}
	private double[][] transpose(final double[][] matrix)
	{
		double[][] trans = new double[matrix[0].length][matrix.length];
		for(int i = 0 ; i < matrix.length ; i++)
			for(int j = 0 ; j < matrix[0].length ; j++)
				trans[j][i] = matrix[i][j] ;
		return trans ;
	}

	public void computeWithQclpKmeans(ExpertDemo[] demo, int K, int option)
	{
		ExpertDemo[] validationDemo = new ExpertDemo[demo.length/2] ;
		for(int i = 0 ; i < validationDemo.length ; i++)
			validationDemo[i] = demo[i + validationDemo.length].clone();
		ExpertDemo demosMerged = ExpertDemo.mergeDemos(validationDemo);
		// System.err.println("demosMerged:\n" + demosMerged.toString());
		FeatureExpectations fe = new FeatureExpectations();
		fe.computeWithKmeans(demosMerged, K);
		
		HashMap<String, ArrayList<Vector<Double>>> rewardValues
		= new HashMap<String, ArrayList<Vector<Double>>>();
		
		// go over each demo
		for(int i = 0 ; i < demo.length/2 ; i++)
		{
			// go over each transition
			for(String key : demo[i].keySet())
			{
				// go through each trajectory in the list and classify
				HashMap<String, ArrayList<TransitionFeatureTuple>> cluster
					= new HashMap<String, ArrayList<TransitionFeatureTuple>>();
				for(TransitionFeatureTuple t : demo[i].get(key))
				{
					String clusterKey = key + "-" + fe.classify(key, t) ;
					if(!cluster.containsKey(clusterKey))
					{
						ArrayList<TransitionFeatureTuple> alst = 
							new ArrayList<TransitionFeatureTuple>();
						alst.add(t);
						cluster.put(clusterKey, alst);
					}
					else
					{
						ArrayList<TransitionFeatureTuple> alst =
							cluster.get(clusterKey);
						alst.add(t);
						cluster.put(clusterKey, alst);
					}
				}// for-3
				// after this point, we have a cluster.
				/*String str = "" ;
				for(String k : cluster.keySet())
				{
					str += k + ": \n" ;
					for(TransitionFeatureTuple t : cluster.get(k))
						str += t.toString() + "\n" ;
				}//for-4
				System.err.println(str);*/

				// now for each cluster
				for(String subkey : cluster.keySet())
				{
					ArrayList<TransitionFeatureTuple> tlst = 
						cluster.get(subkey) ;
					double[][] fvals = new double[tlst.size()][ExpertDemo.FEATURE_COUNT] ;
					for(int j = 0 ; j < tlst.size() ; j++)
						fvals[j] = ((TransitionFeatureTuple)tlst.get(j)).
					           getFeatureValues();
					fvals = this.transpose(fvals);
					double[] fexp = fe.getFeatureExpectationValues(key, subkey.split("-")[2]);

					/*System.err.print("fvals: \n");
					for(int j = 0 ; j < fvals.length ; j++)
					{
						for(int k = 0 ; k < fvals[j].length ; k++)
							System.err.print(fvals[j][k] + " ");
						if( j < fvals.length - 1)
							System.err.print("\n");
					}
					System.err.print("\nfexp: ");
					for(int j = 0 ; j < fexp.length ; j++)
						System.err.print(fexp[j] + " ");
					System.err.print("\n\n");*/
					
					System.err.println("--- RewardFunction.computeWithQclpKMeans() :" 
							+ " computing on demo " + i);
					Vector<Double> weights = this.solveQCLP(fvals, fexp);
					if(rewardValues.containsKey(subkey))
					{
						ArrayList<Vector<Double>> lst = rewardValues.get(subkey);
						lst.add(weights);
						rewardValues.put(subkey, lst);
					}
					else
					{
						ArrayList<Vector<Double>> lst = new ArrayList<Vector<Double>>();
						lst.add(weights);
						rewardValues.put(subkey, lst);
					}
				}//for-5

			}// for-2
		}// for-1
		
		/*String str = "" ;	
		for(String key : rewardValues.keySet())
		{
			str += key + ": \n";
			for(Vector<Double> vec : rewardValues.get(key))
				str += vec.toString() + "\n" ;	
		}
		System.err.println(str);*/
		
		this.rMatrix = new double[ExpertDemo.BEHAVIOUR_COUNT]
		[ExpertDemo.BEHAVIOUR_COUNT * K]
		[ExpertDemo.FEATURE_COUNT] ;
		// * --> Start is not allowed by default
		double[] notAllowed = new double[ExpertDemo.FEATURE_COUNT];
		for(int i = 0 ; i < notAllowed.length ; i++) notAllowed[i] = -1.0 ;
		for(int i = 0 ; i < rMatrix.length ; i++)
			for(int k = 0 ; k < K ; k++)
				this.rMatrix[i][k] = notAllowed ;
		if(option == QCLP_MAX)
			this.populateRewardValuesWithMax(rewardValues, K);
		else if(option == QCLP_MEAN)
			this.populateRewardValuesWithMean(rewardValues, K);
		this.rf = rewardValues ;
	}

	public void computeWithQCLP(ExpertDemo[] demo, int option)
	{
		ExpertDemo[] validationDemo = new ExpertDemo[demo.length/2] ;
		for(int i = 0 ; i < validationDemo.length ; i++)
			validationDemo[i] = demo[i + validationDemo.length].clone();
		ExpertDemo demosMerged = ExpertDemo.mergeDemos(validationDemo);
		// System.err.println("demosMerged:\n" + demosMerged.toString());
		FeatureExpectations fe = new FeatureExpectations();
		fe.computeFrom(demosMerged);
		// System.err.println("fe:\n" + fe.toString());

		HashMap<String, ArrayList<Vector<Double>>> rewardValues
		= new HashMap<String, ArrayList<Vector<Double>>>();

		for(int i = 0 ; i < demo.length/2 ; i++)
		{
			for (String key : demo[i].keySet())
			{
				ArrayList<TransitionFeatureTuple> tlst
								= demo[i].get(key);
				double[][] fvals = new double[tlst.size()][ExpertDemo.FEATURE_COUNT] ;
				for(int j = 0 ; j < tlst.size() ; j++)
					fvals[j] = ((TransitionFeatureTuple)tlst.get(j)).
					           getFeatureValues();
				fvals = this.transpose(fvals);
				double[] fexp = fe.getFeatureExpectationValues(key);

				/*for(TransitionFeatureTuple tft : tlst)
					System.err.println(tft.toString());
				System.err.print("fvals: \n");
				for(int j = 0 ; j < fvals.length ; j++)
				{
					for(int k = 0 ; k < fvals[j].length ; k++)
						System.err.print(fvals[j][k] + " ");
					if( j < fvals.length - 1)
						System.err.print("\n");
				}
				System.err.print("\nfexp: ");
				for(int j = 0 ; j < fexp.length ; j++)
					System.err.print(fexp[j] + " ");
				System.err.print("\n\n");*/

				Vector<Double> weights = this.solveQCLP(fvals, fexp);
				if(rewardValues.containsKey(key))
				{
					ArrayList<Vector<Double>> lst = rewardValues.get(key);
					lst.add(weights);
					rewardValues.put(key, lst);
				}
				else
				{
					ArrayList<Vector<Double>> lst = new ArrayList<Vector<Double>>();
					lst.add(weights);
					rewardValues.put(key, lst);
				}
			}
		}

		this.rMatrix = new double[ExpertDemo.BEHAVIOUR_COUNT]
		[ExpertDemo.BEHAVIOUR_COUNT]
		[ExpertDemo.FEATURE_COUNT] ;
		// * --> Start is not allowed by default
		double[] notAllowed = new double[ExpertDemo.FEATURE_COUNT];
		for(int i = 0 ; i < notAllowed.length ; i++) notAllowed[i] = -1.0 ;
		for(int i = 0 ; i < rMatrix.length ; i++)
			this.rMatrix[i][0] = notAllowed ;

		if(option == QCLP_MAX)
			this.populateRewardValuesWithMax(rewardValues, 0);
		else if(option == QCLP_MEAN)
			this.populateRewardValuesWithMean(rewardValues, 0);
		this.rf = rewardValues ;
	}

	private Vector<Double> solveQCLP(final double[][] fvals, final double[] muE)
	{
		double[] w = new double[fvals.length] ;
		
		double rhobeg = 0.5 ;
		double rhoend = 1.0e-6 ;
		int iprint = 3 ;
		int maxfun = 3500 ;
		int numConst = 2;
		if(muE == null)
			for(int i = 0 ; i < w.length ; i++) w[i] = 0.0 ;
		else
		{
			for(int f = 0 ; f < fvals.length ; f++)
			{
				// Solve this QCLP (using COBYLA2 algorithm) --
				// 	max w[i] min w[i] * (muE[i] - fvals[i][j])
				// 	s.t. ∀i 0.0 < \w[i]\² < 1.0
				final double[] x = new double[fvals[f].length] ;
				for(int i = 0 ; i < x.length ; i++) x[i] = Math.random() ;
				final int _f = f ;
				Calcfc objective = new Calcfc()
				{
					public double Compute(int n, int m, double[] x, double[] con)
					{
						double sum = 0.0 ;
						for(int i = 0 ; i < n ; i++) sum+= x[i] * x[i] ;
						sum = Math.sqrt(sum);
						con[0] = 1.0 - sum;
						con[1] = 0.0001 - sum ;
						sum = 0.0 ;
						for(int i = 0 ; i < n ; i++)
							sum += x[i] * (muE[_f] - fvals[_f][i]);
						return sum ;
					}
				};
				CobylaExitStatus result = Cobyla.FindMinimum(objective, x.length, 
						numConst, x, rhobeg, rhoend, iprint, maxfun);
				// max w[i]
				w[f] = x[0] ;
				for(int i = 1 ; i < x.length ; i++)
					if(x[i] >= w[f])
						w[f] = x[i] ;
			}
		}

		Vector<Double> vec = new Vector<Double>();
		for(int i = 0 ; i < w.length ; i++)
			vec.add(new Double(w[i]));
		return vec ;
	}

	private int mapColIndex(int key, int subkey, int K)
	{
		if(K == 0)
			return key ;
		else
			return key * K + subkey ;	
	}

	private void populateRewardValuesWithMax(
	    final HashMap<String, ArrayList<Vector<Double>>> rewardValues, int K)
	{
		for(String key : rewardValues.keySet())
		{
			ArrayList<Vector<Double>> lst = rewardValues.get(key);
			Vector<Double> first = lst.get(0) ;
			double[] maxVal = new double[ExpertDemo.FEATURE_COUNT] ;
			for(int i = 0 ; i < first.size() ; i++)
				maxVal[i] = first.get(i).doubleValue();
			for(Vector<Double> vec : lst)
			{
				for(int i = 0 ; i < ExpertDemo.FEATURE_COUNT ; i++)
				{
					Double val = vec.get(i);
					double dval = val.doubleValue();
					if(dval >= maxVal[i])
					{
						//if(dval < 0) // no, wrong !
						//	dval = 0.0;
						maxVal[i] = dval ;
					}
				}
			}
			int rindex = Integer.parseInt(key.split("-")[0]);
			int cindex = 0 ;
			if(K == 0)
				cindex = Integer.parseInt(key.split("-")[1]);
			else
				cindex = mapColIndex(Integer.parseInt(key.split("-")[1]),
					       Integer.parseInt(key.split("-")[2]), K);	
			rMatrix[rindex][cindex] = maxVal ;
		}
	}

	private void populateRewardValuesWithMean(
	    final HashMap<String, ArrayList<Vector<Double>>> rewardValues, int K)
	{
		for(String key : rewardValues.keySet())
		{
			ArrayList<Vector<Double>> lst = rewardValues.get(key);
			double[] meanVal = new double[ExpertDemo.FEATURE_COUNT];
			for(Vector<Double> vec : lst)
			{
				for(int i = 0 ; i < vec.size() ; i++)
				{
					double dval = vec.get(i).doubleValue();
					//if(dval < 0) // no wrong !
					//	dval = 0.0 ;
					meanVal[i] += dval;
				}
			}
			for(int i = 0 ; i < ExpertDemo.FEATURE_COUNT ; i++)
				meanVal[i] = meanVal[i] / lst.size();
			int rindex = Integer.parseInt(key.split("-")[0]);
			int cindex = 0 ;
			if(K == 0)
				cindex = Integer.parseInt(key.split("-")[1]);
			else
				cindex = mapColIndex(Integer.parseInt(key.split("-")[1]),
					       Integer.parseInt(key.split("-")[2]), K);	
			rMatrix[rindex][cindex] = meanVal ;
		}
	}

	public String toString(boolean val)
	{
		String str = "";
		if(this.rf != null && val)
		{
			for(String key : this.rf.keySet())
			{
				str += key + ":\n" ;
				ArrayList<Vector<Double>> lst = this.rf.get(key);
				for(Vector<Double> v : lst)
					str += v.toString() + "\n";
				str += "\n";
			}
		}
		if(this.rMatrix != null)
		{
			for(int i = 0 ; i < this.rMatrix.length ; i++)
			{
				for(int j = 0 ; j < this.rMatrix[i].length ; j++)
				{
					for(int k = 0 ; k < this.rMatrix[i][j].length ; k++)
					{
						if(k == 0)
							str += "[" + String.format("%+.2f",
							                           this.rMatrix[i][j][k])
							       + "," ;
						else if(k == this.rMatrix[i][j].length - 1 )
							str += String.format("%+.2f",
							                     this.rMatrix[i][j][k]) + "]" ;
						else
							str += String.format("%+.2f",
							                     this.rMatrix[i][j][k]) + "," ;
					}
					str += " " ;
				}
				str += "\n";
			}
		}
		return str ;
	}

	// shallow copy
	public RewardFunction clone()
	{
		RewardFunction rf = null ;
		if(this.rMatrix != null)
		{
			rf = new RewardFunction();
			rf.rMatrix = this.rMatrix ;
		}
		return rf ;
	}

	// tester function
	public static void main(String args[])
	{
		int N = 16 ;
		ExpertDemo[] demo = new ExpertDemo[N];
		for(int i = 0 ; i < demo.length ; i++)
		{
			demo[i] = new ExpertDemo();
			demo[i].loadExpertDemo(i);
			// System.err.println("demo[" + i + "]:\n" + demo[i].toString());
		}
		RewardFunction rf = new RewardFunction();
		//rf.computeWithQCLP(demo, RewardFunction.QCLP_MAX);
		//System.err.println("------ MAX ------\n" + rf.toString(true));
		//rf.computeWithQCLP(demo, RewardFunction.QCLP_MEAN);
		//System.err.println("------ MEAN ------\n" + rf.toString(true));
		rf.computeWithQclpKmeans(demo, 3, 0);
		System.err.println("------ MEAN ------\n" + rf.toString(true));
	}
}
