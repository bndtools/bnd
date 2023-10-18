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

	IToken error(ICharacterScanner scanner) {
		int c = scanner.read();
		int n = 1;
		if (c == '\\') {
			c = scanner.read();
			n++;
			if (c == ' ' || c == '\t') {
				while (c == ' ' || c == '\t') {
					c = scanner.read();
				}
				scanner.unread();
				return bsvc.T_ERROR;
			}
		}
		while (n-- > 0)
			scanner.unread();
		return Token.UNDEFINED;
	}

}
