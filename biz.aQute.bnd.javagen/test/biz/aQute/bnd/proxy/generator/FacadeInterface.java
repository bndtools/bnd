package biz.aQute.bnd.proxy.generator;

import java.net.NetworkInterface;

public interface FacadeInterface {

	void foo();

	int fooint();

	String fooString();

	void foo(String a1, NetworkInterface ni);
}
