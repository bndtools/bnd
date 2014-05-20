package aQute.bnd.osgi.resource;

import java.util.*;

import org.osgi.resource.*;

import aQute.bnd.build.model.clauses.*;
import aQute.bnd.build.model.conversions.*;
import aQute.bnd.service.*;
import aQute.bnd.service.resolve.hook.*;
import aQute.bnd.version.*;
import aQute.service.reporter.*;

/**
 * ResolutionBlacklistFilter removes bundles from being resolution inputs.
 * 
 * Format for blacklist argument is 
 * bundle;version=version or
 * bundle;version=versionrange
 */
public class ResolutionBlacklistFilter implements ResolverHook, Plugin {

	public static final String BLACKLIST_PROPERTYNAME = "blacklist";
	
	private HashMap<String, List<VersionRange>> filterOut = new HashMap<String, List<VersionRange>>();
	private Converter<List<VersionedClause>,String>	converter	= new ClauseListConverter<VersionedClause>(new VersionedClauseConverter());
	
	public void setProperties(Map<String,String> map) {
		if (map.containsKey(BLACKLIST_PROPERTYNAME)) {
			List<VersionedClause> l = converter.convert(map.get(BLACKLIST_PROPERTYNAME));
			for (VersionedClause vc : l) {
				String name = vc.getName();
				String range = vc.getVersionRange();
				if (!filterOut.containsKey(name)) {
					filterOut.put(name, new LinkedList<VersionRange>());
				}
				List<VersionRange> vclist = filterOut.get(name);
				VersionRange vr = new VersionRange(vc.getVersionRange());
				vclist.add(vr);
			}
		}
	}

	public void setReporter(Reporter processor) {
	}

	public void filterMatches(Requirement requirement, List<Capability> candidates) {
		for (Iterator<Capability> iter = candidates.iterator(); iter.hasNext();) {
			Capability c = iter.next();
			Object id = c.getResource().getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity");
			//Version v = c.getResource().getCapabilities("osgi.identity").get(0).getAttributes().get("osgi.identity");
			Object vv = c.getResource().getCapabilities("osgi.identity").get(0).getAttributes().get("version");
			Version v = null;
			if (vv != null) {
				v = new Version(vv.toString());
			}
			if (id != null && v != null && filterOut.containsKey(id)) {
				List<VersionRange> versionsToFilter = filterOut.get(id);
				for (VersionRange vr : versionsToFilter) {
					if (vr.includes(v)) {
						iter.remove();
						break;
					}
				}
			}
		}
		
	}
}
