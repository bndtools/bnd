package reference.runtime.annotations;

import runtime.annotations.RuntimeTypeAnnotation;

public interface Foo {
	String foo(int p) throws @RuntimeTypeAnnotation Exception;
}
