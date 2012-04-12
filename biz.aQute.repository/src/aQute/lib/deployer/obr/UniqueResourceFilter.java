package aQute.lib.deployer.obr;

import java.util.*;

import org.xml.sax.*;

import aQute.libg.sax.filters.*;

public class UniqueResourceFilter extends ElementSelectionFilter {
	
	final Set<String> uris = new HashSet<String>();
	final Map<String, List<String>> filteredResources = new HashMap<String, List<String>>();

	@Override
	protected boolean select(int depth, String uri, String localName, String qName, Attributes attribs) {
		if ("resource".equals(qName)) {
			String resourceUri = attribs.getValue("uri");
			if (uris.contains(resourceUri)) {
				addFilteredBundle(attribs.getValue("symbolicname"), attribs.getValue("version"));
				return false;
			}
			uris.add(resourceUri);
		}
		return true;
	}

	private void addFilteredBundle(String bsn, String version) {
		List<String> versions = filteredResources.get(bsn);
		if (versions == null) {
			versions = new LinkedList<String>();
			filteredResources.put(bsn, versions);
		}
		versions.add(version);
	}
	
	public Collection<String> getFilteredBSNs() {
		return filteredResources.keySet();
	}
	
	public Collection<String> getFilteredVersions(String bsn) {
		List<String> list = filteredResources.get(bsn);
		if (list == null)
			list = Collections.emptyList();
		return list;
	}

}
