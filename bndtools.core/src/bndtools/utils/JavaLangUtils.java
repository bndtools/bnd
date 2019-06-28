package bndtools.utils;

public class JavaLangUtils {
	private static final String[]	KEYWORDS	= new String[] {
		"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
		"default", "do", "double", "else", "enum", "exports", "extends", "final", "finally", "float", "for", "goto",
		"if", "implements", "import", "instanceof", "int", "interface", "long", "module", "native", "new", "package",
		"private", "protected", "public", "requires", "return", "short", "static", "strictfp", "super", "switch",
		"synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
	};

	private static final String[]	RESERVED	= new String[] {
		"false", "null", "true"
	};

	public static boolean isKeyword(String s) {
		for (String keyword : KEYWORDS) {
			if (keyword.equals(s)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isReserved(String s) {
		for (String keyword : RESERVED) {
			if (keyword.equals(s)) {
				return true;
			}
		}
		return false;
	}
}
