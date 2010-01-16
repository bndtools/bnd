package name.neilbartlett.eclipse.bndtools.editor.model;

import java.util.Map;

import org.osgi.framework.Constants;

public class ExportedPackage extends HeaderClause {
	public ExportedPackage(String packageName, Map<String, String> attribs) {
		super(packageName, attribs);
	}

	@Override
	protected boolean newlinesBetweenAttributes() {
		return false;
	}

	public void setVersionString(String version) {
		attribs.put(Constants.VERSION_ATTRIBUTE, version);
	}
	
	public String getVersionString() {
		return attribs.get(Constants.VERSION_ATTRIBUTE);
	}
}
