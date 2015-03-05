package sim.app.horde.irl;

import java.util.*;

public class KmeansClustering
{
	public static interface Listener
	{
		void iteration(int iteration, int move);
	}

	public static interface DistanceFunction
	{
		double distance(double[] p1, double[] p2);
	}

	public static final DistanceFunction EUCLIDEAN_DISTANCE_FUNCTION = new DistanceFunction()
	{
		@Override
		public double distance(double[] p1, double[] p2)
		{
			double s = 0;
			for (int d = 0; d < p1.length; d++)
			{
				s += Math.pow(Math.abs(p1[d] - p2[d]), 2);
			}
			double d = Math.sqrt(s);
			return d;
		}
	};

	public static final DistanceFunction MANHATTAN_DISTANCE_FUNCTION = new DistanceFunction()
	{
		@Override
		public double distance(double[] p1, double[] p2)
		{
			double s = 0;
			for (int d = 0; d < p1.length; d++)
			{
				s += Math.abs(p1[d] - p2[d]);
			}
			return s;
		}
	};

	protected double[][] centroids;
	protected double[][] points;
	protected int idealCount;
	protected double[][] distances;
	protected int[] assignments;
	protected boolean[] changes;
	protected int[] counts;
	protected boolean[] dones;
	protected int iteration;
	protected boolean equal;
	protected DistanceFunction distanceFunction;
	protected Listener listener;

	public KmeansClustering(double[][] centroids, double[][] points)
	{
		this.centroids = centroids;
		this.points = points;
		if (centroids.length > 0)
			idealCount = points.length / centroids.length;
		else
			idealCount = 0;

		distances = new double[centroids.length][points.length];
		assignments = new int[points.length];
		Arrays.fill(assignments, -1);
		changes = new boolean[centroids.length];
		Arrays.fill(changes, true);
		counts = new int[centroids.length];
		dones = new boolean[centroids.length];
		iteration = 128;
		equal = false;
		distanceFunction = EUCLIDEAN_DISTANCE_FUNCTION;
		listener = null;
	}

	public double[][] getCentroids()
	{
		return centroids;
	}

	public double[][] getPoints()
	{
		return points;
	}

	public double[][] getDistances()
	{
		return distances;
	}

	public int[] getAssignments()
	{
		return assignments;
	}

	public boolean[] getChanges()
	{
		return changes;
	}

	public int[] getCounts()
	{
		return counts;
	}

	public int getIteration()
	{
		return iteration;
	}

	public void setIteration(int iteration)
	{
		this.iteration = iteration;
	}

	public boolean isEqual()
	{
		return equal;
	}

	public void setEqual(boolean equal)
	{
		this.equal = equal;
	}

	public DistanceFunction getDistanceFunction()
	{
		return distanceFunction;
	}

	public void setDistanceFunction(DistanceFunction distanceFunction)
	{
		this.distanceFunction = distanceFunction;
	}

	public Listener getListener()
	{
		return listener;
	}

	public void setListener(Listener listener)
	{
		this.listener = listener;
	}

	public void compute()
	{
		calculateDistances();
		int move = makeAssignments();
		int i = 0;
		while (move > 0 && i++ < iteration)
		{
			if (points.length >= centroids.length)
			{
				move = fillEmptyCentroids();
				// calculateDistances();
			}
			moveCentroids();
			calculateDistances();
			move += makeAssignments();
			if (listener != null)
				listener.iteration(i, move);
		}
	}

	protected void calculateDistances()
	{
		for (int c = 0; c < centroids.length; c++)
		{
			if (!changes[c])
				continue;
			double[] centroid = centroids[c];
			for (int p = 0; p < points.length; p++)
			{
				double[] point = points[p];
				distances[c][p] = distanceFunction.distance(centroid, point);
			}
			changes[c] = false;
		}
	}

	protected int makeAssignments()
	{
		int move = 0;
		Arrays.fill(counts, 0);
		for (int p = 0; p < points.length; p++)
		{
			int nc = nearestCentroid(p);
			if (nc == -1)
				continue;
			if (assignments[p] != nc)
			{
				if (assignments[p] != -1)
					changes[assignments[p]] = true;
				changes[nc] = true;
				assignments[p] = nc;
				move++;
			}
			counts[nc]++;
			if (equal && counts[nc] > idealCount)
				move += remakeAssignments(nc);
		}
		return move;
	}

	protected int remakeAssignments(int cc)
	{
		int move = 0;
		double md = Double.MAX_VALUE;
		int nc = -1;
		int np = -1;
		for (int p = 0; p < points.length; p++)
		{
			if (assignments[p] != cc)
				continue;
			for (int c = 0; c < centroids.length; c++)
			{
				if (c == cc || dones[c])
					continue;
				double d = distances[c][p];
				if (d < md)
				{
					md = d;
					nc = c;
					np = p;
				}
			}
		}
		if (nc != -1 && np != -1)
		{
			if (assignments[np] != nc)
			{
				if (assignments[np] != -1)
				{
					changes[assignments[np]] = true;
				}
				changes[nc] = true;
				assignments[np] = nc;
				move++;
			}
			counts[cc]--;
			counts[nc]++;
			if (counts[nc] > idealCount)
			{
				dones[cc] = true;
				move += remakeAssignments(nc);
				dones[cc] = false;
			}
		}
		return move;
	}

	protected int nearestCentroid(int p)
	{
		double md = Double.MAX_VALUE;
		int nc = -1;
		for (int c = 0; c < centroids.length; c++)
		{
			double d = distances[c][p];
			if (d < md)
			{
				md = d;
				nc = c;
			}
		}
		return nc;
	}

	protected int nearestPoint(int inc, int fromc)
	{
		double md = Double.MAX_VALUE;
		int np = -1;
		for (int p = 0; p < points.length; p++)
		{
			if (assignments[p] != inc)
				continue;
			double d = distances[fromc][p];
			if (d < md)
			{
				md = d;
				np = p;
			}
		}
		return np;
	}

	protected int largestCentroid(int except)
	{
		int lc = -1;
		int mc = 0;
		for (int c = 0; c < centroids.length; c++)
		{
			if (c == except)
				continue;
			if (counts[c] > mc)
				lc = c;
		}
		return lc;
	}

	protected int fillEmptyCentroids()
	{
		int move = 0;
		for (int c = 0; c < centroids.length; c++)
		{
			if (counts[c] == 0)
			{
				int lc = largestCentroid(c);
				int np = nearestPoint(lc, c);
				assignments[np] = c;
				counts[c]++;
				counts[lc]--;
				changes[c] = true;
				changes[lc] = true;
				move++;
			}
		}
		return move;
	}

	protected void moveCentroids()
	{
		for (int c = 0; c < centroids.length; c++)
		{
			if (!changes[c])
				continue;
			double[] centroid = centroids[c];
			int n = 0;
			Arrays.fill(centroid, 0);
			for (int p = 0; p < points.length; p++)
			{
				if (assignments[p] != c)
					continue;
				double[] point = points[p];
				n++;
				for (int d = 0; d < centroid.length; d++)
					centroid[d] += point[d];
			}
			if (n > 0)
				for (int d = 0; d < centroid.length; d++)
					centroid[d] /= n;
		}
	}

	public static void main(String[] args)
	{
		int n = 100 ;
		int k = 2;
		Random random = new Random(System.currentTimeMillis());

		double[][] points = new double[n][2] ;
		for(int i = 0 ; i < n ; i++)
		{
			points[i][0] = Math.abs(random.nextInt() % 100);
			points[i][1] = Math.abs(random.nextInt() % 100);
		}	

		double[][] centroids = new double[k][2] ;
		int min = 0 ;
		int max = points.length - 1;
		for(int i = 0 ; i < k ; i++)
		{
			int index = min + (int)(Math.random() * ((max - min) + 1)); 
			System.arraycopy(points[index], 0, centroids[i], 0,
					points[index].length);
		}

		String str = "" ;
		for(int i = 0 ; i < k ; i++)
		{
			str += "centroid[" + i + "] : " ;
			for(int j = 0 ; j < 2 ; j++)
				str += "[" + centroids[i][0] + ", " + centroids[i][1] + "]";
			str += "\n" ;
		}
		System.err.println(str);

		KmeansClustering kmc = new KmeansClustering(centroids, points);
		kmc.setIteration(64);
		kmc.setDistanceFunction(KmeansClustering.EUCLIDEAN_DISTANCE_FUNCTION);
		kmc.compute();
		centroids = kmc.getCentroids();
		str = "" ;
		for(int i = 0 ; i < k ; i++)
		{
			str += "centroid[" + i + "] : " ;
				str += "[" + centroids[i][0] + ", " + centroids[i][1] + "]";
			str += "\n" ;
		}
		System.err.println(str);
	}
}

