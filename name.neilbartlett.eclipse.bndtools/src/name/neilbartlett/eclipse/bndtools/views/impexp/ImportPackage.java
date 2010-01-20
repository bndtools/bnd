package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.util.Collection;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;

public class ImportPackage extends HeaderClause {

	private final Collection<? extends String> usedBy;

	public ImportPackage(String name, Map<String, String> attribs, Collection<? extends String> usedBy) {
		super(name, attribs);
		this.usedBy = usedBy;
	}
	
	public Collection<? extends String> getUsedBy() {
		return usedBy;
	};

}
