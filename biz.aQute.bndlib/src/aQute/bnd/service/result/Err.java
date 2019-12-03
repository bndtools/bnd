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

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * This class represents the Err side of @{link Result}.
 *
 * @param <V> The value type
 * @param <E> The error type
 */
public final class Err<V, E> implements Result<V, E> {
	private final E error;

	Err(E error) {
		this.error = requireNonNull(error);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOk() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isErr() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<V> value() {
		return Optional.empty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<E> error() {
		return Optional.of(error);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V unwrap() {
		throw new ResultException("Cannot call unwrap() on an Err value");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V unwrap(String message) throws ResultException {
		throw new ResultException(message);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V orElse(V orElse) {
		return orElse;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V orElseGet(Supplier<? extends V> orElseSupplier) {
		requireNonNull(orElseSupplier);
		return orElseSupplier.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <R extends Throwable> V orElseThrow(FunctionWithException<? super E, ? extends R> throwableSupplier)
		throws R {
		requireNonNull(throwableSupplier);
		R r;
		try {
			r = requireNonNull(throwableSupplier.apply(error));
		} catch (Exception e) {
			throw new ResultException(e);
		}
		throw r;
	}

	@SuppressWarnings("unchecked")
	private <U> Result<U, E> coerce() {
		return (Result<U, E>) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U, E> map(FunctionWithException<? super V, ? extends U> mapper) {
		requireNonNull(mapper);
		return coerce();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <F> Result<V, F> mapErr(FunctionWithException<? super E, ? extends F> mapper) {
		requireNonNull(mapper);
		try {
			return new Err<>(mapper.apply(error));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U, E> flatMap(
		FunctionWithException<? super V, ? extends Result<? extends U, ? extends E>> mapper) {
		requireNonNull(mapper);
		return coerce();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V, E> recover(FunctionWithException<? super E, ? extends V> recover) {
		requireNonNull(recover);
		try {
			return new Ok<>(recover.apply(error));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public String toString() {
		return String.format("Err(%s)", error);
	}
}
