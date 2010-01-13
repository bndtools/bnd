package name.neilbartlett.eclipse.bndtools.editor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.operation.IRunnableContext;

public interface IJavaSearchContext {
	IJavaProject getJavaProject();
	IRunnableContext getRunContext();
}
