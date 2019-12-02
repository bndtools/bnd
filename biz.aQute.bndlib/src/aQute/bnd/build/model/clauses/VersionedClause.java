package aQute.bnd.build.model.clauses;

import org.osgi.framework.Constants;

import aQute.bnd.header.Attrs;

public class VersionedClause extends HeaderClause implements Cloneable {
	public VersionedClause(String name, Attrs attribs) {
		super(name, attribs);
	}

	public VersionedClause(String v) {
		super(v);
	}

	public String getVersionRange() {
		return attribs.get(Constants.VERSION_ATTRIBUTE);
	}

	public void setVersionRange(String versionRangeString) {
		attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeString);
	}

	@Override
	public VersionedClause clone() {
		VersionedClause clone = (VersionedClause) super.clone();
		clone.name = this.name;
		clone.attribs = new Attrs(this.attribs);
		return clone;
	}

	public static VersionedClause error(String msg) {
		Attrs a = new Attrs();
		a.put("PARSE ERROR", msg);
		return new VersionedClause("ERROR", a);
	}
}
