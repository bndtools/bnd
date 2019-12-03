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
 * This class represents the Ok side of @{link Result}.
 *
 * @param <V> The value type
 * @param <E> The error type
 */
public final class Ok<V, E> implements Result<V, E> {
	private final V value;

	/**
	 * Constructor.
	 *
	 * @param value the value
	 */
	Ok(V value) {
		this.value = requireNonNull(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOk() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isErr() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<V> value() {
		return Optional.of(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<E> error() {
		return Optional.empty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V unwrap() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V unwrap(String message) throws ResultException {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V orElse(V orElse) {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V orElseGet(Supplier<? extends V> orElseSupplier) {
		requireNonNull(orElseSupplier);
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <R extends Throwable> V orElseThrow(FunctionWithException<? super E, ? extends R> throwableSupplier)
		throws R {
		requireNonNull(throwableSupplier);
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U, E> map(FunctionWithException<? super V, ? extends U> mapper) {
		requireNonNull(mapper);
		try {
			return new Ok<>(mapper.apply(value));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <F> Result<V, F> coerce() {
		return (Result<V, F>) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <F> Result<V, F> mapErr(FunctionWithException<? super E, ? extends F> mapper) {
		requireNonNull(mapper);
		return coerce();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U, E> flatMap(
		FunctionWithException<? super V, ? extends Result<? extends U, ? extends E>> mapper) {
		requireNonNull(mapper);
		try {
			@SuppressWarnings("unchecked")
			Result<U, E> result = (Result<U, E>) requireNonNull(mapper.apply(value));
			return result;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V, E> recover(FunctionWithException<? super E, ? extends V> recover) {
		requireNonNull(recover);
		return this;
	}

	@Override
	public String toString() {
		return String.format("Ok(%s)", value);
	}
}
