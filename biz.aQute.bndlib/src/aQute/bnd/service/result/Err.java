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
 * This class represents the Err side of @{link Result}.
 *
 * @param <V> The value type
 * @param <E> The error type
 */
public class Err<V, E> implements Result<V, E> {
	private final E error;

	/**
	 * Returns a new Err instance containing the given error.
	 *
	 * @param error the error
	 * @param <V> The type of the value
	 * @param <E> The type of the error
	 * @return a new error
	 */
	public static <V, E> Result<V, E> result(final E error) {
		return new Err<V, E>(error);
	}

	Err(final E error) {
		this.error = error;
	}

	@Override
	public Optional<V> getValue() {
		return Optional.empty();
	}

	@Override
	public Optional<E> getError() {
		return Optional.of(error);
	}

	@Override
	public boolean isOk() {
		return false;
	}

	@Override
	public boolean isErr() {
		return true;
	}

	@Override
	public V unwrap() {
		throw new ResultException("Cannot call unwrap() on an Err value");
	}

	@Override
	public V unwrap(final String message) throws ResultException {
		throw new ResultException(message);
	}

	@Override
	public String toString() {
		return String.format("Err(%s)", error);
	}

	@Override
	public Result<V, E> recover(FunctionWithException<E, V> recover) {
		try {
			V apply = recover.apply(error);
			return Ok.result(apply);
		} catch (Exception e) {
			return failed(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U> Result<U, E> map(FunctionWithException<V, U> lambda) {
		return (Result<U, E>) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <F> Result<V, F> mapErr(FunctionWithException<E, F> lambda) {
		try {
			return result(lambda.apply(error));
		} catch (Exception e) {
			return (Result<V, F>) failed(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <U> Result<U, E> flatMap(FunctionWithException<V, Result<U, E>> lambda) {
		return (Result<U, E>) this;
	}

	@Override
	public <R extends Throwable> V orElseThrow(FunctionWithException<? super E, ? extends R> f) throws R {
		R r;
		try {
			r = f.apply(error);
		} catch (Exception e) {
			throw new ResultException(e);
		}
		throw r;
	}
}
