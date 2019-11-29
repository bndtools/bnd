/*
 * Copyright 2016 René Perschon <rperschon85@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aQute.bnd.service.result;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The Result type is an alternative way of chaining together functions in a
 * functional programming style while hiding away error handling structures such
 * as try-catch-blocks and conditionals.<br/>
 * Instead of adding a throws declaration to a function, the return type of the
 * function is instead set to Result<V, E> where V is the original return type,
 * i.e. the "happy case" and E is the error type, usually the Exception type or
 * a String if an error explanation is sufficient.<br/>
 * <br/>
 * Example:
 *
 * <pre>
 * public Result<Float, String> divide(int a, int b) {
 * 	if (b == 0) {
 * 		return Result.err("Can't divide by zero!");
 * 	} else {
 * 		return Result.ok(a / b);
 * 	}
 * }
 * </pre>
 *
 * @param <V> The value type of the Result.
 * @param <E> The error type of the Result.
 */
public interface Result<V, E> {

	/**
	 * Returns the value of this instance as an {@link Optional}. Returns
	 * Optional.empty() if this is an Err instance.
	 *
	 * @return see above.
	 */
	Optional<V> getValue();

	/**
	 * Returns the error of this instance as an {@link Optional}. Returns
	 * Optional.empty() if this is an Ok instance.
	 *
	 * @return see above.
	 */
	Optional<E> getError();

	/**
	 * Returns <code>true</code> if this instance represents an Ok value, false
	 * otherwise.
	 *
	 * @return see above.
	 */
	boolean isOk();

	/**
	 * Returns <code>true</code> if this instance represents an Err value, false
	 * otherwise.
	 *
	 * @return see above
	 */
	boolean isErr();

	/**
	 * Returns the contained value if this is an Ok value, otherwise throws a
	 * ResultException.
	 *
	 * @return the contained value
	 * @throws ResultException in case unwrap() is called on an Err value
	 */
	V unwrap() throws ResultException;

	/**
	 * Express the expectation that this object is an Ok value. If it's an Err
	 * value instead, throw a ResultException with the given message.
	 *
	 * @param message the message to pass to a potential ResultException
	 * @throws ResultException if unwrap() is called on an Err value
	 */
	V unwrap(String message) throws ResultException;

	/**
	 * If this is an Ok value, flatMap() returns the result of the given
	 * {@link FunctionWithException}. Otherwise returns this.
	 *
	 * @param lambda The {@link FunctionWithException} to be called with the
	 *            value of this.
	 * @param <U> The new value type.
	 * @return see above.
	 */
	@SuppressWarnings("unchecked")
	<U> Result<U, E> flatMap(final FunctionWithException<V, Result<U, E>> lambda);

	/**
	 * If this is an Ok value, map() returns the result of the given
	 * {@link FunctionWithException}, wrapped in a new Ok Result instance.
	 * Otherwise returns this.
	 *
	 * @param lambda The {@link FunctionWithException} to call with the value of
	 *            this.
	 * @param <U> The new value type.
	 * @return see above.
	 */
	<U> Result<U, E> map(final FunctionWithException<V, U> lambda);

	/**
	 * If this is an Err value, mapErr() returns the result of the given @{link
	 * Function}, wrapped in a new Err Result instance. Otherwise returns this.
	 *
	 * @param lambda The {@link FunctionWithException} to call with the error of
	 *            this.
	 * @param <F> The new error type.
	 * @return see above
	 */
	<F> Result<V, F> mapErr(final FunctionWithException<E, F> lambda);

	/**
	 * If this is an ok value, return this. Otherwise change the error into a
	 * new value and return ok.
	 *
	 * @param recover lambda converting error in a value
	 * @return a result
	 */
	Result<V, E> recover(FunctionWithException<E, V> recover);

	default V orElse(V value) {
		return getValue().orElse(value);
	}

	default V orElseGet(Supplier<V> value) {
		return getValue().orElseGet(value);
	}

	<R extends Throwable> V orElseThrow(FunctionWithException<? super E, ? extends R> f) throws R;

	default Result<V, E> failed(Exception e) {
		throw Private.duck(e);
	}

	static <O, F> Result<O, F> fromNull(O ok, F err) {
		if (ok == null)
			return Err.result(err);
		return Ok.result(ok);
	}
}
