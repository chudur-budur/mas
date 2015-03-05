package sim.app.horde.behaviors;

public class RotateNeg extends Rotate
{
	private static final long serialVersionUID = 1;
	public RotateNeg()
	{
		super(-2);    // good default
		setKeyStroke(KS_LEFT);
	}
}
