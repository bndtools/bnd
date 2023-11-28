package biz.aQute.bnd.proxy.generator;

import java.net.NetworkInterface;
import java.util.Map;

public interface FacadeInterface {

	void clean(Map<String, String> args) throws Exception;

	void foo();

	int fooint();

	String fooString();

	void foo(String a1, NetworkInterface ni);


}
