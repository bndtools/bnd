package bndtools.javamodel;

public interface IJavaMethodSearchContext extends IJavaSearchContext {
	String getTargetTypeName();

	String[] getParameterTypeNames();
}
