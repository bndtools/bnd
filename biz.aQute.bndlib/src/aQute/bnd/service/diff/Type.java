package aQute.bnd.service.diff;

public enum Type {
	ACCESS,
	BUNDLE,
	API,
	MANIFEST,

	// XML Resource Repository Differ Types - START
	ATTRIBUTE,
	DIRECTIVE,
	REQUIREMENT,
	REQUIREMENTS,
	CAPABILITY,
	CAPABILITIES,
	REPOSITORY,
	RESOURCE_ID,
	FILTER,
	EXPRESSION,
	// XML Resource Repository Differ Types - END

	PACKAGE,
	CLASS,
	INTERFACE,
	ANNOTATION,
	ENUM,
	EXTENDS,
	IMPLEMENTS,
	FIELD,
	METHOD,
	ANNOTATED,
	PROPERTY,
	RESOURCE,
	SHA,
	CUSTOM,
	CLAUSE,
	HEADER,
	PARAMETER,
	CLASS_VERSION,
	RESOURCES,
	CONSTANT,
	DEFAULT,
	RETURN,
	VERSION,
	DEPRECATED,
	REPO,
	PROGRAM,
	REVISION;

	public boolean isInherited() {
		return false;
	}
}
