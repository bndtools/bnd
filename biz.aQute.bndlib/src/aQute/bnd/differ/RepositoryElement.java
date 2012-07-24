package aQute.bnd.differ;

import java.util.*;

import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.diff.*;
import aQute.bnd.version.*;

public class RepositoryElement {

	public static Element getTree(RepositoryPlugin repo) throws Exception {
		List<Element> programs = new ArrayList<Element>();
		for (String bsn : repo.list(null)) {
			List<Element> versions = new ArrayList<Element>();
			for (Version version : repo.versions(bsn)) {
				versions.add(new Element(Type.VERSION, version.toString()));
			}
			programs.add(new Element(Type.PROGRAM, bsn, versions, Delta.MINOR, Delta.MAJOR, null));
		}
		return new Element(Type.REPO, repo.getName(), programs, Delta.MINOR, Delta.MAJOR, repo.getLocation());
	}

}
