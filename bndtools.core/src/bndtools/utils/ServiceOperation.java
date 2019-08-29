package bndtools.utils;

public interface ServiceOperation<R, S, E extends Throwable> {
	R execute(S service) throws E;
}
