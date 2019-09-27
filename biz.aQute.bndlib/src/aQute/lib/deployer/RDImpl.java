package aQute.lib.deployer;

import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;

public class RDImpl extends ResourceDescriptor implements Cloneable, Comparable<RDImpl> {

	@Override
	public RDImpl clone() throws CloneNotSupportedException {
		return (RDImpl) super.clone();
	}

	@Override
	public int compareTo(RDImpl o) {
		if (this == o)
			return 0;

		int r = bsn.compareTo(o.bsn);
		if (r == 0)
			r = version.compareTo(o.version);

		return r;
	}

}
