package biz.aQute.resolve;

import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;

import aQute.bnd.build.model.EE;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.VersionRange;

/**
 * This Resolve Context is for testing purposes
 */
public class GenericResolveContext extends AbstractResolveContext {

	protected static final String	CONTRACT_OSGI_FRAMEWORK		= "OSGiFramework";
	protected static final String	IDENTITY_INITIAL_RESOURCE	= "<<INITIAL>>";
	protected static final String	IDENTITY_SYSTEM_RESOURCE	= "<<SYSTEM>>";

	private boolean					initialized					= false;
	private ResourceBuilder			system						= new ResourceBuilder();
	private ResourceBuilder			input						= new ResourceBuilder();

	public GenericResolveContext(LogService log) {
		super(log);
	}

	@Override
	public synchronized void init() {
		if (initialized)
			return;

		try {
			initialized = true;

			setInputResource(input.build());
			setSystemResource(system.build());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		super.init();
	}

	public ResourceBuilder getInput() {
		return input;
	}

	public ResourceBuilder getSystem() {
		return system;
	}

	public void addFramework(String framework, String version) throws Exception {
		Resource r = getHighestResource(framework, version);
		setFramework(system, r);
	}

	public void addEE(EE ee) throws Exception {
		system.addEE(ee);
	}

	public void addRequireBundle(String bsn, VersionRange versionRange) throws Exception {
		input.addRequireBundle(bsn, versionRange);
	}

	public void addCapability(String namespace, Attrs attrs) {
		CapabilityBuilder cb = new CapabilityBuilder(namespace);
		cb.addAttributesOrDirectives(attrs);
		system.addCapability(cb);
	}

	public void done() {
		init();
	}
}
