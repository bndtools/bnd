package aQute.bnd.component.error;

public class DeclarativeServicesAnnotationError {
	
	public static enum ErrorType {
		ACTIVATE_SIGNATURE_ERROR, DEACTIVATE_SIGNATURE_ERROR, MODIFIED_SIGNATURE_ERROR, 
		COMPONENT_PROPERTIES_ERROR, INVALID_REFERENCE_BIND_METHOD_NAME, MULTIPLE_REFERENCES_SAME_NAME,
		UNABLE_TO_LOCATE_SUPER_CLASS, DYNAMIC_REFERENCE_WITHOUT_UNBIND, INVALID_TARGET_FILTER, 
		UNSET_OR_MODIFY_WITH_WRONG_SIGNATURE;
	}
	
	public final String className;
	public final String methodName;
	public final String methodSignature;
	public final ErrorType errorType;
	
	public DeclarativeServicesAnnotationError(String className, String methodName, String methodSignature, ErrorType errorType) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.methodSignature = methodSignature;
		this.errorType = errorType;
	}
	
}
