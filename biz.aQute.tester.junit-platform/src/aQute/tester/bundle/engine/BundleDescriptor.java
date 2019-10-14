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

	public BundleDescriptor(Bundle bundle, UniqueId uniqueId) {
		this(bundle, uniqueId, null);
	}

	public BundleDescriptor(Bundle bundle, UniqueId uniqueId, BundleException bundleException) {
		super(uniqueId, displayNameOf(bundle));
		this.bundle = bundle;
		this.bundleException = bundleException;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void addEngine(TestDescriptor descriptor, TestEngine engine) {
		engineMap.put(descriptor, engine);
		addChild(descriptor);
	}

	public TestEngine getEngineFor(TestDescriptor descriptor) {
		return engineMap.get(descriptor);
	}

	@Override
	public Type getType() {
		return bundleException == null ? Type.CONTAINER : Type.TEST;
	}

	public BundleException getException() {
		return bundleException;
	}
}
