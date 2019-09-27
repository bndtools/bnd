package aQute.junit.runtime;

public abstract class VoidOperation<S> implements Operation<S, Object> {
	@Override
	public final Object perform(S param) {
		doPerform(param);
		return null;
	}

	protected abstract void doPerform(S param);
}
