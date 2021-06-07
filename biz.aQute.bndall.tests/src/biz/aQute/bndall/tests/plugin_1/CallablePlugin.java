package biz.aQute.bndall.tests.plugin_1;

import java.util.concurrent.Callable;

import aQute.bnd.service.externalplugin.ExternalPlugin;

@ExternalPlugin(name = "hellocallable", objectClass = Callable.class)
public class CallablePlugin implements Callable<String> {

	@Override
	public String call() throws Exception {
		return "hello";
	}

}
