package sim.app.horde.irl ;

public class QTable
{
	private double[][] qtable = null ;
	private double alpha = 0.01 ;
	private double gamma = 0.5 ;

	public QTable()
	{
		this.qtable = new double[ExpertDemo.BEHAVIOUR_COUNT]
		[ExpertDemo.BEHAVIOUR_COUNT];
	}

	public QTable(RewardFunction rf)
	{
		this.qtable = new double[rf.getNumRows()][rf.getNumCols()] ;
	}

	public void populateWithRandomValues()
	{
		for(int i = 0 ; i < this.qtable.length ; i++)
			for(int j = 1 ; j < this.qtable[i].length ; j++)
				this.qtable[i][j] = Math.random();
	}

	// getters and setters
	public double get(int i, int j)
	{
		return this.qtable[i][j];
	}
	public void set(int i, int j, double val)
	{
		this.qtable[i][j] = val ;
	}

	private double getNextBestQValue(int currentState)
	{
		double maxVal = this.qtable[currentState][1] ;
		for(int i = 2 ; i < this.qtable[currentState].length-1 ; i++)
			if(this.qtable[currentState][i] > maxVal)
				maxVal = this.qtable[currentState][i] ;
		return maxVal ;
	}
	
	private double getNextBestQValue(int currentState, int K)
	{
		double maxVal = this.qtable[currentState][K] ;
		for(int i = K+1 ; i < this.qtable[currentState].length-K ; i++)
			if(this.qtable[currentState][i] > maxVal)
				maxVal = this.qtable[currentState][i] ;
		return maxVal ;
	}

	public int getNextBestState(int currentState)
	{
		int nextState = 1 ;
		double maxVal = this.qtable[currentState][1] ;
		boolean allSame = false ;
		for(int c = 2 ; c < this.qtable[currentState].length-1 ; c++)
			if(this.qtable[currentState][c] >= maxVal)
			{
				maxVal = this.qtable[currentState][c] ;
				nextState = c;
				if(maxVal == this.qtable[currentState][c])
					allSame = true ;
			}
		if(allSame)
			nextState = getNextRandomState(currentState);
		return nextState ;
	}

	public int getNextBestState(int currentState, int K, boolean original)
	{
		int nextState = K ;
		double maxVal = this.qtable[currentState][K+1] ;
		boolean allSame = false ;
		for(int c = K+1 ; c < this.qtable[currentState].length-K ; c++)
			if(this.qtable[currentState][c] >= maxVal)
			{
				maxVal = this.qtable[currentState][c] ;
				nextState = c;
				if(maxVal == this.qtable[currentState][c])
					allSame = true ;
			}
		if(allSame)
			nextState = getNextRandomState(currentState, K, true);
		if(original)
			return nextState ;
		else
			return (int)(nextState / K);
	}

	public int getNextRandomState(int currentState)
	{
		int min = 1 ;
		int max = this.qtable[currentState].length - 2;
		return min + (int)(Math.random() * ((max - min) + 1));
	}
	
	public int getNextRandomState(int currentState, int K, boolean original)
	{
		int min = K ;
		int max = this.qtable[currentState].length - (K+1);
		int index = min + (int)(Math.random() * ((max - min) + 1));
		if(original)
			return index ;
		else
			return (int)(index / K) ; 
	}
	
	public int learnQTableModelFree(int currentState, 
			double[] currentFeatureVals, RewardFunction rf)
	{
		int nextState = 1 ;
	        if(Math.random() < 0.1)	
			nextState = getNextRandomState(currentState);
		else
			nextState = getNextBestState(currentState);
		double reward = rf.getReward(currentState, nextState, currentFeatureVals);
		this.qtable[currentState][nextState] = 
			(1.0 - alpha) * this.qtable[currentState][nextState] 
				+ alpha * (reward + gamma * getNextBestQValue(nextState));
		return nextState ;
	}

	public int learnQTableModelFree(int currentState, 
			double[] currentFeatureVals, RewardFunction rf, int K)
	{
		int nextState = 1 ;
	        if(Math.random() < 0.15)	
			nextState = getNextRandomState(currentState, K, true);
		else
			nextState = getNextBestState(currentState, K, true);
		double reward = rf.getReward(currentState, nextState, currentFeatureVals);
		this.qtable[currentState][nextState] = 
			(1.0 - alpha) * this.qtable[currentState][nextState] 
				+ alpha * (reward + gamma * getNextBestQValue((int)(nextState / K)));
		System.err.println("--- QTable.learnQTableModelFree() : nextState == " 
				+ ((int)(nextState / K)));
		return (int)(nextState / K);
	}
	
	public int learnQTableWithModel(int currentState,
		double[] currentFeature, RewardFunction rf)
	{
		return 1 ;	
	}	

	public String toString()
	{
		String str = "" ;
		if(this.qtable != null)
		{
			for(int i = 0 ; i < this.qtable.length ; i++)
			{
				for(int j = 0 ; j < this.qtable[i].length ; j++)
					str += "[" + String.format("%+.2f", this.qtable[i][j]) + "] " ;
				str += "\n";
			}
		}
		return str ;
	}

	// shallow copy
	public QTable clone()
	{
		QTable q = null ;
		if(this.qtable != null)
		{
			q = new QTable();
			q.qtable = this.qtable ;
		}
		return q ;
	}

	public static void main(String args[])
	{
		QTable qt = new QTable();
		qt.populateWithRandomValues();
		System.err.println(qt.toString());
	}

}
