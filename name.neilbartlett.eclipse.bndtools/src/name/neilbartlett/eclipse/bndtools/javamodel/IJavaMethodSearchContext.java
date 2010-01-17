package name.neilbartlett.eclipse.bndtools.javamodel;


public interface IJavaMethodSearchContext extends IJavaSearchContext {
	String getTargetTypeName();
	String[] getParameterTypeNames();
}
