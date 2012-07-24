package aQute.bnd.build.model.clauses;

import org.osgi.framework.*;

import aQute.bnd.header.*;

public class ImportPattern extends VersionedClause implements Cloneable {

	public ImportPattern(String pattern, Attrs attributes) {
		super(pattern, attributes);
	}

	public boolean isOptional() {
		String resolution = attribs.get(aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE);
		return Constants.RESOLUTION_OPTIONAL.equals(resolution);
	}

	public void setOptional(boolean optional) {
		if (optional)
			attribs.put(aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
		else
			attribs.remove(aQute.bnd.osgi.Constants.RESOLUTION_DIRECTIVE);
	}

	@Override
	public ImportPattern clone() {
		return new ImportPattern(this.name, new Attrs(this.attribs));
	}
}
