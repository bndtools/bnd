package bndtools.utils;

import java.util.Set;

import aQute.lib.unmodifiable.Sets;

public class JavaLangUtils {
	private static final Set<String>	KEYWORDS	= Sets.of("abstract", "assert", "boolean", "break", "byte", "case",
		"catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
		"finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
		"native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
		"switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while");

	private static final Set<String>	RESERVED	= Sets.of("false", "null", "true", "_");

	public static boolean isKeyword(String s) {
		return KEYWORDS.contains(s);
	}

	public static boolean isReserved(String s) {
		return RESERVED.contains(s);
	}
}
