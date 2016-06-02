package aQute.lib.exceptions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Curry {
	final Object	target;
	final Method	method;

	public Curry(Object target, String method, Class< ? >... c) {
		try {
			this.target = target;
			this.method = target.getClass().getMethod(method, c);
		} catch (NoSuchMethodException e) {
			throw Exceptions.duck(e);
		}
	}

	Runnable run(final Object... args) {
		return new Runnable() {

			@Override
			public void run() {
				invoke(args);
			}

		};
	}

	void invoke(final Object... args) {
		try {
			method.invoke(target, args);
		} catch (IllegalAccessException e) {
			throw Exceptions.duck(e);
		} catch (InvocationTargetException e) {
			throw Exceptions.duck(e);
		}
	}

	public <T, R> org.osgi.util.function.Function<T,R> function(final Object... args) {
		return new org.osgi.util.function.Function<T,R>() {

			@SuppressWarnings("unchecked")
			@Override
			public R apply(T t) {
				try {
					Object[] xargs = new Object[args.length + 1];
					xargs[0] = t;
					System.arraycopy(args, 0, xargs, 1, args.length);
					return (R) method.invoke(target, xargs);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			}
		};
	}

	public static class V extends Curry implements Runnable {

		public V(Object target, String method) {
			super(target, method);
		}

		@Override
		public void run() {
			invoke();
		}
	}

	public static class R1<A> extends Curry {

		public R1(Object target, String method, Class<A> a) {
			super(target, method, a);
		}

		public Runnable run(A a) {
			return super.run(a);
		}
	}

	public static class F1<R, A> extends R1<A> {

		public F1(Object target, String method, Class<A> a) {
			super(target, method, a);
		}

		public org.osgi.util.function.Function<A,R> function() {
			return super.function();
		}
	}

	public static class R2<A, B> extends Curry {

		public R2(Object target, String method, Class<A> a, Class<B> b) {
			super(target, method, a, b);
		}

		public Runnable run(A a, B b) {
			return super.run(a, b);
		}
	}

	public static class F2<R, A, B> extends R2<A,B> {

		public F2(Object target, String method, Class<A> a, Class<B> b) {
			super(target, method, a, b);
		}

		public org.osgi.util.function.Function<A,R> function(B a) {
			return super.function(a);
		}
	}

	public static class R3<A, B, C> extends Curry {

		public R3(Object target, String method, Class<A> a, Class<B> b, Class<C> c) {
			super(target, method, a, b, c);
		}

		public Runnable run(A a, B b, C c) {
			return super.run(a, b, c);
		}
	}

	public static class F3<R, A, B, C> extends R3<A,B,C> {

		public F3(Object target, String method, Class<A> a, Class<B> b, Class<C> c) {
			super(target, method, a, b, c);
		}

		public org.osgi.util.function.Function<A,R> function(B b, C c) {
			return super.function(b, c);
		}
	}

	public static class R4<A, B, C, D> extends Curry {

		public R4(Object target, String method, Class<A> a, Class<B> b, Class<C> c, Class<D> d) {
			super(target, method, a, b, c, d);
		}

		public Runnable run(A a, B b, C c, D d) {
			return super.run(a, b, c, d);
		}
	}

	public static class F4<R, A, B, C, D> extends R4<A,B,C,D> {

		public F4(Object target, String method, Class<A> a, Class<B> b, Class<C> c, Class<D> d) {
			super(target, method, a, b, c, d);
		}

		public org.osgi.util.function.Function<A,R> function(B b, C c, D d) {
			return super.function(b, c);
		}
	}

}
