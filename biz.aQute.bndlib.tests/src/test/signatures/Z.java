package test.signatures;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;

class X<A> {}

interface Y<B> {}

public class Z<C> extends X<String> implements Y<Integer> {

	public class V<D> {
		public <E> void fooLCO(@SuppressWarnings("unused") E e, @SuppressWarnings("unused") C c,
			@SuppressWarnings("unused") D d) {}
	}

	public abstract class U extends Z<String> implements Cloneable, Serializable {
		private static final long serialVersionUID = 1L;
	}

	// Constructors
	public Z() {}

	public Z(@SuppressWarnings("unused") C c) {}

	// Fields
	public X<Y<C>>				field;
	public Z<Long>.V<Integer>	referenceToNestedClass;
	public V<C>					vc	= new V<>();

	// Methods
	public <T> void method() {}

	// Test all possible declarations
	public <E> void foo(@SuppressWarnings("unused") E e) {}

	public <E extends InputStream & Cloneable> void fooCI(@SuppressWarnings("unused") E e) {}

	public <E extends InputStream & Cloneable & Serializable> void fooCII(@SuppressWarnings("unused") E e) {}

	public <E extends Serializable & Cloneable> void fooII(@SuppressWarnings("unused") E e) {}

	public <E extends Cloneable> void fooI(@SuppressWarnings("unused") E e) {}

	public <E extends InputStream> void fooC(@SuppressWarnings("unused") E e) {}

	public <E extends C> void fooP(@SuppressWarnings("unused") E e) {}

	public <E, F> void foo(@SuppressWarnings("unused") E e, @SuppressWarnings("unused") F f) {}

	// test with variable in signature
	public <E> void fooLC(@SuppressWarnings("unused") E e, @SuppressWarnings("unused") C f) {}

	// test wildcards
	public Collection<?>							wildcard_001;
	public Collection<? extends Cloneable>			wildcard_002;
	public Collection<? super Cloneable>			wildcard_003;
	public Collection<? extends C>					wildcard_004;
	public Collection<? super C>					wildcard_005;
	public Collection<? extends Z<C>.V<Integer>>	wildcard_006;
	public Collection<? super Z<C>.V<Integer>>		wildcard_007;

	// test compatibility
	public static <E extends Cloneable> Collection<E> compatibility_001() {
		return null;
	}

	public static <F extends Cloneable> Collection<F> compatibility_002() {
		return null;
	}

	public static <F extends InputStream> Collection<F> compatibility_003() {
		return null;
	}

	public C[] typevarArray;

}
