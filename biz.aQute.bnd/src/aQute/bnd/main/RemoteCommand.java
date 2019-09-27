package aQute.bnd.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.resource.Resource;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.service.repository.ContentNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.Version;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.remote.api.Agent;
import aQute.remote.api.Event;
import aQute.remote.api.Supervisor;
import aQute.remote.util.AgentSupervisor;

class RemoteCommand extends Processor {
	private final static Logger					logger				= LoggerFactory.getLogger(RemoteCommand.class);
	private static TypeReference<List<Version>>	tref				= new TypeReference<List<Version>>() {};
	private Yaml								y					= new Yaml();
	private bnd									bnd;
	private LauncherSupervisor					launcher			= new LauncherSupervisor();
	private Agent								agent;
	private int									port;
	private String								host;
	private static Set<String>					IGNORED_NAMESPACES	= new HashSet<>();

	static {
		IGNORED_NAMESPACES.add(PackageNamespace.PACKAGE_NAMESPACE); // handled
																	// specially
		IGNORED_NAMESPACES.add(ContentNamespace.CONTENT_NAMESPACE);
	}

	/**
	 * This is the supervisor on the bnd launcher side. It provides the SHA
	 * repository for the agent and handles the redirection. It also handles the
	 * events.
	 */
	class LauncherSupervisor extends AgentSupervisor<Supervisor, Agent> implements Supervisor {

		@Override
		public boolean stdout(String out) throws Exception {
			System.out.print(out);
			return true;
		}

		@Override
		public boolean stderr(String out) throws Exception {
			System.err.print(out);
			return true;
		}

		public void connect(String host, int port) throws Exception {
			super.connect(Agent.class, this, host, port);
		}

		@Override
		public void event(Event e) throws Exception {
			System.out.println(e);
		}

	}

	@Description("Communicates with the remote agent")
	interface RemoteOptions extends Options {
		@Description("Specify the host to commicate with, default is 'localhost'")
		String host(String deflt);

		@Description("Specify the port to commicate with, default is " + Agent.DEFAULT_PORT)
		int port(int deflt);
	}

	RemoteCommand(bnd bnd, RemoteOptions options) throws Exception {
		super(bnd);
		this.bnd = bnd;
		use(bnd);
		launcher = new LauncherSupervisor();
		launcher.connect(host = options.host("localhost"), port = options.port(Agent.DEFAULT_PORT));
		agent = launcher.getAgent();
	}

	@Description("Communicate with the remote framework to list the installed bundles")
	interface ListOptions extends Options {
		@Description("Specify to return the output as JSON")
		boolean json();
	}

	@Description("List the bundles installed in the remote framework")
	public void _list(ListOptions options) throws Exception {
		List<BundleDTO> installedBundles = agent.getBundles();
		outputAs(options.json(), installedBundles);
	}

	private void outputAs(boolean isJsonOutput, List<BundleDTO> bundles) throws IOException, Exception {
		if (isJsonOutput) {
			new JSONCodec().enc()
				.to((OutputStream) bnd.out)
				.put(bundles)
				.flush();
		} else {
			bundles.stream()
				.map(b -> b.symbolicName + "-" + b.version)
				.forEach(bnd.out::println);
		}
	}

	@Description("Communicate with the remote framework to perform bundle operation")
	@Arguments(arg = {
		"bundleId..."
	})
	interface BundleOptions extends Options {}

	@Description("Start the specified bundles")
	public void _start(BundleOptions options) throws Exception {
		long[] ids = Converter.cnv(long[].class, options._arguments());
		agent.start(ids);
	}

	@Description("Stop the specified bundles")
	public void _stop(BundleOptions options) throws Exception {
		long[] ids = Converter.cnv(long[].class, options._arguments());
		agent.stop(ids);
	}

	@Description("Uninstall the specified bundles")
	public void _uninstall(BundleOptions options) throws Exception {
		long[] ids = Converter.cnv(long[].class, options._arguments());
		agent.uninstall(ids);
	}

	@Description("Communicate with the remote framework to install or update bundle")
	@Arguments(arg = {
		"filespec..."
	})
	interface InstallOptions extends Options {
		@Description("By default the location is 'manual:<bsn>'. You can specify multiple locations when installing multiple bundles")
		String[] location();
	}

	@Description("Install/update the specified bundle.")
	public void _install(InstallOptions options) throws Exception {
		List<String> args = options._arguments();

		if (args.isEmpty()) {
			error("No bundles specified");
			return;
		}

		String[] location = options.location();

		int n = 0;
		for (String fileSpec : args) {
			URI uri = IO.work.toURI();
			URL url = uri.resolve(fileSpec)
				.toURL();
			byte data[] = IO.read(url);
			String l = location == null || location.length <= n ? null : location[n];
			BundleDTO dto = agent.installWithData(l, data);
			bnd.out.println(dto);
			n++;
		}
	}

	@Override
	public void close() throws IOException {
		launcher.close();
	}

	@Description("Get the framework info")
	@Arguments(arg = {})
	interface FrameworkOptions extends Options {}

	public void _framework(FrameworkOptions opts) throws Exception {
		dump(agent.getFramework());
	}

	@Description("Get the bundle revisions")
	@Arguments(arg = {
		"bundleid..."
	})
	interface RevisonOptions extends Options {}

	public void _revisions(RevisonOptions opts) throws Exception {
		long[] ids = Converter.cnv(long[].class, opts._arguments());
		dump(agent.getBundleRevisons(ids));
	}

	@Description("Ping the remote framework")
	@Arguments(arg = {})
	interface PingOptions extends Options {}

	public void _ping(PingOptions opts) throws Exception {
		long start = System.currentTimeMillis();
		if (agent.ping())
			bnd.out.println("Ok " + (System.currentTimeMillis() - start) + "ms");
		else
			bnd.out.println("Could not reach " + host + ":" + port);
	}

	/**
	 * Create a distro from a remote agent
	 */

	@Description("Create a distro jar (or xml) from a remote agent")
	@Arguments(arg = {
		"bsn", "[version]"
	})
	interface DistroOptions extends Options {
		@Description("The Bundle-Vendor header")
		String vendor();

		@Description("The Bundle-Description header")
		String description();

		@Description("The Bundle-Copyright header")
		String copyright();

		@Description("The Bundle-License header")
		String license();

		@Description("Output name")
		String output(String deflt);

		@Description("Generate xml instead of a jar with manifest")
		boolean xml();
	}

	public void _distro(DistroOptions opts) throws Exception {
		List<String> args = opts._arguments();
		String bsn;
		String version;

		bsn = args.remove(0);

		if (!Verifier.isBsn(bsn)) {
			error("Not a bundle symbolic name %s", bsn);
		}

		if (args.isEmpty())
			version = "0";
		else {
			version = args.remove(0);
			if (!Version.isVersion(version)) {
				error("Invalid version %s", version);
			}
		}

		File output = getFile(opts.output(opts.xml() ? bsn + ".xml" : "distro.jar"));
		if (output.getParentFile() == null || !output.getParentFile()
			.isDirectory()) {
			error("Cannot write to %s because parent not a directory", output);
		}

		if (output.isFile() && !output.canWrite()) {
			error("Cannot write to %s", output);
		}

		logger.debug("Starting distro {};{}", bsn, version);

		List<BundleRevisionDTO> bundleRevisons = agent.getBundleRevisons();
		logger.debug("Found {} bundle revisions", bundleRevisons.size());

		Parameters packages = new Parameters();
		List<Parameters> provided = new ArrayList<>();

		ResourceBuilder resourceBuilder = new ResourceBuilder();

		for (BundleRevisionDTO brd : bundleRevisons) {
			for (CapabilityDTO capabilityDTO : brd.capabilities) {
				CapabilityBuilder capabilityBuilder = new CapabilityBuilder(capabilityDTO.namespace);

				//
				// We need to fixup versions :-(
				// Versions are encoded as strings in DTOs
				// and that means we need to treat the version key
				// special
				//

				for (Entry<String, Object> e : capabilityDTO.attributes.entrySet()) {
					String key = e.getKey();
					Object value = e.getValue();

					if (key.equals("version")) {
						if (value instanceof Collection || value.getClass()
							.isArray())
							value = Converter.cnv(tref, value);
						else
							value = new Version((String) value);
					}
					capabilityBuilder.addAttribute(key, value);
				}
				capabilityBuilder.addDirectives(capabilityDTO.directives);

				Attrs attrs = capabilityBuilder.toAttrs();

				resourceBuilder.addCapability(capabilityBuilder);

				if (capabilityBuilder.isPackage()) {
					attrs.remove(Constants.BUNDLE_SYMBOLIC_NAME_ATTRIBUTE);
					attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
					String pname = attrs.remove(PackageNamespace.PACKAGE_NAMESPACE);
					if (pname == null) {
						warning("Invalid package capability found %s", capabilityDTO);
					} else if (packages.containsKey(pname)) {
						Attrs existing = packages.get(pname);
						Version existingPackageVersion = existing.getTyped(Attrs.VERSION, "version");
						Version newPackageVersion = attrs.getTyped(Attrs.VERSION, "version");
						if ((existingPackageVersion != null) && (newPackageVersion != null)
							&& newPackageVersion.compareTo(existingPackageVersion) > 0) {
							packages.put(pname, attrs);
						}
					} else
						packages.put(pname, attrs);
					logger.debug("P: {};{}", pname, attrs);
				} else if (NativeNamespace.NATIVE_NAMESPACE.equals(capabilityDTO.namespace)) {
					Attrs newAttrs = new Attrs();
					for (Entry<String, String> entry : attrs.entrySet()) {
						if (entry.getKey()
							.startsWith(NativeNamespace.NATIVE_NAMESPACE)) {
							newAttrs.put(entry.getKey(), entry.getValue());
						}
					}
					Parameters p = new Parameters();
					p.put(capabilityDTO.namespace, newAttrs);
					provided.add(p);
				} else if (!IGNORED_NAMESPACES.contains(capabilityDTO.namespace)) {
					logger.debug("C {};{}", capabilityDTO.namespace, attrs);
					Parameters p = new Parameters();
					p.put(capabilityDTO.namespace, attrs);
					provided.add(p);
				}
			}
		}

		if (isOk()) {
			if (opts.xml()) {
				buildXML(bsn + ":" + version, output, resourceBuilder.build());
			} else {
				buildjar(opts, bsn, version, output, packages, provided, resourceBuilder.build());
			}
		}
	}

	public void buildXML(String name, File output, Resource resource) throws IOException {
		XMLResourceGenerator xrg = getGenerator(name, resource);
		xrg.save(output);
	}

	public XMLResourceGenerator getGenerator(String name, Resource resource) {
		XMLResourceGenerator xrg = new XMLResourceGenerator().name(name)
			.resource(resource);
		return xrg;
	}

	public void buildjar(DistroOptions opts, String bsn, String version, File output, Parameters packages,
		List<Parameters> provided, Resource resource) throws Exception, IOException {
		Manifest m = new Manifest();
		Attributes main = m.getMainAttributes();

		main.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");

		main.putValue(Constants.BUNDLE_SYMBOLICNAME, bsn);
		main.putValue(Constants.BUNDLE_VERSION, version);

		main.putValue(Constants.EXPORT_PACKAGE, packages.toString());

		// Make distro unresolvable
		Parameters unresolveable = new Parameters(
			"osgi.unresolvable; filter:='(&(must.not.resolve=*)(!(must.not.resolve=*)))'");
		main.putValue(Constants.REQUIRE_CAPABILITY, unresolveable.toString());

		provided.add(new Parameters("osgi.unresolvable"));

		StringBuilder sb = new StringBuilder();

		for (Parameters parameter : provided) {
			sb.append(parameter.toString());
			sb.append(",");
		}

		String capabilities = sb.toString()
			.substring(0, sb.length() - 1);

		main.putValue(Constants.PROVIDE_CAPABILITY, capabilities);

		if (opts.description() != null)
			main.putValue(Constants.BUNDLE_DESCRIPTION, opts.description());
		if (opts.license() != null)
			main.putValue(Constants.BUNDLE_LICENSE, opts.license());
		if (opts.copyright() != null)
			main.putValue(Constants.BUNDLE_COPYRIGHT, opts.copyright());
		if (opts.vendor() != null)
			main.putValue(Constants.BUNDLE_VENDOR, opts.vendor());

		try (Jar jar = new Jar(bsn + ":" + version)) {
			jar.setManifest(m);

			XMLResourceGenerator generator = getGenerator(bsn + ":" + version, resource);
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			generator.save(bout);

			EmbeddedResource r = new EmbeddedResource(bout.toByteArray(), System.currentTimeMillis());
			jar.putResource("OSGI-OPT/obr.xml", r);

			try (Verifier v = new Verifier(jar)) {
				v.setProperty(Constants.FIXUPMESSAGES,
					"osgi.* namespace must not be specified with generic capabilities");
				v.verify();
				v.getErrors();

				if (isFailOk() || v.isOk()) {
					jar.updateModified(System.currentTimeMillis(), "Writing distro jar");
					jar.write(output);
				} else
					getInfo(v);
			}
		}
	}

	private void dump(Object o) {
		y.dump(o, new OutputStreamWriter(bnd.out));
	}

}
