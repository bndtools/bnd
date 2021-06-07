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

package aQute.bnd.result;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import aQute.bnd.exceptions.ConsumerWithException;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.FunctionWithException;
import aQute.bnd.exceptions.SupplierWithException;

/**
 * This class represents the Ok side of @{link Result}.
 *
 * @param <V> The value type
 */
final class Ok<V> implements Result<V> {
	private final V value;

	/**
	 * Constructor.
	 *
	 * @param value the value
	 */
	Ok(V value) {
		this.value = value;
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
		return Optional.ofNullable(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<String> error() {
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
	public V unwrap(CharSequence message) throws ResultException {
		requireNonNull(message);
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
	public V orElseGet(SupplierWithException<? extends V> orElseSupplier) {
		requireNonNull(orElseSupplier);
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <R extends Throwable> V orElseThrow(FunctionWithException<? super String, ? extends R> throwableSupplier)
		throws R {
		requireNonNull(throwableSupplier);
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U> map(FunctionWithException<? super V, ? extends U> mapper) {
		try {
			return Result.ok(mapper.apply(value));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V> mapErr(FunctionWithException<? super String, ? extends CharSequence> mapper) {
		requireNonNull(mapper);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U> flatMap(FunctionWithException<? super V, ? extends Result<? extends U>> mapper) {
		try {
			@SuppressWarnings("unchecked")
			Result<U> result = (Result<U>) requireNonNull(mapper.apply(value));
			return result;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V> recover(FunctionWithException<? super String, ? extends V> recover) {
		requireNonNull(recover);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V> recoverWith(FunctionWithException<? super String, ? extends Result<? extends V>> recover) {
		requireNonNull(recover);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void accept(ConsumerWithException<? super V> ok, ConsumerWithException<? super String> err) {
		requireNonNull(err);
		try {
			ok.accept(value);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U> asError() {
		throw new ResultException("Not an Err value");
	}

	@Override
	public String toString() {
		return String.format("Ok(%s)", value);
	}
}
