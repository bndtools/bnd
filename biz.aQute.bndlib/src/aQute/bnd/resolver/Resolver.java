package aQute.bnd.resolver;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.build.*;
import aQute.bnd.resolver.Resource.Requirement;
import aQute.lib.collections.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;

public class Resolver extends Processor {

	final Set<Resource>									resources	= new HashSet<Resource>();
	private Map<Resource.Requirement, Set<Resource>>	cache		= new IdentityHashMap<Resource.Requirement, Set<Resource>>();

	public void add(Container c) throws Exception {
		List<File> files = new ArrayList<File>();
		c.contributeFiles(files, this);
		for (File f : files) {
			add(f);
		}
	}

	public void addAll(Collection<Container> containers) throws Exception {
		for (Container c : containers) {
			add(c);
		}
	}

	public Resolution resolve() throws Exception {
		// Split fragments and bundles
		Set<Resource> active = new HashSet<Resource>();
		Set<Resource> fragments = new HashSet<Resource>();
		Set<Resource> singletons = new HashSet<Resource>();

		for (Resource r : resources) {
			if (r.fragments != null) {
				active.add(r);
				if (r.singleton)
					singletons.add(r);
			} else
				fragments.add(r);
		}

		// Attach fragments
		for (Resource r : fragments) {
			Collection<Resource> hosts = find(active, r.requirements, new HashSet<Resource>());
			for (Resource host : hosts) {
				host.fragments.add(host);
			}
		}

		// Create a list of all the requirements
		Set<Resource.Requirement> reqs = new HashSet<Resource.Requirement>();
		for (Resource r : active) {
			reqs.addAll(r.requirements);
			// And its attached fragments
			for (Resource f : r.fragments) {
				reqs.addAll(f.requirements);
			}
		}

		Set<Resource.Requirement> optional = Create.set();
		Set<Resource.Requirement> unresolved = Create.set();
		Map<Resource.Requirement, Resource> unique = Create.map();
		MultiMap<Requirement, Resource> multiple = new MultiMap<Requirement, Resource>();

		for (Resource.Requirement req : reqs) {
			Collection<Resource> solutions = find(active, req, new HashSet<Resource>());
			if (solutions.isEmpty()) {
				if (req.optional)
					optional.add(req);
				else
					unresolved.add(req);
			} else if (solutions.size() == 1)
				unique.put(req, solutions.iterator().next());
			else {
				multiple.addAll(req, solutions);
			}
		}

		// If we have unresolveds, tough shit

		if (!unresolved.isEmpty()) {
			for (Requirement r : unresolved) {
				error("Unresolved %s", r);
			}
		}

		// Calculate our singleton candidates
		MultiMap<String, Resource> picked = new MultiMap<String, Resource>();
		for (Resource r : singletons) {
			picked.add(r.bsn, r);
		}

		// Remove any singletons that are alone
		// and verify that if there are multiple they are not
		// both in the unique solutions
		for (Iterator<Map.Entry<String, Set<Resource>>> i = picked.entrySet().iterator(); i
				.hasNext();) {
			Map.Entry<String, Set<Resource>> entry = i.next();
			if (entry.getValue().size() == 1)
				i.remove();
			else {
				Set<Resource> x = new HashSet<Resource>(entry.getValue());
				boolean changed = x.retainAll(unique.values());
				if (x.size() > 1) {
					// We need multiple singleton bundles with the same bsn
					error("Singleton conflict: %s", x);
				} else if (changed) {
					Set<Resource> delta = new HashSet<Resource>(entry.getValue());
					delta.removeAll(x);

					// We've removed bundles from the possible solutions
					for (Iterator<Resource> it = multiple.all(); i.hasNext();) {
						Resource r = it.next();
						if (delta.contains(r)) {
							it.remove();
						}
					}
				}
			}
		}

		Resolution res = new Resolution();
		res.multiple = multiple;
		res.unique = unique;
		res.unresolved = unresolved;
		return res;
	}

	private Collection<Resource> find(Set<Resource> active, Set<Resource.Requirement> requirements,
			Set<Resource> result) {
		for (Resource.Requirement req : requirements) {
			Set<Resource> resources = cache.get(req);
			if (resources != null) {
				result.addAll(resources);
			} else {
				resources = find(active, req, new HashSet<Resource>());
				cache.put(req, resources);
				result.addAll(resources);
			}
		}
		return result;
	}

	private Set<Resource> find(Set<Resource> active, Requirement req, Set<Resource> result) {
		for (Resource r : active) {
			for (Resource.Capability cap : r.capabilities) {
				if ( cap.name.equals(req.name))
					System.out.println("Yes");
				if (req.matches(cap))
					result.add(r);
			}
		}
		return result;
	}

	public void add(File file) throws IOException {
		JarFile jf = new JarFile(file);
		try {
			Manifest m = jf.getManifest();
			Resource r = new Resource(this, m);
			resources.add(r);
		} finally {
			jf.close();
		}
	}

}
