package bndtools.editor.pkgpatterns;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.fieldassist.IContentProposal;

public class PkgPatternProposal implements IContentProposal {

	private final IPackageFragment	pkg;
	private final boolean			wildcard;

	private final int				replaceFromPos;

	public PkgPatternProposal(IPackageFragment pkg, boolean wildcard, int replaceFromPos) {
		this.pkg = pkg;
		this.wildcard = wildcard;

		this.replaceFromPos = replaceFromPos;
	}

	@Override
	public String getContent() {
		String content = pkg.getElementName();
		if (wildcard)
			content += "*";
		return content;
	}

	@Override
	public int getCursorPosition() {
		int length = pkg.getElementName()
			.length();
		if (wildcard)
			length++;
		return length + replaceFromPos;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
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
