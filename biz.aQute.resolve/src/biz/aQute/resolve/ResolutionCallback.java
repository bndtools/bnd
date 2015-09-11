package biz.aQute.resolve;

import java.util.List;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public interface ResolutionCallback {
	void processCandidates(Requirement requirement, Set<Capability> wired, List<Capability> candidates);
}
