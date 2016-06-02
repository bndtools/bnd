package aQute.lib.exceptions;

public interface FunctionWithException<T, R> {
	R apply(T t) throws Exception;
}
