package aQute.tester.bundle.engine;

import java.util.Optional;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import aQute.tester.bundle.engine.discovery.BundleEnginePruner;
import aQute.tester.bundle.engine.discovery.BundleSelectorResolver;

@ServiceProvider(value = TestEngine.class, resolution = Resolution.OPTIONAL)
public class BundleEngine implements TestEngine {

	public static final String				CHECK_UNRESOLVED	= "aQute.bnd.junit.bundle.engine.checkUnresolved";
	public static final String				PRUNE				= "aQute.bnd.junit.bundle.engine.prune";

	public static final String				ENGINE_ID			= "bnd-bundle-engine";

	private final Optional<BundleContext>	context;

	public BundleEngine() {
		context = Optional.ofNullable(FrameworkUtil.getBundle(BundleEngine.class))
			.map(Bundle::getBundleContext);
	}

	@Override
	public String getId() {
		return ENGINE_ID;
	}

	@Override
	public Optional<String> getGroupId() {
		return Optional.of("biz.aQute.bnd");
	}

	@Override
	public Optional<String> getArtifactId() {
		return Optional.of("biz.aQute.tester.junit-platform");
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
		BundleEngineDescriptor descriptor = new BundleEngineDescriptor(uniqueId);

		if (!context.isPresent()) {
			descriptor.addChild(new StaticFailureDescriptor(uniqueId.append("test", "noFramework"),
				"Initialization Error", new JUnitException("BundleEngine must run inside an OSGi framework")));
			return descriptor;
		}
		BundleSelectorResolver.resolve(context.get(), request, descriptor);

		Optional<String> prune = request.getConfigurationParameters()
			.get(PRUNE);

		if (prune.isPresent()) {
			try {
				new BundleEnginePruner(prune.get()).prune(descriptor);
			} catch (IllegalArgumentException e) {
				StaticFailureDescriptor sd = new StaticFailureDescriptor(descriptor.getUniqueId()
					.append("test", "invalidConfig"), "Malformed prune directive", e);
				descriptor.addChild(sd);
			}
		}
		return descriptor;
	}

	void dump(String indent, TestDescriptor desc) {
		System.err.println(indent + desc.getDisplayName());
		for (TestDescriptor child : desc.getChildren()) {
			dump(indent + "  ", child);
		}
	}

	@Override
	public void execute(ExecutionRequest request) {
		new BundleEngineExecutor(request).execute();
	}
}
