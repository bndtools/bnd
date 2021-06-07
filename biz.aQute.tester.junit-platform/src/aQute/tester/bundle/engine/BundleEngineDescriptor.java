package aQute.tester.bundle.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

public class BundleEngineDescriptor extends EngineDescriptor {
	public BundleEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, "Bnd JUnit Platform Bundle Engine");
	}
}
