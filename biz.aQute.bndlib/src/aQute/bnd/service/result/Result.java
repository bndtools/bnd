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
	 * Returns an {@link Ok} if the {@code value} parameter is non-{@code null}
	 * or an {@link Err} otherwise. Either one of {@code value} or {@code error}
	 * must not be {@code null}.
	 *
	 * @param <V> The value type of the Result.
	 * @param <E> The error type of the Result.
	 * @param value If non-{@code null}, an {@link Ok} result is returned with
	 *            the specified value.
	 * @param error If {@code value} is {@code null}, an {@link Err} result is
	 *            returned with the specified error.
	 * @return An {@link Ok} if the {@code value} parameter is non-{@code null}
	 *         or an {@link Err} otherwise.
	 */
	static <V, E> Result<V, E> of(V value, E error) {
		return (value != null) ? ok(value) : err(error);
	}

	/**
	 * Returns an {@link Ok} containing the specified {@code value}.
	 *
	 * @param <V> The value type of the Result.
	 * @param <E> The error type of the Result.
	 * @param value The value to contain in the {@link Ok} result. Must not be
	 *            {@code null}.
	 * @return An {@link Ok} result.
	 */
	static <V, E> Result<V, E> ok(V value) {
		return new Ok<>(value);
	}

	/**
	 * Returns an {@link Err} containing the specified {@code error}.
	 *
	 * @param <V> The value type of the Result.
	 * @param <E> The error type of the Result.
	 * @param error The error to contain in the {@link Err} result. Must not be
	 *            {@code null}.
	 * @return An {@link Err} result.
	 */
	static <V, E> Result<V, E> err(E error) {
		return new Err<>(error);
	}

	/**
	 * Returns {@code true} if this instance represents an {@link Ok} value,
	 * {@code false} otherwise.
	 *
	 * @return {@code true} if this instance represents an {@link Ok} value,
	 *         {@code false} otherwise.
	 */
	boolean isOk();

	/**
	 * Returns {@code true} if this instance represents an {@link Err} value,
	 * {@code false} otherwise.
	 *
	 * @return {@code true} if this instance represents an {@link Err} value,
	 *         {@code false} otherwise.
	 */
	boolean isErr();

	/**
	 * Returns the value of this instance as an {@link Optional}. Returns
	 * {@code Optional.empty()} if this is an {@link Err} instance.
	 *
	 * @return The value of this instance as an {@link Optional}. Returns
	 *         {@code Optional.empty()} if this is an {@link Err} instance.
	 */
	Optional<V> value();

	/**
	 * Returns the error of this instance as an {@link Optional}. Returns
	 * {@code Optional.empty()} if this is an {@link Ok} instance.
	 *
	 * @return The error of this instance as an {@link Optional}. Returns
	 *         {@code Optional.empty()} if this is an {@link Ok} instance.
	 */
	Optional<E> error();

	/**
	 * Returns the contained value if this is an {@link Ok} value. Otherwise
	 * throws a {@link ResultException}.
	 *
	 * @return The contained value
	 * @throws ResultException If this is an {@link Err} instance.
	 */
	V unwrap();

	/**
	 * Express the expectation that this object is an {@link Ok} value.
	 * Otherwise throws a {@link ResultException} with the specified message.
	 *
	 * @param message The message to pass to a potential ResultException.
	 * @throws ResultException If this is an {@link Err} instance.
	 */
	V unwrap(String message);

	/**
	 * Returns the contained value if this is an {@link Ok} value. Otherwise
	 * returns the specified alternate value.
	 *
	 * @param orElse The value to return if this is an {@link Err} instance.
	 * @return The contained value or the alternate value
	 */
	V orElse(V orElse);

	/**
	 * Returns the contained value if this is an {@link Ok} value. Otherwise
	 * returns the alternate value supplied by the specified supplier.
	 *
	 * @param orElseSupplier The supplier to supply an alternate value if this
	 *            is an {@link Err} instance. Must not be {@code null}.
	 * @return The contained value or the alternate value
	 */
	V orElseGet(Supplier<? extends V> orElseSupplier);

	/**
	 * Returns the contained value if this is an {@link Ok} value. Otherwise
	 * throws the exception supplied by the specified function.
	 *
	 * @param <R> The exception type.
	 * @param throwableSupplier The supplier to supply an exception if this is
	 *            an {@link Err} instance. Must not be {@code null}. The
	 *            supplier must return a non-{@code null} result.
	 * @return The contained value.
	 * @throws R If this is an {@link Err} instance.
	 */
	<R extends Throwable> V orElseThrow(FunctionWithException<? super E, ? extends R> throwableSupplier) throws R;

	/**
	 * Map the contained value if this is an {@link Ok} value. Otherwise return
	 * this.
	 *
	 * @param <U> The new value type.
	 * @param mapper The function to map the contained value into a new value.
	 *            Must not be {@code null}. The function must return a
	 *            non-{@code null} value.
	 * @return A result containing the mapped value if this is an {@link Ok}
	 *         value. Otherwise this.
	 */
	<U> Result<U, E> map(FunctionWithException<? super V, ? extends U> mapper);

	/**
	 * Map the contained error if this is an {@link Err} value. Otherwise return
	 * this.
	 *
	 * @param <F> The new error type.
	 * @param mapper The function to map the contained error into a new error.
	 *            Must not be {@code null}. The function must return a
	 *            non-{@code null} error.
	 * @return A result containing the mapped error if this is an {@link Err}
	 *         value. Otherwise this.
	 */
	<F> Result<V, F> mapErr(FunctionWithException<? super E, ? extends F> mapper);

	/**
	 * FlatMap the contained value if this is an {@link Ok} value. Otherwise
	 * return this.
	 *
	 * @param <U> The new value type.
	 * @param mapper The function to flatmap the contained value into a new
	 *            result. Must not be {@code null}. The function must return a
	 *            non-{@code null} result.
	 * @return The flatmapped result if this is an {@link Ok} value. Otherwise
	 *         this.
	 */
	<U> Result<U, E> flatMap(FunctionWithException<? super V, ? extends Result<? extends U, ? extends E>> mapper);

	/**
	 * Recover the contained error if this is an {@link Err} value. Otherwise
	 * return this.
	 *
	 * @param recover The function to recover the contained error into a new
	 *            value. Must not be {@code null}. The function must return a
	 *            non-{@code null} value.
	 * @return A result containing the new value if this is an {@link Err}
	 *         value. Otherwise this.
	 */
	Result<V, E> recover(FunctionWithException<? super E, ? extends V> recover);

}
