package name.neilbartlett.eclipse.bndtools.editor.model;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;

public class ImportPattern extends HeaderClause implements Cloneable {
	
	public ImportPattern(String pattern, Map<String, String> attributes) {
		super(pattern, attributes);
	}
	public boolean isOptional() {
		String resolution = attribs.get(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE);
		return Constants.RESOLUTION_OPTIONAL.equals(resolution);
	}
	public void setOptional(boolean optional) {
		attribs.put(aQute.lib.osgi.Constants.RESOLUTION_DIRECTIVE, optional ? Constants.RESOLUTION_OPTIONAL : null);
	}
	
	public String getVersionRange() {
		return attribs.get(Constants.VERSION_ATTRIBUTE);
	}
	public void setVersionRange(String versionRangeString) {
		attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeString);
	}
	
	@Override
	public ImportPattern clone() {
		return new ImportPattern(this.name, new HashMap<String, String>(this.attribs));
	}
	@Override
	protected boolean newlinesBetweenAttributes() {
		return false;
	} 
}
