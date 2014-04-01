package biz.aQute.resolve.umbrella;

import org.osgi.resource.*;

public class DRequirement extends DNamespace implements Requirement {
	public DRequirement() {}

	public DRequirement(org.osgi.service.indexer.Requirement req) {
		super(req.getNamespace());
	}
}
