package bndtools.editor.completion;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;

import aQute.bnd.help.Syntax;
import aQute.bnd.osgi.Constants;

public class BndScanner extends RuleBasedScanner {
	BndSourceViewerConfiguration	bsvc;
	final Set<String>				instructions;
	final Set<String>				directives	= new HashSet<>();

	public BndScanner(BndSourceViewerConfiguration manager) {
		bsvc = manager;
		instructions = Syntax.HELP.values()
			.stream()
			.map(Syntax::getHeader)
			.collect(Collectors.toSet());

		directives.addAll(Constants.directives);
		directives.addAll(Constants.COMPONENT_DIRECTIVES);
		directives.addAll(Constants.COMPONENT_DIRECTIVES_1_1);
		directives.addAll(Constants.COMPONENT_DIRECTIVES_1_2);

		IRule[] rules = new IRule[] {
			this::comment, //
			this::keyword, //
			this::error,
		};

		setRules(rules);
		setDefaultReturnToken(bsvc.T_DEFAULT);
	}

	IToken comment(ICharacterScanner scanner) {
		if (scanner.getColumn() != 0)
			return Token.UNDEFINED;

		int c;
		int n = 0;
		while (true) {
			do {
				c = scanner.read();
				n++;
			} while ((c == ' ' || c == '\t'));

			if (c == '#' || c == '!') {
				while (true) {
					c = scanner.read();
					n++;

					if (c == '\n' || c == '\r' || c == ICharacterScanner.EOF)
						return bsvc.T_COMMENT;
				}
			} else {
				while (n-- > 0)
					scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}

	IToken keyword(ICharacterScanner scanner) {
		if (scanner.getColumn() != 0)
			return Token.UNDEFINED;

		int c;
		int n = 0;
		c = scanner.read();
		n++;

		StringBuilder sb = new StringBuilder();
		while (!(c == ' ' || c == '\t' || c == ':' || c == '=' || c == ICharacterScanner.EOF)) {

			if (c == '\\') {
				c = scanner.read();
				n++;
				if (c == ICharacterScanner.EOF) {
					break;
				}
			}

			sb.append((char) c);
			c = scanner.read();
			n++;
		}

		if (sb.isEmpty()) {

			while (n-- > 0)
				scanner.unread();

			return Token.UNDEFINED;
		}

		scanner.unread();

		String key = sb.toString();

		if (Constants.options.contains(key)) {
			return bsvc.T_OPTION;
		}

		if (instructions.contains(key)) {
			return bsvc.T_INSTRUCTION;
		}

		return bsvc.T_KEY;
	}


	/**
	 * Scans for invalid backslash sequences in a Java properties file and
	 * returns a token indicating the type of content.
	 * <p>
	 * This method detects:
	 * <ul>
	 * <li>Single backslashes at the end of the line or before EOF (line
	 * continuation)</li>
	 * <li>Invalid sequences such as a backslash followed by a space or tab
	 * (error)</li>
	 * <li>Valid escape sequences like \t, \n, \r, \f, \\, \:, \=, \#, \!, and
	 * Unicode \\uXXXX</li>
	 * </ul>
	 * <p>
	 * Implementation notes:
	 * <ul>
	 * <li>The first character is read and checked for a backslash.</li>
	 * <li>All consecutive backslashes are collected into a buffer.</li>
	 * <li>If the number of consecutive backslashes is even, it is valid and
	 * unread back into the scanner.</li>
	 * <li>If odd, the next character determines whether it is a valid escape,
	 * line continuation, or error.</li>
	 * <li>Special handling for EOF: a single backslash at EOF returns T_DEFAULT
	 * instead of UNDEFINED to force Eclipse to refresh the partition and
	 * prevent "sticky red" highlighting.</li>
	 * <li>Buffering ensures scanner state is fully restored for non-error
	 * sequences.</li>
	 * </ul>
	 *
	 * @param scanner the character scanner
	 * @return a token representing either UNDEFINED, T_ERROR, or T_DEFAULT for
	 *         a valid backslash at EOF
	 */
	IToken error(ICharacterScanner scanner) {
		int startColumn = scanner.getColumn();
		int first = scanner.read();
		// BackslashValidator.dbg("error() start col=%d first='%s' (code=%d)",
		// startColumn,
		// (first == ICharacterScanner.EOF ? "<EOF>" : Character.toString((char)
		// first)), first);

		if (first == ICharacterScanner.EOF) {
			return Token.UNDEFINED;
		}

		IToken token = handleBackspaces(scanner, first);
		// BackslashValidator.dbg("→ return token=%s at col=%d%n",
		// token.isUndefined() ? "UNDEFINED" : token.getData(),
		// scanner.getColumn());
		return token;
	}


	private IToken handleBackspaces(ICharacterScanner scanner, int first) {

		// Use the shared helper to determine the result
		BackslashValidator.Result result = BackslashValidator.handleBackslashes(scanner, first);

		switch (result) {
			case UNDEFINED :
				return Token.UNDEFINED;
			case ERROR :
				return bsvc.T_ERROR;
			case REFRESH :
				return bsvc.T_DEFAULT; // forces partition refresh
			default :
				return Token.UNDEFINED;
		}
	}



}
