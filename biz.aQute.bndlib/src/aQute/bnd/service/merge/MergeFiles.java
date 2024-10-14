package aQute.bnd.service.merge;

import java.util.Optional;

import aQute.bnd.osgi.Resource;

public interface MergeFiles {
	Optional<Resource> merge(String path, Resource a, Resource b);
}
