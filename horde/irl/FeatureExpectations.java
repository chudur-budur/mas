package sim.app.horde.irl ;

import java.util.* ;

public class FeatureExpectations
{
	private HashMap<String, Vector<Double>> featureExpectations = null ;
	private HashMap<String, ArrayList<Pair<String, Vector<Double>>>> featureExpectationsCluster = null ;

	public FeatureExpectations()
	{
		this.featureExpectations
			= new HashMap<String, Vector<Double>>();
		this.featureExpectationsCluster 
			= new HashMap<String, ArrayList<Pair<String, Vector<Double>>>>();
	}

	// getters
	public double[] getFeatureExpectationValues(String key)
	{
		if(this.featureExpectations.containsKey(key))
		{
			Object[] d = this.featureExpectations.get(key).toArray();
			double[] ret = new double[d.length] ;
			for(int i = 0 ; i < ret.length ; i++)
				ret[i] = ((Double)d[i]).doubleValue();
			return ret ;
		}
		else
			return null ;
	}

	public double[] getFeatureExpectationValues(String key, String subkey)
	{
		if(this.featureExpectationsCluster.containsKey(key))
		{
			Object[] vec = null ;
			for(Pair<String, Vector<Double>> p : 
					this.featureExpectationsCluster.get(key))
			{
				if(subkey.equals(p.x))
				{
					vec = p.y.toArray() ;
					break ;
				}
			}
			double[] ret = new double[vec.length] ;
			for(int i = 0 ; i < ret.length ; i++)
				ret[i] = ((Double)vec[i]).doubleValue();
			return ret ;
		}
		else
			return null ;
	}

	// compute the feature expectations
	public void computeFrom(ExpertDemo demo)
	{
		for(String key : demo.keySet())
		{
			ArrayList<TransitionFeatureTuple> lst = demo.get(key);
			double[] mu = new double[lst.get(0).getFeatureValues().length];
			for(TransitionFeatureTuple t : lst)
			{
				double[] val = t.getFeatureValues();
				for(int i = 0 ; i < val.length ; i++)
					mu[i] += val[i] ;
			}
			Vector<Double> v = new Vector<Double>();
			int len = lst.size();
			for(int i = 0 ; i < mu.length ; i++)
			{
				mu[i] = mu[i] / len ;
				v.add(new Double(mu[i]));
			}
			this.featureExpectations.put(key, v);
		}
	}

	public void computeWithKmeans(ExpertDemo demo, int k)
	{
		// for each transition
		for(String key : demo.keySet())
		{
			// feature data for the current transition
			ArrayList<TransitionFeatureTuple> lst = demo.get(key);
			// put them in a 2D array
			double points[][] =
				new double[lst.size()][lst.get(0).getFeatureValues().length] ;
			for(int i = 0 ; i < lst.size() ; i++)
				points[i] = lst.get(i).getFeatureValues();
			
			// randomly pick 3 points from them as intial centroids
			double[][] centroids = null ;
			if(lst.size() > k )
				centroids = new double[k][points[0].length] ;
			else
				centroids = new double[1][points[0].length] ;
			int min = 0 ;
			int max = points.length - 1;
			for(int i = 0 ; i < centroids.length ; i++)
			{
				int index = min + (int)(Math.random() * ((max - min) + 1)); 
				System.arraycopy(points[index], 0, centroids[i], 0, 
						points[index].length) ; 
			}

			KmeansClustering kmc = new KmeansClustering(centroids, points);
			kmc.setIteration(256);
			kmc.setDistanceFunction(KmeansClustering.EUCLIDEAN_DISTANCE_FUNCTION);
			kmc.compute();
			centroids = kmc.getCentroids();
			ArrayList<Pair<String, Vector<Double>>> alst = 
				new ArrayList<Pair<String, Vector<Double>>>();
			if(centroids.length == k)
			{
				for(int i = 0 ; i < centroids.length ; i++)
				{
					Vector<Double> vec = new Vector<Double>();
					for(int j = 0 ; j < centroids[i].length ; j++)
						vec.add(new Double(centroids[i][j]));
					alst.add(new Pair<String, Vector<Double>>("" + i, vec));
				}
			}
			else
			{
				for(int i = 0 ; i < k ; i++)
				{
					Vector<Double> vec = new Vector<Double>();
					for(int j = 0 ; j < centroids[0].length ; j++)
						vec.add(new Double(centroids[0][j]));
					alst.add(new Pair<String, Vector<Double>>("" + i, vec));
				}
			}
			this.featureExpectationsCluster.put(key, alst);
		}
	}

	public String classify(String transition, TransitionFeatureTuple tft)
	{
		String classification = "" ;
		if(this.featureExpectationsCluster != null)
		{
			ArrayList<Pair<String, Vector<Double>>> lst 
				= this.featureExpectationsCluster.get(transition);
			double[] fvals = tft.getFeatureValues();
			double dist = Double.MAX_VALUE ;
			for(Pair<String, Vector<Double>> p : lst)
			{
				double[] muE = new double[p.y.size()];
				for(int i = 0 ; i < muE.length ; i++) 
					muE[i] = p.y.get(i).doubleValue();
				double d = this.getEuclideanDistance(fvals, muE);
				if(d < dist)
				{
					dist =  d;
					classification = p.x ;
				}
			}
		}
		return classification ;
	}

	private double getEuclideanDistance(double[] x, double[] y)
	{
		double dist = 0.0 ;
		for(int i = 0 ; i < x.length ; i++)
			dist += (x[i] - y[i]) * (x[i] - y[i]);
		return Math.sqrt(dist);
	}

	// stringize
	public String toString()
	{
		String str = "" ;
		if(this.featureExpectations != null)
		{
			Object[] keys = this.featureExpectations.keySet().toArray();
			for(int i = 0 ; i < keys.length ; i++)
			{
				str += (String)keys[i] + ": " ;
				str += this.featureExpectations.get((String)keys[i]).toString() ;
				if(i < keys.length - 1)
					str += "\n" ;
			}
		}
		if(this.featureExpectationsCluster != null)
		{
			Object[] keys = this.featureExpectationsCluster.keySet().toArray();
			for(int i = 0 ; i < keys.length ; i++)
			{
				str += (String)keys[i] + ": \n" ;
				ArrayList<Pair<String, Vector<Double>>> lst 
					= this.featureExpectationsCluster.get((String)keys[i]);
				for(Pair<String, Vector<Double>> p : lst)
					str += p.toString() + "\n";
				if(i < keys.length - 1)
					str += "\n" ;
			}
		}
		return str ;
	}

	// deep copy
	public FeatureExpectations clone()
	{
		FeatureExpectations fe = null ;
		if(this.featureExpectations != null)
		{
			fe = new FeatureExpectations();
			HashMap<String, Vector<Double>> hm =
			    new HashMap<String, Vector<Double>>();
			for(String key : this.featureExpectations.keySet())
			{
				Vector<Double> vec = new Vector<Double>();
				for(Double d : this.featureExpectations.get(key))
					vec.add(new Double(d.doubleValue()));
				hm.put(key, vec);
			}
			fe.featureExpectations = hm ;
		}
		if(this.featureExpectationsCluster != null)
		{
			fe = new FeatureExpectations();
			HashMap<String, ArrayList<Pair<String, Vector<Double>>>> hm = 
				new HashMap<String, ArrayList<Pair<String, Vector<Double>>>>();
			for(String key : this.featureExpectationsCluster.keySet())
			{
				ArrayList<Pair<String, Vector<Double>>> lst 
					= new ArrayList<Pair<String, Vector<Double>>>();
				for(Pair<String, Vector<Double>> p : 
						this.featureExpectationsCluster.get(key))
				{
					Vector<Double> vec = new Vector<Double>();
					for(Double d : p.y)
						vec.add(new Double(d.doubleValue()));
					lst.add(new Pair<String, Vector<Double>>(p.x, vec));
				}
				hm.put(key, lst);
			}
			fe.featureExpectationsCluster = hm ;
		}
		// need to clone the clustered one.
		return fe ;
	}

	public static void main(String[] args)
	{
		/*ExpertDemo demo = new ExpertDemo();
		demo.loadExpertDemo(0);
		FeatureExpectations fe = new FeatureExpectations();
		fe.compute(demo);
		System.err.println("Demo 0: \n" + demo.toString());
		System.err.println("Demo 0 mu: \n" + fe.toString());
		double[] vals = fe.getFeatureExpectationValues("3-3");
		for(int i = 0 ; i < vals.length ; i++)
			System.err.print(vals[i] + " ");
		System.err.println("\n");*/

		ExpertDemo[] demo = new ExpertDemo[ExpertDemo.DEMO_COUNT];
		for(int i = 0 ; i < ExpertDemo.DEMO_COUNT ; i++)
		{
			demo[i] = new ExpertDemo();
			demo[i].loadExpertDemo(i);
		}

		ExpertDemo[] validationDemo = new ExpertDemo[demo.length/2] ;
		for(int i = 0 ; i < validationDemo.length ; i++)
			validationDemo[i] = demo[i + validationDemo.length].clone();
		ExpertDemo validationMerged = ExpertDemo.mergeDemos(validationDemo);
	
		FeatureExpectations fe = new FeatureExpectations();
		//fe.computeFrom(validationMerged);
		fe.computeWithKmeans(validationMerged, 3);

		for(int i = 0 ; i < demo.length/2 ; i++)
			System.err.println("demo-" + i + ": \n" + demo[i].toString());
		System.err.println("validation FE: \n" + fe.toString());

		TransitionFeatureTuple tft = demo[0].get("2-2").get(10);
		System.err.println("tft: " + tft.toString());
		System.err.println("classify: " + fe.classify("2-2", tft));
	}
}


class Pair<X, Y> 
{ 
	public final X x; 
	public final Y y; 
	
	public Pair(X x, Y y) 
	{ 
		this.x = x; 
		this.y = y; 
	} 

	public String toString()
	{
		return this.x.toString() + " # " + this.y.toString();
	}	
} 
