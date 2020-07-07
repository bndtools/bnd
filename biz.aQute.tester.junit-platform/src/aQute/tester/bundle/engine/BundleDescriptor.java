package aQute.tester.bundle.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BundleDescriptor extends AbstractTestDescriptor {

	final private Bundle							bundle;
	final private BundleException					bundleException;

	public static String displayNameOf(Bundle bundle) {
		return "[" + bundle.getBundleId() + "] " + bundle.getSymbolicName() + ';'
			+ bundle.getVersion();
	}

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

	@Override
	public Type getType() {
		return bundleException == null ? Type.CONTAINER : Type.TEST;
	}

	public BundleException getException() {
		return bundleException;
	}
}
