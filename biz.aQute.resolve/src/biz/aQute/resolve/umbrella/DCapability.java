package biz.aQute.resolve.umbrella;

import org.osgi.resource.*;

public class DCapability extends DNamespace implements Capability {
	public DCapability() {}

	public DCapability(org.osgi.service.indexer.Capability cap) {
		super(cap.getNamespace());
	}
}
