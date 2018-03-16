package aQute.bnd.differ;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Type;
import aQute.bnd.version.Version;

public class RepositoryElement {

	public static Element getTree(RepositoryPlugin repo) throws Exception {
		List<Element> programs = new ArrayList<>();
		for (String bsn : repo.list(null)) {
			List<Element> versions = new ArrayList<>();
			for (Version version : repo.versions(bsn)) {
				versions.add(new Element(Type.VERSION, version.toString()));
			}
			programs.add(new Element(Type.PROGRAM, bsn, versions, Delta.MINOR, Delta.MAJOR, null));
		}
		return new Element(Type.REPO, repo.getName(), programs, Delta.MINOR, Delta.MAJOR, repo.getLocation());
	}

}
