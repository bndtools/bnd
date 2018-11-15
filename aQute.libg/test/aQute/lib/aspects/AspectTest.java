package aQute.lib.aspects;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AspectTest {

	public interface A {
		int foo();

		void bar(long n);

		double dbl(int n);

		default void exception() {
			throw new IllegalArgumentException();
		}
	}

	@Test
	public void simple() {
		A build = Aspects.intercept(A.class, new A() {

			@Override
			public int foo() {
				return 42;
			}

			@Override
			public void bar(long n) {

			}

			@Override
			public double dbl(int n) {
				return n;
			}
		})
			.before((inv) -> System.out.println("before 1 " + inv.method.getName()))
			.before((inv) -> System.out.println("before 2 " + inv.method.getName()))
			.after((inv, r) -> {
				System.out.println("after 1 " + inv.method.getName() + " " + r);
				return r;
			})
			.after((inv, r) -> {
				System.out.println("after 2 " + inv.method.getName() + " " + r);
				return r;
			})
			.around((inv, callable) -> {
				Object r = null;
				try {
					System.out.println("around 1 " + inv.method.getName() + " before " + inv);
					r = callable.call();
				} finally {
					System.out.println("around 1 " + inv.method.getName() + " after -> " + r);
				}
				return r;
			})
			.around((inv, callable) -> {
				Object r = null;
				try {
					System.out.println("around 2 " + inv.method.getName() + " before " + inv);
					r = callable.call();
				} finally {
					System.out.println("around 2 " + inv.method.getName() + " after -> " + r);
				}
				return r;
			})
			.onException((inv, ex) -> {
				System.out.println("Exception " + ex);
				return Aspects.DEFAULT;
			})
			.intercept(() -> 55, "foo")
			.build();

		assertEquals(55, build.foo());
		assertEquals(8, build.dbl(8), 0.1);
		build.bar(1);
		build.exception();
	}
}
