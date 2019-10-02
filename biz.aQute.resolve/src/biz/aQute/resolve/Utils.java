package biz.aQute.resolve;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.util.dto.DTO;

public class Utils {
	static FilterParser fp = new FilterParser();

	public static Version findIdentityVersion(Resource resource) {
		List<Capability> idCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (idCaps == null || idCaps.isEmpty())
			throw new IllegalArgumentException("Resource has no identity capability.");
		if (idCaps.size() > 1)
			throw new IllegalArgumentException("Resource has more than one identity capability.");

		Object versionObj = idCaps.get(0)
			.getAttributes()
			.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		if (versionObj == null)
			return Version.emptyVersion;

		if (versionObj instanceof Version)
			return (Version) versionObj;

		if (versionObj instanceof String)
			return Version.parseVersion((String) versionObj);

		throw new IllegalArgumentException("Unable to convert type for version attribute: " + versionObj.getClass());
	}

	public static class ResolveTrace extends DTO {
		public String	message;
		public String	bsn;
		public String	version;
		public String	requirement;
	}

	public static final Pattern	RESOLVE_MESSAGE_P		= Pattern.compile(		//
		"(?:org\\.osgi\\.service\\.resolver\\.ResolutionException: )?"			//
			+ "(?<msg>[^:]+): # prefix\n"										//
			+ "(?<bsn>[^\\s]+)  # the bsn\n"									//
			+ "(?<version>[^:]+): # version\n"									//
			+ "missing requirement Require\\[ # upto the requirement\n"			//
			+ "(?<ns>[^\\]]+)\\] # namespace\n"									//
			+ "\\{(?<attrs>[^}]*)\\} # attrs\n"									//
			+ "\\{(?<directives>[^}]*)\\} # dirs\n"								//
			+ "(?<cause>\\[caused by:)?",
		Pattern.COMMENTS | Pattern.CASE_INSENSITIVE);

	public static final Pattern	RESOLVE_DIRECTIVES_P	= Pattern.compile(		//
		"(?:^|.*,)filter=(?<filter>[^,]+)(?:$|,.*)",							//
		Pattern.COMMENTS | Pattern.CASE_INSENSITIVE);

	public static List<ResolveTrace> parseException(String message) {
		Matcher m = RESOLVE_MESSAGE_P.matcher(message);
		List<ResolveTrace> result = new ArrayList<>();
		while (m.lookingAt()) {
			ResolveTrace rt = new ResolveTrace();
			rt.bsn = m.group("bsn");
			rt.message = m.group("msg");
			rt.version = m.group("version");

			String namespace = m.group("ns");
			String attrs = m.group("attrs");
			String dirs = m.group("directives");
			try {
				Matcher filter = RESOLVE_DIRECTIVES_P.matcher(dirs);
				if (filter.matches()) {
					String f = filter.group("filter");
					Expression parse = fp.parse(f);
					rt.requirement = parse.toString();
				} else
					rt.requirement = "[" + namespace + "] {" + attrs + "} {" + dirs + "}";
			} catch (Exception e) {
				rt.requirement = "[" + namespace + "] {" + attrs + "} {" + dirs + "} " + e;
			}
			result.add(rt);
		}
		return result;
	}

}
