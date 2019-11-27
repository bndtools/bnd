package aQute.bnd.osgi.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.stream.MapStream;
import aQute.lib.collections.MultiMap;

public class AugmentRepository extends BaseRepository {

	private final Repository					repository;
	private final Map<Capability, Capability>	wrapped					= new HashMap<>();
	private final List<Capability>				augmentedCapabilities	= new ArrayList<>();
	private final List<Resource>				augmentedBundles		= new ArrayList<>();

	public AugmentRepository(Parameters augments, Repository repository) throws Exception {
		this.repository = repository;
		init(augments);
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> fromRepos = repository.findProviders(requirements);

		for (Requirement requirement : requirements) {

			List<Capability> provided = new ArrayList<>();
			boolean replaced = false;

			for (Capability originalCapability : fromRepos.get(requirement)) {
				if (isValid(originalCapability)) {
					Capability wrappedCapability = wrapped.get(originalCapability);
					if (wrappedCapability != null) {
						provided.add(wrappedCapability);
						replaced = true;
					} else
						provided.add(originalCapability);
				}
			}

			Collection<Capability> additional = ResourceUtils.findProviders(requirement, augmentedCapabilities);
			replaced |= provided.addAll(additional);
			if (replaced)
				fromRepos.put(requirement, provided);
		}

		return fromRepos;
	}

	public boolean isValid(Capability capability) {
		return true;
	}

	static class Augment {
		Parameters	additionalRequirements;
		Parameters	additionalCapabilities;

	}

	private void init(Parameters augments) throws Exception {
		MultiMap<Requirement, Augment> operations = new MultiMap<>();

		MapStream.of(augments)
			.forEachOrdered((bsn, attrs) -> createAugmentOperation(operations, bsn, attrs));

		Map<Requirement, Collection<Capability>> allBundles = repository.findProviders(operations.keySet());

		for (Entry<Requirement, List<Augment>> e : operations.entrySet()) {
			executeAugmentOperations(allBundles, e.getKey(), e.getValue());
		}
	}

	private void createAugmentOperation(MultiMap<Requirement, Augment> operations, String bsn, Attrs attrs) {
		String range = attrs.getVersion();

		Requirement bundleRequirement = CapReqBuilder.createBundleRequirement(bsn, range)
			.buildSyntheticRequirement();

		Augment augment = new Augment();
		augment.additionalCapabilities = new Parameters(attrs.get(Constants.AUGMENT_CAPABILITY_DIRECTIVE));
		augment.additionalRequirements = new Parameters(attrs.get(Constants.AUGMENT_REQUIREMENT_DIRECTIVE));

		operations.add(bundleRequirement, augment);
	}

	private void executeAugmentOperations(Map<Requirement, Collection<Capability>> allBundles,
		Requirement bundleRequirement, List<Augment> augments) throws Exception {

		Collection<Capability> matchedBundleCapabilities = allBundles.get(bundleRequirement);
		Collection<Resource> bundles = ResourceUtils.getResources(matchedBundleCapabilities);

		for (Resource bundle : bundles) {

			ResourceBuilder wrappedBundleBuilder = new ResourceBuilder();
			Map<Capability, Capability> originalToWrapper = wrappedBundleBuilder.from(bundle);
			wrapped.putAll(originalToWrapper);

			List<Augment> bundleAugments = augments;
			for (Augment augment : bundleAugments) {
				List<Capability> addedCapabilities = augment(augment, wrappedBundleBuilder);
				augmentedCapabilities.addAll(addedCapabilities);
			}

			Resource wrappedBundle = wrappedBundleBuilder.build();
			augmentedBundles.add(wrappedBundle);
		}
	}

	private List<Capability> augment(Augment augment, ResourceBuilder builder) throws Exception {
		builder.addRequireCapabilities(augment.additionalRequirements);
		return builder.addProvideCapabilities(augment.additionalCapabilities);
	}
}
