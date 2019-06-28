package aQute.lib.aspects;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import aQute.lib.converter.Converter;
import aQute.lib.exceptions.BiFunctionWithException;
import aQute.lib.exceptions.ConsumerWithException;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.exceptions.FunctionWithException;
import aQute.lib.exceptions.RunnableWithException;
import aQute.lib.exceptions.SupplierWithException;

/**
 * Minute library to do some aspect oriented programming without dragging in the
 * world. Should not be used for high performance things.
 */
public class Aspects {
	static MethodHandles.Lookup	publicLookup	= MethodHandles.publicLookup();
	static final Object[]		EMPTY			= new Object[0];
	public static final Object	NORETURN		= new Object();
	public static final Object	DEFAULT			= new Object();

	public static class Invocation {
		final Object	proxy;
		final Method	method;
		final Object[]	args;

		Invocation(Object proxy, Method method, Object[] args) {
			this.proxy = proxy;
			this.method = method;
			this.args = args == null ? EMPTY : args;
		}

		@Override
		public String toString() {
			return "Invocation [" + (method != null ? "method=" + method.getName() + ", " : "")
				+ (args != null ? "args=" + Arrays.toString(args) : "") + "]";
		}
	}

	/**
	 * A builder to create a proxy that delegates to another object but can
	 * intercept calls, put something before, after and around calls.
	 *
	 * @param <T> the delegate type
	 */
	public interface InterceptBuilder<T> {

		/**
		 * Intercept a method call with a lambda. This is the generic form. For
		 * 0, 1, and 2 argument forms there are more convenient shortcuts. The
		 * number of types and the given arguments to the lambda must match
		 *
		 * @param intercept the lambda to intercept
		 * @param name the method name
		 * @param types the types
		 */
		InterceptBuilder<T> intercept(FunctionWithException<Invocation, Object> intercept, String name,
			Class<?>... types);

		/**
		 * Intercept a no method call
		 *
		 * @param intercept the no method lambda
		 * @param name the name of the method without arguments
		 */
		<R> InterceptBuilder<T> intercept(SupplierWithException<R> intercept, String name);

		/**
		 * Intercept a no method call
		 *
		 * @param intercept the no method lambda
		 * @param name the name of the method without arguments
		 */
		<R> InterceptBuilder<T> intercept(RunnableWithException intercept, String name);

		/**
		 * Intercept a one argument method call
		 *
		 * @param intercept the one argument method lambda
		 * @param name the name of the method with one argument
		 */
		<A, R> InterceptBuilder<T> intercept(FunctionWithException<A, R> intercept, String name, Class<A> type);

		/**
		 * Intercept a two argument method call
		 *
		 * @param intercept the two argument method lambda
		 * @param name the name of the method with two arguments
		 */
		<A, B, R> InterceptBuilder<T> intercept(BiFunctionWithException<A, B, R> intercept, String name, Class<A> aType,
			Class<B> bType);

		/**
		 * Provide a function that is called with the method calling function.
		 * The around can setup some function around the call, which is passed
		 * as a callable. The around should setup whatever it wants to setup,
		 * call the callable, tear down what was setup and then return the
		 * result of the callable. Exceptions can be passed upwards.
		 *
		 * @param around the around advice (the argument is a callable that
		 *            should be calle
		 */
		InterceptBuilder<T> around(BiFunctionWithException<Invocation, Callable<Object>, Object> around);

		/**
		 * Provide a function that is called before the method is called. The
		 * argument to the Consumer is the array of arguments (which is never
		 * null). The consumer may change these arguments but should of course
		 * be extremely careful to not change the types in a way that would make
		 * the method call impossible.
		 *
		 * @param before advice
		 */
		InterceptBuilder<T> before(ConsumerWithException<Invocation> before);

		/**
		 * Provide a function that is called after the method is called. The
		 * given parameter to the function are the arguments and the result of
		 * the previous methods. The function should return the result, modified
		 * if so needed.
		 *
		 * @param after advice
		 */
		InterceptBuilder<T> after(BiFunctionWithException<Invocation, Object, Object> after);

		/**
		 * Called when an exception occurs
		 *
		 * @param exception the throw exception
		 */
		InterceptBuilder<T> onException(BiFunctionWithException<Invocation, Throwable, Object> exception);

		/**
		 * Build the proxy
		 */
		T build();
	}

	/**
	 * Create an intercepting proxy using a builder
	 *
	 * @param type the type of the proxy
	 * @param delegate the delegate to delegate to
	 * @return a builder
	 */
	@SuppressWarnings("unchecked")
	public static <T> InterceptBuilder<T> intercept(Class<T> type, T delegate) {

		assert Objects.nonNull(type);
		assert Objects.nonNull(delegate);

		return new InterceptBuilder<T>() {
			final Map<Method, FunctionWithException<Invocation, Object>>	methods		= new HashMap<>();
			ConsumerWithException<Invocation>								before		= null;
			BiFunctionWithException<Invocation, Callable<Object>, Object>	around		= (x, c) -> c.call();
			BiFunctionWithException<Invocation, Object, Object>				after		= null;
			BiFunctionWithException<Invocation, Throwable, Object>			exceptions	= null;

			{
				try {

					for (Method m : type.getMethods()) {
						methods.putIfAbsent(m, (inv) -> m.invoke(delegate, inv.args));
					}

				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

			@Override
			public InterceptBuilder<T> intercept(FunctionWithException<Invocation, Object> intercept, String name,
				Class<?>... types) {
				try {
					Method method = type.getMethod(name, types);
					methods.put(method, intercept);
					return this;
				} catch (NoSuchMethodException nsme) {
					try {
						Method method = Object.class.getMethod(name, types);
						methods.put(method, intercept);
					} catch (NoSuchMethodException e) {
						throw Exceptions.duck(e);
					}
					return this;
				} catch (Throwable e) {
					throw Exceptions.duck(e);
				}
			}

			@Override
			public <R> InterceptBuilder<T> intercept(RunnableWithException intercept, String name) {
				return intercept((inv) -> {
					intercept.run();
					return null;
				}, name);
			}

			@Override
			public <R> InterceptBuilder<T> intercept(SupplierWithException<R> intercept, String name) {
				return intercept((Invocation inv) -> intercept.get(), name);
			}

			@Override
			public <A, R> InterceptBuilder<T> intercept(FunctionWithException<A, R> intercept, String name,
				Class<A> aType) {
				return intercept((Invocation inv) -> intercept.apply((A) inv.args[0]), name, aType);
			}

			@Override
			public <A, B, R> InterceptBuilder<T> intercept(BiFunctionWithException<A, B, R> intercept, String name,
				Class<A> aType, Class<B> bType) {
				return intercept((Invocation inv) -> intercept.apply((A) inv.args[0], (B) inv.args[1]), name, aType,
					bType);
			}

			@Override
			public InterceptBuilder<T> around(BiFunctionWithException<Invocation, Callable<Object>, Object> around) {
				if (this.around == null) {
					this.around = around;
				} else {
					BiFunctionWithException<Invocation, Callable<Object>, Object> previous = this.around;
					this.around = (inv, callable) -> around.apply(inv, () -> previous.apply(inv, callable));
				}
				return this;
			}

			@Override
			public InterceptBuilder<T> before(ConsumerWithException<Invocation> before) {
				if (this.before == null)
					this.before = before;
				else {
					ConsumerWithException<Invocation> previous = this.before;
					this.before = (args) -> {
						previous.accept(args);
						before.accept(args);

					};
				}
				return this;
			}

			@Override
			public InterceptBuilder<T> after(BiFunctionWithException<Invocation, Object, Object> after) {
				if (this.after == null)
					this.after = after;
				else {
					BiFunctionWithException<Invocation, Object, Object> previous = this.after;
					this.after = (args, result) -> {
						result = previous.apply(args, result);
						return after.apply(args, result);
					};
				}
				return this;
			}

			@Override
			public InterceptBuilder<T> onException(BiFunctionWithException<Invocation, Throwable, Object> exceptions) {
				if (this.exceptions == null)
					this.exceptions = exceptions;
				else {
					BiFunctionWithException<Invocation, Throwable, Object> previous = this.exceptions;
					this.exceptions = (invocation, except) -> {
						Object result = previous.apply(invocation, except);
						if (result != NORETURN)
							return result;

						return exceptions.apply(invocation, except);
					};
				}
				return this;
			}

			@Override
			public T build() {
				try {

					if (before == null)
						this.before = (inv) -> {};

					if (after == null)
						this.after = (inv, x) -> x;

					if (exceptions == null)
						this.exceptions = (inv, exc) -> {
							throw Exceptions.duck(Exceptions.unrollCause(exc, InvocationTargetException.class));
						};

					return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
						type
					}, this::invoke);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}

			Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Invocation inv = new Invocation(proxy, method, args);
				try {
					before.accept(inv);

					FunctionWithException<Invocation, Object> target = methods.get(method);
					Object result = around.apply(inv, () -> target.apply(inv));

					return after.apply(inv, result);
				} catch (Throwable t) {
					Object result = exceptions.apply(inv, t);
					if (result == DEFAULT) {
						return Converter.cnv(method.getGenericReturnType(), null);
					}

					if (result != NORETURN)
						return result;

					throw t;
				}
			}

		};
	}

}
