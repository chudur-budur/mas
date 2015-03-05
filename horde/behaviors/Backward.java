package sim.app.horde.behaviors;


public class Backward extends Forward
{
	private static final long serialVersionUID = 1;
	public Backward()
	{
		super(-0.1);     // good default
		name = "Backward[" + -speed + "]";
		setKeyStroke(null);
	}
}
