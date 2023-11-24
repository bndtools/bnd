package biz.aQute.bnd.proxy.generator;

import java.util.List;
import java.util.Set;

public abstract class FacadeClass extends FacadeSuperClass {
	@Override
	public final String finalMethod() {
		return "final";
	}

	@Override
	public void bar() {

	}

	public <T, E extends T> List<T> generics(Class<?> foo, Set<? super E> s) throws Exception {
		return null;
	}
}
