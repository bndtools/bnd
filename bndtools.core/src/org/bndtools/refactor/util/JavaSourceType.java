package org.bndtools.refactor.util;

/**
 * Represents the source type. This slightly mixes up two concepts. There are
 * source types like expression, compilation unit, module-info file,
 * package-info file. There is also the different types like enum, interface,
 * class, etc. The latter imply a Java compilation unit. TODO right abstraction?
 */
public enum JavaSourceType {
	UNKNOWN,
	EXPRESSION,
	STATEMENTS,
	TYPES,
	CLASS,
	ENUM,
	INTERFACE,
	ANNOTATION,
	RECORD,
	PACKAGEINFO,
	MODULEINFO;
}
