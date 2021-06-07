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
 * This class represents the Err side of @{link Result}.
 *
 * @param <V> The value type
 */
final class Err<V> implements Result<V> {
	private final String error;

	Err(CharSequence error) {
		this.error = requireNonNull(error.toString());
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
	public Optional<String> error() {
		return Optional.of(error);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V unwrap() {
		throw new ResultException("Not an Ok value");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V unwrap(CharSequence message) throws ResultException {
		throw new ResultException(message.toString());
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
	public V orElseGet(SupplierWithException<? extends V> orElseSupplier) {
		try {
			return orElseSupplier.get();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <R extends Throwable> V orElseThrow(FunctionWithException<? super String, ? extends R> throwableSupplier)
		throws R {
		requireNonNull(throwableSupplier);
		R r;
		try {
			r = requireNonNull(throwableSupplier.apply(error));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
		throw r;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U> map(FunctionWithException<? super V, ? extends U> mapper) {
		requireNonNull(mapper);
		return asError();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V> mapErr(FunctionWithException<? super String, ? extends CharSequence> mapper) {
		try {
			return Result.err(mapper.apply(error));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U> flatMap(FunctionWithException<? super V, ? extends Result<? extends U>> mapper) {
		requireNonNull(mapper);
		return asError();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V> recover(FunctionWithException<? super String, ? extends V> recover) {
		try {
			V v = recover.apply(error);
			return (v != null) ? Result.ok(v) : this;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Result<V> recoverWith(FunctionWithException<? super String, ? extends Result<? extends V>> recover) {
		try {
			@SuppressWarnings("unchecked")
			Result<V> result = (Result<V>) requireNonNull(recover.apply(error));
			return result;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void accept(ConsumerWithException<? super V> ok, ConsumerWithException<? super String> err) {
		requireNonNull(ok);
		try {
			err.accept(error);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <U> Result<U> asError() {
		@SuppressWarnings("unchecked")
		Result<U> coerced = (Result<U>) this;
		return coerced;
	}

	@Override
	public String toString() {
		return String.format("Err(%s)", error);
	}
}
