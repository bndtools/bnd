package aQute.lib.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class ExceptionsTest {

	@Test
	public void testDuck() {
		IOException ioe = new IOException();
		assertThatExceptionOfType(IOException.class).isThrownBy(() -> duck(ioe))
			.isEqualTo(ioe);
		assertThatExceptionOfType(Throwable.class).isThrownBy(() -> duck(new Throwable()));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> duck(new RuntimeException()));
		assertThatExceptionOfType(Error.class).isThrownBy(() -> duck(new Error()));
	}

	private void duck(Throwable t) {
		throw Exceptions.duck(t);
	}

	@Test
	public void testUnrollCause() {
		IOException ioe1 = new IOException();
		IOException ioe2 = new IOException(ioe1);
		InvocationTargetException ite1 = new InvocationTargetException(ioe2);
		InvocationTargetException ite2 = new InvocationTargetException(ite1);
		assertThat(Exceptions.unrollCause(ite2)).isEqualTo(ioe1);
		assertThat(Exceptions.unrollCause(ite2, Throwable.class)).isEqualTo(ioe1);
		assertThat(Exceptions.unrollCause(ite2, InvocationTargetException.class)).isEqualTo(ioe2);
		assertThat(Exceptions.unrollCause(ioe2, InvocationTargetException.class)).isEqualTo(ioe2);
		assertThat(Exceptions.unrollCause(ioe1)).isEqualTo(ioe1);

		assertThatNullPointerException().isThrownBy(() -> Exceptions.unrollCause(null));
		assertThatNullPointerException()
			.isThrownBy(() -> Exceptions.unrollCause(null, InvocationTargetException.class));
		assertThatNullPointerException().isThrownBy(() -> Exceptions.unrollCause(ioe2, null));
	}

	@Test
	public void testFunctionWithException() {
		FunctionWithException<String, String> fwe = this::functionThrows;
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> fwe.orElseThrow()
			.apply("hi"))
			.withMessage("hi");
		assertThat(fwe.orElse("aloha")
			.apply("hi")).isEqualTo("aloha");
		assertThat(fwe.orElseGet(() -> "aloha")
			.apply("hi")).isEqualTo("aloha");

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> FunctionWithException.asFunction(fwe)
			.apply("hi"))
			.withMessage("hi");
		assertThat(FunctionWithException.asFunctionOrElse(fwe, "aloha")
			.apply("hi")).isEqualTo("aloha");
		assertThat(FunctionWithException.asFunctionOrElseGet(fwe, () -> "aloha")
			.apply("hi")).isEqualTo("aloha");
	}

	private <T, R> R functionThrows(T t) throws Exception {
		throw new Exception(String.valueOf(t));
	}

	@Test
	public void testBiFunctionWithException() {
		BiFunctionWithException<String, String, String> bfwe = this::biFunctionThrows;
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> bfwe.orElseThrow()
			.apply("hi", "hola"))
			.withMessage("hihola");
		assertThat(bfwe.orElse("aloha")
			.apply("hi", "hola")).isEqualTo("aloha");
		assertThat(bfwe.orElseGet(() -> "aloha")
			.apply("hi", "hola")).isEqualTo("aloha");

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> BiFunctionWithException.asBiFunction(bfwe)
			.apply("hi", "hola"))
			.withMessage("hihola");
		assertThat(BiFunctionWithException.asBiFunctionOrElse(bfwe, "aloha")
			.apply("hi", "hola")).isEqualTo("aloha");
		assertThat(BiFunctionWithException.asBiFunctionOrElseGet(bfwe, () -> "aloha")
			.apply("hi", "hola")).isEqualTo("aloha");
	}

	private <T, U, R> R biFunctionThrows(T t, U u) throws Exception {
		throw new Exception(String.valueOf(t) + String.valueOf(u));
	}

	@Test
	public void testPredicateWithException() {
		PredicateWithException<String> pwe = this::predicateThrows;
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> pwe.orElseThrow()
			.test("hi"))
			.withMessage("hi");
		assertThat(pwe.orElse(false)
			.test("hi")).isFalse();
		assertThat(pwe.orElseGet(() -> false)
			.test("hi")).isFalse();

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> PredicateWithException.asPredicate(pwe)
			.test("hi"))
			.withMessage("hi");
		assertThat(PredicateWithException.asPredicateOrElse(pwe, false)
			.test("hi")).isFalse();
		assertThat(PredicateWithException.asPredicateOrElseGet(pwe, () -> false)
			.test("hi")).isFalse();
	}

	private <T> boolean predicateThrows(T t) throws Exception {
		throw new Exception(String.valueOf(t));
	}

	@Test
	public void testSupplierWithException() {
		SupplierWithException<String> swe = this::supplierThrows;
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> swe.orElseThrow()
			.get())
			.withMessage("hi");
		assertThat(swe.orElse("aloha")
			.get()).isEqualTo("aloha");
		assertThat(swe.orElseGet(() -> "aloha")
			.get()).isEqualTo("aloha");

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> SupplierWithException.asSupplier(swe)
			.get())
			.withMessage("hi");
		assertThat(SupplierWithException.asSupplierOrElse(swe, "aloha")
			.get()).isEqualTo("aloha");
		assertThat(SupplierWithException.asSupplierOrElseGet(swe, () -> "aloha")
			.get()).isEqualTo("aloha");
	}

	private <T> T supplierThrows() throws Exception {
		throw new Exception("hi");
	}

	@Test
	public void testConsumerWithException() {
		ConsumerWithException<String> cwe = this::consumerThrows;
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> cwe.orElseThrow()
			.accept("hi"))
			.withMessage("hi");
		cwe.ignoreException()
			.accept("hi");

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> ConsumerWithException.asConsumer(cwe)
			.accept("hi"))
			.withMessage("hi");
		ConsumerWithException.asConsumerIgnoreException(cwe)
			.accept("hi");
	}

	private <T> void consumerThrows(T t) throws Exception {
		throw new Exception(String.valueOf(t));
	}

	@Test
	public void testBiConsumerWithException() {
		BiConsumerWithException<String, String> bcwe = this::biConsumerThrows;
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> bcwe.orElseThrow()
			.accept("hi", "hola"))
			.withMessage("hihola");
		bcwe.ignoreException()
			.accept("hi", "hola");

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> BiConsumerWithException.asBiConsumer(bcwe)
			.accept("hi", "hola"))
			.withMessage("hihola");
		BiConsumerWithException.asBiConsumerIgnoreException(bcwe)
			.accept("hi", "hola");
	}

	private <T, U> void biConsumerThrows(T t, U u) throws Exception {
		throw new Exception(String.valueOf(t) + String.valueOf(u));
	}

	@Test
	public void testRunnableWithException() {
		RunnableWithException rwe = this::runnableThrows;
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> rwe.orElseThrow()
			.run())
			.withMessage("hi");
		rwe.ignoreException()
			.run();

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> RunnableWithException.asRunnable(rwe)
			.run())
			.withMessage("hi");
		RunnableWithException.asRunnableIgnoreException(rwe)
			.run();
	}

	private void runnableThrows() throws Exception {
		throw new Exception("hi");
	}

	@Test
	public void testUnchecked() {
		AtomicBoolean called = new AtomicBoolean(false);
		assertThatCode(() -> {
			Exceptions.unchecked(() -> {
				called.set(true);
				return; // Runnable
			});
		}).doesNotThrowAnyException();
		assertThat(called).isTrue();

		called.set(false);
		AtomicReference<String> result = new AtomicReference<>(null);
		assertThatCode(() -> {
			String v = Exceptions.unchecked(() -> {
				called.set(true);
				return "value"; // Callable
			});
			result.set(v);
		}).doesNotThrowAnyException();
		assertThat(called).isTrue();
		assertThat(result).hasValue("value");

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> Exceptions.unchecked(() -> runnableThrows()))
			.withMessage("hi");
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> Exceptions.unchecked(this::runnableThrows))
			.withMessage("hi");

		assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
			String v = Exceptions.unchecked(() -> callableThrows());
		})
			.withMessage("hiho");
		assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
			String v = Exceptions.unchecked(this::callableThrows);
		})
			.withMessage("hiho");
	}

	private String callableThrows() throws Exception {
		throw new Exception("hiho");
	}
}
