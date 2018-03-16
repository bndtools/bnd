package aQute.remote.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SystemIO implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Hello World");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);

		System.out.println(br.readLine()
			.trim()
			.toUpperCase());
	}

}
