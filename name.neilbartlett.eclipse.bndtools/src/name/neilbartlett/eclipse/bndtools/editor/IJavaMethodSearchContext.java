package name.neilbartlett.eclipse.bndtools.editor;


public interface IJavaMethodSearchContext extends IJavaSearchContext {
	String getTargetTypeName();
	String[] getParameterTypeNames();
}
