package aQute.bnd.build.model.clauses;

import org.osgi.framework.Constants;

import aQute.libg.header.Attrs;

public class ImportPattern extends VersionedClause implements Cloneable {

	public ImportPattern(String pattern, Attrs attributes) {
		super(pattern, attributes);
	}

	public boolean isOptional() {
		String resolution = attribs.get(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE);
		return Constants.RESOLUTION_OPTIONAL.equals(resolution);
	}

	public void setOptional(boolean optional) {
		if (optional)
			attribs.put(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
		else
			attribs.remove(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE);
	}

	@Override
	public ImportPattern clone() {
		return new ImportPattern(this.name, new Attrs(this.attribs));
	}
}
