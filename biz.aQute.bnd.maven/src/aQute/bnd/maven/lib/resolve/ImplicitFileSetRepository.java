package aQute.bnd.maven.lib.resolve;

import java.io.File;
import java.util.Collection;

import aQute.bnd.repository.fileset.FileSetRepository;

/**
 * Marker type so that we can distinguish it within workspace plugins.
 */
public class ImplicitFileSetRepository extends FileSetRepository {

	public ImplicitFileSetRepository(String name, Collection<File> files) throws Exception {
		super(name, files);
	}

}
