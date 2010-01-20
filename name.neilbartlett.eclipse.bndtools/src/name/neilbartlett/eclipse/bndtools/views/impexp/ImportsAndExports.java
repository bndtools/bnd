package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.util.Collection;

class ImportsAndExports {
	final Collection<? extends ImportPackage> imports;
	final Collection<? extends ExportPackage> exports;

	public ImportsAndExports(Collection<? extends ImportPackage> imports, Collection<? extends ExportPackage> exports) {
		this.imports = imports;
		this.exports = exports;
	}
}
