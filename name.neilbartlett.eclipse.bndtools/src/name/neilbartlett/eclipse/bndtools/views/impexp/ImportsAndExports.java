package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.util.Map;

class ImportsAndExports {
	final Map<String, Map<String, String>> imports;
	final Map<String, Map<String, String>> exports;

	public ImportsAndExports(Map<String,Map<String,String>> imports, Map<String,Map<String,String>> exports) {
		this.imports = imports;
		this.exports = exports;
	}
}
