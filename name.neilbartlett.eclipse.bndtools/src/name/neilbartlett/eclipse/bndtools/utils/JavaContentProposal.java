package name.neilbartlett.eclipse.bndtools.utils;

import org.eclipse.jface.fieldassist.IContentProposal;

public class JavaContentProposal implements IContentProposal {
	
	private final String packageName;
	private final String typeName;
	private final boolean isInterface;

	public JavaContentProposal(String packageName, String typeName, boolean isInterface) {
		this.packageName = packageName;
		this.typeName = typeName;
		this.isInterface = isInterface;
	}

	public String getContent() {
		return packageName + "." + typeName;
	}

	public int getCursorPosition() {
		return packageName.length() + typeName.length() + 1;
	}

	public String getDescription() {
		return null;
	}

	public String getLabel() {
		return typeName + " - " + packageName;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getTypeName() {
		return typeName;
	}
}
