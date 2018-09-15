package test;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Collection;

import junit.framework.TestCase;

public class CompareTest<O extends Serializable> extends TestCase {

	interface I<T> {}

	class B {
		class C<X extends O> {}
	}

	class A0 {}

	// Declarations
	class A1<T> { //
	}

	class A2<T extends Serializable> { //
	}

	class A3<T extends Exception & Serializable> { //
	}

	public static String foo0() {
		return null;
	}

	public static <T> T foo1() {
		return null;
	}

	public static <T extends Serializable> T foo2() {
		return null;
	}

	public static <T extends Exception & I<String>> T foo3() {
		return null;
	}

	@SuppressWarnings("static-method")
	public <T extends O> T foo4() {
		return null;
	}

	public void testGenericDeclaration() throws Exception {
		assertTrue(A0.class.getTypeParameters().length == 0);

		assertTrue(A1.class.getTypeParameters().length == 1);
		assertEquals("T", A1.class.getTypeParameters()[0].getName());
		assertEquals(1, A1.class.getTypeParameters()[0].getBounds().length);
		assertEquals(Object.class, A1.class.getTypeParameters()[0].getBounds()[0]);

		assertTrue(A2.class.getTypeParameters().length == 1);
		assertEquals("T", A2.class.getTypeParameters()[0].getName());
		assertEquals(1, A2.class.getTypeParameters()[0].getBounds().length);
		assertEquals(Serializable.class, A2.class.getTypeParameters()[0].getBounds()[0]);

		assertTrue(A3.class.getTypeParameters().length == 1);
		assertEquals("T", A3.class.getTypeParameters()[0].getName());
		assertEquals(2, A3.class.getTypeParameters()[0].getBounds().length);
		assertEquals(Exception.class, A3.class.getTypeParameters()[0].getBounds()[0]);
		assertEquals(Serializable.class, A3.class.getTypeParameters()[0].getBounds()[1]);

		Method m = getClass().getMethod("foo0");
		assertEquals(0, m.getTypeParameters().length);

		m = getClass().getMethod("foo1");
		assertEquals(1, m.getTypeParameters().length);
		assertEquals("T", m.getTypeParameters()[0].getName());
		assertEquals(1, m.getTypeParameters()[0].getBounds().length);
		assertEquals(Object.class, m.getTypeParameters()[0].getBounds()[0]);

		m = getClass().getMethod("foo3");
		assertEquals(1, m.getTypeParameters().length);
		assertEquals("T", m.getTypeParameters()[0].getName());
		assertEquals(2, m.getTypeParameters()[0].getBounds().length);
		assertEquals(Exception.class, m.getTypeParameters()[0].getBounds()[0]);

		m = getClass().getMethod("foo4");
		assertEquals(1, m.getTypeParameters().length);
		assertEquals("T", m.getTypeParameters()[0].getName());
		assertEquals(1, m.getTypeParameters()[0].getBounds().length);
		assertTrue(m.getTypeParameters()[0].getBounds()[0] instanceof TypeVariable);
		assertEquals(getClass(), ((TypeVariable<?>) m.getTypeParameters()[0].getBounds()[0]).getGenericDeclaration());

	}

	public A1<Collection<String>[]> a1;

	@SuppressWarnings("static-method")
	<Y, X extends A1<Y>> A1<? extends X> bar() {
		return null;
	}

	public static void testSimple() throws IOException {

		// Scope root = new Scope(Access.PUBLIC, Kind.ROOT, ".");
		// RuntimeSignatureBuilder pc = new RuntimeSignatureBuilder(root);
		// pc.add(CompareTest.class);
		// pc.add(A.class);
		// pc.add(A.B.class);
		//
		// A a = new A<String, String, String, String, String>();
		//
		// pc.add(a.bar().getClass());
		// pc.add(a.bar().foo().getClass());
		//
		// // ParseSignatureBuilder pb = new ParseSignatureBuilder(root);
		// // pb.parse(new FileInputStream("bin_test/test/CompareTest$A$B$1.class"));
		// // pb.parse(new FileInputStream("bin_test/test/CompareTest$A$1.class"));
		// // pb.parse(new FileInputStream("bin_test/test/CompareTest$Z.class"));
		//
		// root.cleanRoot();
		// PrintWriter pw = new PrintWriter(System.err);
		// root.report(pw, 0);
		// pw.flush();
		//
		// root.prune(EnumSet.of(Access.PUBLIC, Access.PROTECTED));
		// root.report(pw, 0);
		// pw.flush();

	}
}
