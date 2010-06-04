package com.example.demo;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

	public void start(BundleContext arg0) throws Exception {
		System.out.println("Hello World");
	}

	public void stop(BundleContext arg0) throws Exception {
		System.out.println("Goodbye World");
	}

}
