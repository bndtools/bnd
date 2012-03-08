package bndtools.model.clauses;

import org.osgi.framework.Constants;

import aQute.libg.header.Attrs;

public class VersionedClause extends HeaderClause implements Cloneable {
	public VersionedClause(String name, Attrs attribs) {
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
		return new VersionedClause(this.name, new Attrs(this.attribs));
	}
}
