package biz.aQute.resolve;

import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.lib.exceptions.Exceptions;

public class MissingRequirementsResource implements Resource {

	public static final String	MISSING_REQUIREMENT	= "bnd.missing.requirement";

	List<Capability> allCapabilities = new CopyOnWriteArrayList<>();

	public MissingRequirementsResource() {
		CapReqBuilder builder = new CapReqBuilder(IDENTITY_NAMESPACE);

		try {
			builder.setResource(this);
			builder.addAttribute(IDENTITY_NAMESPACE, MISSING_REQUIREMENT);
		} catch (Exception e) {
			Exceptions.duck(e);
		}

		allCapabilities.add(builder.buildCapability());
	}

	public List<Capability> addMissingRequirement(Requirement requirement) {
		if (isOptional(requirement)) {
			return new ArrayList<>();
		}

		CapReqBuilder builder = new CapReqBuilder(MISSING_REQUIREMENT);
		builder.setResource(this);

		try {
			builder.addAttribute(MISSING_REQUIREMENT, requirement);
		} catch (Exception e) {
			Exceptions.duck(e);
		}

		Capability capability = builder.buildCapability();
		allCapabilities.add(capability);

		List<Capability> result = new ArrayList<>();

		result.add(capability);

		return result;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		if (namespace == null)
			return Collections.unmodifiableList(allCapabilities);
		List<Capability> filteredList = new ArrayList<>();
		for (Capability capability : allCapabilities) {
			if (namespace.equals(capability.getNamespace())) {
				filteredList.add(capability);
			}
		}
		return Collections.unmodifiableList(filteredList);
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		return Collections.emptyList();
	}

	public Collection<Requirement> getMissingRequirements() {
		List<Requirement> list = new ArrayList<>();
		for (Capability capability : getCapabilities(MissingRequirementsResource.MISSING_REQUIREMENT)) {
			Requirement requirement = getRequirement(capability);
			if (isOptional(requirement)) {
				continue;
			}
			list.add(requirement);
		}
		return Collections.unmodifiableList(list);
	}

	private Requirement getRequirement(Capability capability) {
		Map<String,Object> attributes = capability.getAttributes();
		return (Requirement) attributes.get(MissingRequirementsResource.MISSING_REQUIREMENT);
	}

	private boolean isOptional(Requirement requirement) {
		Map<String,String> directives = requirement.getDirectives();
		if (PackageNamespace.RESOLUTION_OPTIONAL
				.equals(directives.get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
			return true;
		}
		return false;
	}

}