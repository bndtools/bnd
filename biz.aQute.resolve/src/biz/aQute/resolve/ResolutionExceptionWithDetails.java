package biz.aQute.resolve;

import java.util.Collection;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;

/**
 * A {@link ResolutionException} providing more details about why resolution has
 * failed.
 */
public class ResolutionExceptionWithDetails extends ResolutionException {

	private static final long	serialVersionUID	= 1L;

	private Set<Resource>		blackList;
	private Set<Capability>		blacklistedCapabilities;

	public ResolutionExceptionWithDetails(String message, Throwable cause,
		Collection<Requirement> unresolvedRequirements, Set<Resource> blackList,
		Set<Capability> blacklistedCapabilities) {
		super(message, cause, unresolvedRequirements);
		this.blackList = blackList;
		this.blacklistedCapabilities = blacklistedCapabilities;

	}

	public Set<Resource> getBlackList() {
		return blackList;
	}

	public Set<Capability> getBlacklistedCapabilities() {
		return blacklistedCapabilities;
	}
}
