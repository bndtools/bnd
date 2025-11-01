package bndtools.editor.completion;

import org.eclipse.jface.text.rules.ICharacterScanner;

import aQute.lib.hex.Hex;

public final class BackslashValidator {

    public enum Result {
        UNDEFINED,    // valid escape or even run
        ERROR,        // invalid backslash sequence (e.g., \ followed by space)
        REFRESH       // odd backslash at EOF, to refresh partition
    }

	/**
	 * Handles a sequence of backslashes and determines whether it forms a valid
	 * escape, line continuation, or an error sequence.
	 * <p>
	 * Rules:
	 * <ul>
	 * <li>Even-length runs of backslashes are always valid (all characters are
	 * unread).</li>
	 * <li>Odd-length runs are followed by:
	 * <ul>
	 * <li>Line breaks (\n or \r) → valid line continuation, unread buffer,
	 * return UNDEFINED</li>
	 * <li>EOF → return T_DEFAULT to refresh partition in Eclipse (prevents
	 * sticky red)</li>
	 * <li>Space or tab → invalid sequence, consume spaces, return T_ERROR</li>
	 * <li>Valid escape characters (\t, \n, \r, \f, \\, :, =, #, !, or Unicode
	 * \\uXXXX) → unread buffer, return UNDEFINED</li>
	 * <li>Anything else → invalid, return T_ERROR</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * <p>
	 * The method always maintains scanner consistency by using a buffer to
	 * unread non-error characters, ensuring the scanner position is restored.
	 *
	 * @param scanner the character scanner
	 * @param first the first character read (must be a backslash to enter
	 *            processing)
	 * @return a token indicating UNDEFINED, T_ERROR, or T_DEFAULT for EOF
	 */
	public static Result handleBackslashes(ICharacterScanner scanner, int first) {
		StringBuilder buffer = new StringBuilder();
		buffer.append((char) first);

		// dbg("handleBackspaces(): first='%c' (%d)", first, first);

		if (first != '\\') {
			unreadBuffer(scanner, buffer);
			// dbg("not a backslash → UNDEFINED");
			return Result.UNDEFINED;
		}

		int c;
		// count consecutive backslashes
		int runLen = 1;
		while (true) {
			int peek = scanner.read();
			if (peek == ICharacterScanner.EOF || peek != '\\') {
				c = peek; // next char after run
				break;
			}
			buffer.append((char) peek);
			runLen++;
		}
		boolean odd = (runLen & 1) == 1;

		if (!odd) {
			unreadBuffer(scanner, buffer); // even run → unread everything
			// dbg("even runLen=%d → UNDEFINED", runLen);
			return Result.UNDEFINED;
		}

		// odd backslash → check next char
		if (c == '\n' || c == '\r') {
			unreadBuffer(scanner, buffer);
			// dbg("odd backslash but line continuation → UNDEFINED");
			return Result.UNDEFINED;
		}

		if (c == ICharacterScanner.EOF) {
			unreadBuffer(scanner, buffer);
			// dbg("odd backslash at EOF → return T_DEFAULT to refresh
			// partition");
			return Result.REFRESH; // forces partition update
		}

		if (c == ' ' || c == '\t') {
			// consume all spaces
			do {
				c = scanner.read();
				if (c == ICharacterScanner.EOF)
					break;
				buffer.append((char) c);
			} while (c == ' ' || c == '\t');

			if (c != ICharacterScanner.EOF)
				scanner.unread(); // unread first non-space

			// dbg("invalid '\\ ' sequence → ERROR");
			return Result.ERROR; // leave buffer consumed for error only
		}

		// Valid single-character escapes
		if ("tnrf\\=:#!".indexOf(c) >= 0) {
			unreadBuffer(scanner, buffer); // valid escape → unread everything
			// dbg("valid escape '\\%c' → UNDEFINED", c);
			return Result.UNDEFINED;

		}

		// Unicode escape
		if (c == 'u') {
			// unicode check borrowed from
			// aQute.lib.utf8properties.PropertiesParser.backslash()
			// read 4 hex digits after \\u
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 4; i++) {
				int ch = scanner.read();
				if (ch == ICharacterScanner.EOF)
					break;
				sb.append((char) ch);
			}
			String unicode = sb.toString();
			if (unicode.length() != 4 || !Hex.isHex(unicode)) {
				// dbg("invalid unicode escape: \\u%s → ERROR", unicode);
				unreadBuffer(scanner, buffer);
				return Result.ERROR;
			} else {
				// dbg("valid unicode escape: \\u%s → UNDEFINED", unicode);
				unreadBuffer(scanner, buffer);
				return Result.UNDEFINED;
			}
		}

		// dbg("anything else after odd '\\' → ERROR");
		return Result.ERROR;
    }

	private static void unreadBuffer(ICharacterScanner scanner, StringBuilder buffer) {
		for (int i = buffer.length() - 1; i >= 0; i--)
			scanner.unread();
		// dbg("unreadBuffer() → put back %d chars", buffer.length());
	}

	/**
	 * for debug logging. Currently all callers comment for performance reasons.
	 */
	static void dbg(String fmt, Object... args) {
		System.out.printf("[BndScanner] " + fmt + "%n", args);
	}

}