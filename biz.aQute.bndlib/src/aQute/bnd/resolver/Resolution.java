package aQute.bnd.resolver;

import java.util.*;

import aQute.bnd.resolver.Resource.Requirement;
import aQute.lib.collections.*;

public class Resolution {
	public Set<Requirement>				unresolved;
	public MultiMap<Requirement, Resource>	multiple;
	public Map<Requirement, Resource>		unique;
}
