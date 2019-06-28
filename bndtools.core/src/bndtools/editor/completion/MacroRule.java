package bndtools.editor.completion;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

import aQute.bnd.osgi.Macro;

public class MacroRule implements IRule {

	private final StringBuffer	buffer	= new StringBuffer();
	private final IToken		token;

	public MacroRule(IToken token) {
		this.token = token;
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (c == '$') {
			buffer.setLength(0);
			buffer.append('$');
			if (scan(scanner, buffer))
				return token;
		}
		scanner.unread();
		return Token.UNDEFINED;

	}

	boolean scan(ICharacterScanner scanner, StringBuffer buffer) {
		int c = scanner.read();
		if (c == ICharacterScanner.EOF)
			return false;
		int terminator = Macro.getTerminator((char) c);

		if (terminator == 0)
			return false;

		while (true) {
			c = scanner.read();
			buffer.append((char) c);
			if (c == terminator)
				return true;
			else if (c == '$') {
				if (!scan(scanner, buffer))
					return false;
			} else if (c == '\\') {
				c = scanner.read();
				if (c == ICharacterScanner.EOF)
					return false;
				buffer.append((char) c);
			}
		}
	}
}
