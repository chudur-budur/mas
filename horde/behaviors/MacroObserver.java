package sim.app.horde.behaviors;

public interface MacroObserver
{
	void transitioned(Macro macro, int from, int to);
}
