package name.neilbartlett.eclipse.bndtools.editor.model;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;

public class VersionedClause extends HeaderClause implements Cloneable {
	public VersionedClause(String name, Map<String, String> attribs) {
		super(name, attribs);
	}
	public String getVersionRange() {
		return attribs.get(Constants.VERSION_ATTRIBUTE);
	}
	public void setVersionRange(String versionRangeString) {
		attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeString);
	}
	@Override
	public VersionedClause clone() {
		return new VersionedClause(this.name, new HashMap<String, String>(this.attribs));
	}
}
