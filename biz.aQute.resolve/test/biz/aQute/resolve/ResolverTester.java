package biz.aQute.resolve;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.log.LogService;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.service.resource.SupportingResource;
import aQute.libg.reporter.ReporterAdapter;

public class ResolverTester extends BndrunResolveContext {

	static final LogService			log		= new LogReporter(new ReporterAdapter(System.out));
	final BndEditModel				model;
	final String					descriptor;
	final Map<Resource, Wiring>		wiring	= new HashMap<>();
	final Map<String, Capability>	index	= new HashMap<>();
	final Resource					systemResource;
	private ResourcesRepository		repo;

	public ResolverTester(String test) throws Exception {
		this(test, new BndEditModel());
	}

	public ResolverTester(String descriptor, BndEditModel model) throws Exception {
		super(model.getProperties(), model.getProject(), model.getProperties(), log);
		this.model = model;
		this.descriptor = descriptor;

		Map<String, ResourceBuilder> repoContent = new HashMap<>();

		ResourceBuilder system = new ResourceBuilder();
		ResourceBuilder wirings = new ResourceBuilder();

		Parameters caps = new Parameters(descriptor);
		caps.forEach((k, v) -> {
			String ns = Processor.removeDuplicateMarker(k);
			CapabilityBuilder cb = new CapabilityBuilder(ns);
			cb.addAttributes(v);

			String where = v.get("where");
			assert where != null;

			switch (where) {
				case "sys" :
					system.addCapability(cb);
					break;
				case "wir" :
					wirings.addCapability(cb);
					break;

				default :
					ResourceBuilder resource = repoContent.computeIfAbsent(where, b -> new ResourceBuilder());
					resource.addCapability(cb);
					break;
			}
		});

		repo = new ResourcesRepository();
		repoContent.forEach((k, v) -> {
			SupportingResource resource = v.build();
			repo.add(resource);
			index(resource);
		});

		Map<Integer, Capability> capIndex = new HashMap<>();
		systemResource = system.build();
		index(systemResource);

		SupportingResource wiring = wirings.build();
		index(wiring);
		this.wiring.put(wiring, new Wiring() {

			@Override
			public List<Capability> getResourceCapabilities(String namespace) {
				return wiring.getCapabilities(namespace);
			}

			@Override
			public List<Requirement> getResourceRequirements(String namespace) {
				return wiring.getRequirements(namespace);
			}

			@Override
			public List<Wire> getProvidedResourceWires(String namespace) {
				return Collections.emptyList();
			}

			@Override
			public List<Wire> getRequiredResourceWires(String namespace) {
				return Collections.emptyList();
			}

			@Override
			public Resource getResource() {
				return wiring;
			}
		});
		init();
	}

	private void index(Resource r) {
		r.getCapabilities(null)
			.forEach(c -> {
				String object = (String) c.getAttributes()
					.get("id");
				if (object == null)
					return;
				index.put(object, c);
			});
	}

	@Override
	public Map<Resource, Wiring> getWirings() {
		return wiring;
	}

	@Override
	public synchronized void init() {
		setSystemResource(systemResource);
		addRepository(repo);
	}

	public List<String> sortedCapabilities(String... ids) {
		return Stream.of(ids)
			.map(id -> index.get(id))
			.filter(Objects::nonNull)
			.sorted(capabilityComparator)
			.map(c -> (String) c.getAttributes()
				.get("id"))
			.toList();
	}
}
