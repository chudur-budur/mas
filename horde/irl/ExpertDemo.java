package sim.app.horde.irl ;

import java.util.* ;
import sim.app.horde.* ;

public class ExpertDemo
{
	// expert demo features are in 'horde/expertdemos'
	public static String EXPERT_DEMO_DIR = "/irl/expertdemos/" ;
	// and we have 6 demos so far
	public static int DEMO_COUNT = 6 ;
	// these are some default values
	// because BEHAVIOUR_COUNT is not possible
	// to guess until you have read all expert demos
	public static int BEHAVIOUR_COUNT = 5 ;
	// but this can be determined
	public static int FEATURE_COUNT = 3 ;

	private int demoNumber = -1 ;
	private HashMap<String, ArrayList<TransitionFeatureTuple>>
	expertDemo = null ;

	public ExpertDemo()
	{
		this.expertDemo =
		    new HashMap<String, ArrayList<TransitionFeatureTuple>>();
	}
	public ExpertDemo(int dn)
	{
		this.expertDemo =
		    new HashMap<String, ArrayList<TransitionFeatureTuple>>();
		this.demoNumber = dn ;
	}

	// getters & setters
	public HashMap<String, ArrayList<TransitionFeatureTuple>> getExpertDemo()
	{
		return this.expertDemo ;
	}
	public Set<String> keySet()
	{
		return this.expertDemo.keySet();
	}
	public ArrayList<TransitionFeatureTuple> get(String key)
	{
		return this.expertDemo.get(key);
	}

	// load expert demos and organize them in the data-structures
	public void loadExpertDemo(int demoNum)
	{
		this.demoNumber = demoNum ;
		String fileName = "demo-" + this.demoNumber + ".xmp" ;
		Scanner scanner = new Scanner(
		    Horde.class.getResourceAsStream(
		        EXPERT_DEMO_DIR + fileName));
		while (scanner.hasNextLine())
		{
			String line = scanner.nextLine().trim();
			if (line.length() == 0) continue ;

			String[] token = line.split("\\s+");
			int sindex = Integer.parseInt(token[0].trim().substring(
			                                  0, token[0].indexOf('-')));
			String sname = token[0].trim().substring(token[0].indexOf('-')+1);
			int eindex = Integer.parseInt(token[token.length-2].trim().substring(0,
			                              token[token.length - 2].indexOf('-')));
			String ename = token[token.length - 2].trim().substring(
			                   token[token.length - 2].indexOf('-')+1);
			double[] vals = new double[token.length - 3] ;
			for(int i = 1, j = 0 ; i < token.length - 2 ; i++, j++)
				vals[j] = Double.parseDouble(token[i]);
			boolean cont = Boolean.parseBoolean(token[token.length - 1]);
			TransitionFeatureTuple tft = new TransitionFeatureTuple(
			    sindex, sname, eindex, ename, vals, cont);
			String key = sindex + "-" + eindex ;
			if(this.expertDemo.containsKey(key))
			{
				ArrayList<TransitionFeatureTuple> lst = this.expertDemo.get(key);
				lst.add(tft);
			}
			else
			{
				ArrayList<TransitionFeatureTuple> lst =
				    new ArrayList<TransitionFeatureTuple>();
				lst.add(tft);
				this.expertDemo.put(key, lst);
			}
		}
		scanner.close();
		// now update the feature count
		String key = (String)this.expertDemo.keySet().toArray()[0];
		ArrayList<TransitionFeatureTuple> lst = this.expertDemo.get(key);
		FEATURE_COUNT =  lst.get(0).getFeatureValues().length ;
	}

	// go through all demos and merge them according to the
	// similar transitions
	public static ExpertDemo mergeDemos(ExpertDemo[] demo)
	{
		HashMap<String, ArrayList<TransitionFeatureTuple>> hm
		= new HashMap<String, ArrayList<TransitionFeatureTuple>>();
		for(int i = 0 ; i < demo.length ; i++)
		{
			for(String key : demo[i].keySet())
			{
				if(hm.containsKey(key))
				{
					ArrayList<TransitionFeatureTuple> lst = hm.get(key) ;
					lst.addAll(demo[i].get(key));
					hm.put(key, lst);
				}
				else
					hm.put(key, demo[i].get(key));
			}
		}
		ExpertDemo ed = new ExpertDemo();
		ed.expertDemo = hm ;
		return ed ;
	}

	// stringize
	public String toString()
	{
		String str = "" ;
		if(this.expertDemo != null)
		{
			for(String key : this.expertDemo.keySet())
			{
				str += key + ":\n";
				for(TransitionFeatureTuple t : this.expertDemo.get(key))
					str += t.toString() + "\n";
				str += "\n" ;
			}
		}
		return str ;
	}

	// deep copy
	public ExpertDemo clone()
	{
		ExpertDemo demo = null ;
		if(this.expertDemo != null)
		{
			HashMap<String, ArrayList<TransitionFeatureTuple>> hm
			= new HashMap<String, ArrayList<TransitionFeatureTuple>>();
			for(String key : this.expertDemo.keySet())
			{
				ArrayList<TransitionFeatureTuple> lst
				= new ArrayList<TransitionFeatureTuple>();
				for(TransitionFeatureTuple t : this.expertDemo.get(key))
					lst.add(t.clone());
				hm.put(key, lst);
			}
			demo = new ExpertDemo(this.demoNumber);
			demo.expertDemo = hm ;
		}
		return demo ;
	}

	public static void main(String[] args)
	{
		ExpertDemo[] edemo = new ExpertDemo[ExpertDemo.DEMO_COUNT];
		for(int i = 0 ; i < ExpertDemo.DEMO_COUNT ; i++)
		{
			edemo[i] = new ExpertDemo();
			edemo[i].loadExpertDemo(i);
			//System.err.println("Demo" + i +": \n" + edemo[i].toString());
		}

		/*ExpertDemo all = ExpertDemo.mergeDemos(edemo);
		System.err.println("All Demos: \n" + all.toString());*/
		System.err.println("Demo" + 1 +": \n" + edemo[1].toString());
	}
}
