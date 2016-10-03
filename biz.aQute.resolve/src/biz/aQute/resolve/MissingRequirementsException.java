package biz.aQute.resolve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;

public class MissingRequirementsException extends ResolutionException {
	private static final long serialVersionUID = 1L;

	private final Map<Resource,List<Wire>>	wirings;
	private final boolean					verbose;
	private final boolean					trace	= false;

	public MissingRequirementsException(Collection<Requirement> requirements,
 Map<Resource,List<Wire>> wirings,
			Processor properties) {
		super((String) null, null, requirements);

		this.wirings = wirings;

		Boolean verbose;
		try {
			verbose = Converter.cnv(Boolean.class, properties.getProperty("-resolve.verbose", "true"));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

		this.verbose = verbose.booleanValue();
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append("Unable to resolve");
		for (Requirement requirement : getUnresolvedRequirements()) {
			if (verbose) {
				ArrayList<Object> path = new ArrayList<>();
				trace("[UR] " + requirement);
				traceProvider(requirement.getResource(), path);
				ListIterator<Object> listIterator = path.listIterator(path.size());
				String prefix = "-> ";
				while (listIterator.hasPrevious()) {
					Object entry = listIterator.previous();
					sb.append("\n");
					sb.append(prefix);
					if (entry instanceof Requirement) {
						Requirement r = (Requirement) entry;
						sb.append(r.getNamespace());
						sb.append("|");
					} else {
						sb.append("resource|");
					}
					sb.append(entry);
					prefix = "  " + prefix;
				}
			}
			sb.append("\n[missing|");
			sb.append(requirement.getNamespace());
			sb.append("] ");
			sb.append(requirement);
			if (verbose)
				sb.append("\n");
		}

		return sb.toString();
	}

	private boolean traceProvider(Resource provider, ArrayList<Object> path) {
		path.add(provider);
		trace("  [TRACE] " + provider);

		for (Capability capability : provider.getCapabilities(null)) {
			trace("    [CHECK] " + capability.getNamespace() + "|" + capability);
			for (Entry<Resource,List<Wire>> entry : wirings.entrySet()) {
				Resource requirer = entry.getKey();
				for (Requirement requirement : requirer.getRequirements(capability.getNamespace())) {
					trace("      [AGAINST] " + requirement.getNamespace() + "|" + requirement);
					if (path.contains(requirement)) {
						trace("    [CONTAINED] " + requirement.getNamespace() + "|" + requirement);
					} else if (ResourceUtils.matches(requirement, capability)) {
						trace("    [MATCHED] " + requirement.getNamespace() + "|" + requirement);
						path.add(requirement);
						IdentityCapability identityCapability = ResourceUtils
								.getIdentityCapability(requirement.getResource());
						String identity = ResourceUtils.getIdentity(identityCapability);
						if (AbstractResolveContext.IDENTITY_INITIAL_RESOURCE.equals(identity)) {
							return true;
						} else if (traceProvider(requirement.getResource(), path)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void trace(String message) {
		if (!trace)
			return;
		System.err.println(message);
	}
}
