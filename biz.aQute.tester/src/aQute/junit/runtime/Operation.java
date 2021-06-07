package aQute.junit.runtime;

/**
 * Represents an operation against a service of type <strong>S</strong> yielding
 * a result of type <strong>R</strong>
 *
 * @author Neil Bartlett
 * @param <S> The service type
 * @param <R> The result type
 */
public interface Operation<S, R> {
	R perform(S service) throws Exception;
}
