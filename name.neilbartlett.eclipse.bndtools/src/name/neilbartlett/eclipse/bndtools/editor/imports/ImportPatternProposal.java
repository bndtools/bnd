package name.neilbartlett.eclipse.bndtools.editor.imports;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.fieldassist.IContentProposal;

public class ImportPatternProposal implements IContentProposal {
	
	private final IPackageFragment pkg;
	private final boolean wildcard;
	
	private final int replaceFromPos;

	public ImportPatternProposal(IPackageFragment pkg, boolean wildcard, int replaceFromPos) {
		this.pkg = pkg;
		this.wildcard = wildcard;
		
		this.replaceFromPos = replaceFromPos;
	}
	public String getContent() {
		String content = pkg.getElementName();
		if(wildcard)
			content += "*";
		return content;
	}
	public int getCursorPosition() {
		int length = pkg.getElementName().length();
		if(wildcard)
			length++;
		return length + replaceFromPos;
	}
	public String getDescription() {
		return null;
	}
	public String getLabel() {
		return getContent();
	}
	public IPackageFragment getPackageFragment() {
		return pkg;
	}
	public boolean isWildcard() {
		return wildcard;
	}
	public int getReplaceFromPos() {
		return replaceFromPos;
	}
}