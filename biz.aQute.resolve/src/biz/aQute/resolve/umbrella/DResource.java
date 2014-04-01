package biz.aQute.resolve.umbrella;

import java.util.*;

import org.osgi.resource.*;

import aQute.bnd.service.*;
import aQute.lib.collections.*;

public class DResource implements Resource {
	public byte[]								digest;
	public MultiMap<String,DRequirement>		requirements;
	public MultiMap<String,DCapability>			capabilities;
	public long									signature;

	transient RepositoryPlugin.DownloadListener	download;

	public List<Capability> getCapabilities(String namespace) {
		if (capabilities == null || capabilities.isEmpty())
			return Collections.emptyList();

		List< ? extends Capability> list;

		if (namespace == null)
			list = capabilities.allValues();
		else
			list = capabilities.get(namespace);

		return Collections.unmodifiableList(list);
	}

	public List<Requirement> getRequirements(String namespace) {
		if (requirements == null)
			return Collections.emptyList();

		List< ? extends Requirement> list;

		if (namespace == null)
			list = requirements.allValues();
		else
			list = requirements.get(namespace);

		return Collections.unmodifiableList(list);
	}

	public void add(DRequirement rq) {
		if (requirements == null)
			requirements = new MultiMap<String,DRequirement>();

		requirements.add(rq.getNamespace(), rq);
	}

	public void add(DCapability cap) {
		if (capabilities == null)
			capabilities = new MultiMap<String,DCapability>();

		capabilities.add(cap.getNamespace(), cap);
	}

	/*
	 * We need to set the resource in our caps/reqs because they are not having
	 * a reference now since the serialization can only handle trees.
	 */
	public void fixup() {
		if (capabilities != null)
			for (Map.Entry<String,List<DCapability>> ns : capabilities.entrySet()) {
				for (DCapability dcap : ns.getValue()) {
					dcap.resource = this;
				}
			}

		if (requirements != null)
			for (Map.Entry<String,List<DRequirement>> ns : requirements.entrySet()) {
				for (DRequirement dreq : ns.getValue()) {
					dreq.resource = this;
				}
			}
	}
}
