/*
 * Copyright 2017 René Perschon <rperschon85@gmail.com>
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

/**
 * This class represents the Ok side of @{link Result}.
 *
 * @param <V> The value type
 * @param <E> The error type
 */
public class Ok<V, E> implements Result<V, E> {
	private final V value;

	/**
	 * Returns a new Ok instance containing the given value.
	 *
	 * @param value the value
	 * @param <V> The type of the value
	 * @param <E> The type of the error
	 * @return see above
	 */
	public static <V, E> Result<V, E> result(final V value) {
		return new Ok<V, E>(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value the value
	 */
	Ok(final V value) {
		super();
		this.value = value;
	}

	@Override
	public Optional<V> getValue() {
		return Optional.of(value);
	}

	@Override
	public Optional<E> getError() {
		return Optional.empty();
	}

	@Override
	public boolean isOk() {
		return true;
	}

	@Override
	public boolean isErr() {
		return false;
	}

	@Override
	public V unwrap() {
		return value;
	}

	@Override
	public V unwrap(final String message) throws ResultException {
		return value;
	}

	@Override
	public String toString() {
		return String.format("Ok(%s)", value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U> Result<U, E> map(FunctionWithException<V, U> lambda) {
		try {
			U apply = lambda.apply(value);
			return result(apply);
		} catch (Exception e) {
			return (Result<U, E>) failed(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <F> Result<V, F> mapErr(FunctionWithException<E, F> lambda) {
		return (Result<V, F>) this;
	}

	@Override
	public Result<V, E> recover(FunctionWithException<E, V> recover) {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U> Result<U, E> flatMap(FunctionWithException<V, Result<U, E>> lambda) {
		try {
			return lambda.apply(value);
		} catch (Exception e) {
			return (Result<U, E>) failed(e);
		}
	}

	@Override
	public <R extends Throwable> V orElseThrow(FunctionWithException<? super E, ? extends R> f) {
		return value;
	}
}
