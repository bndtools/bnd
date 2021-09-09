package biz.aQute.bndall.tests.plugin_1;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;

import aQute.bnd.service.externalplugin.ExternalPlugin;

@ExternalPlugin(name = "hellocallable", objectClass = Callable.class)
public class CallablePlugin implements Callable<String>, Closeable {
	boolean closed;

	@Override
	public String call() throws Exception {
		if (closed)
			return "goodbye";
		else
			return "hello";
	}

	@Override
	public void close() throws IOException {
		System.out.println("closed");
		closed = true;
	}

}
