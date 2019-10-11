package aQute.tester.bundle.engine;

import static aQute.tester.bundle.engine.discovery.BundleSelectorResolver.displayNameOf;

import java.util.HashMap;
import java.util.Map;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BundleDescriptor extends AbstractTestDescriptor {

	final private Bundle							bundle;
	final private BundleException					bundleException;
	final private Map<TestDescriptor, TestEngine>	engineMap	= new HashMap<>();

	public BundleDescriptor(Bundle b, UniqueId uniqueId) {
		this(b, uniqueId, null);
	}

	public BundleDescriptor(Bundle b, UniqueId uniqueId, BundleException bundleException) {
		super(uniqueId, displayNameOf(b));
		bundle = b;
		this.bundleException = bundleException;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void addEngine(TestDescriptor d, TestEngine e) {
		engineMap.put(d, e);
		addChild(d);
	}

	public TestEngine getEngineFor(TestDescriptor d) {
		return engineMap.get(d);
	}

	@Override
	public Type getType() {
		return bundleException == null ? Type.CONTAINER : Type.TEST;
	}

	public BundleException getException() {
		return bundleException;
	}
}
