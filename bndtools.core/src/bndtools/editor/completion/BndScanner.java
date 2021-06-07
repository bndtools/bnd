package bndtools.editor.completion;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

import aQute.bnd.help.Syntax;
import aQute.bnd.osgi.Constants;

public class BndScanner extends RuleBasedScanner {
	BndSourceViewerConfiguration bsvc;

	public BndScanner(BndSourceViewerConfiguration manager) {
		bsvc = manager;
		IRule[] rules = new IRule[] {
			new WhitespaceRule(c -> (c == ' ' || c == '\t' || c == '\n' || c == '\r')), new BndWordRule(),
			new MacroRule(bsvc.T_MACRO), new EndOfLineRule("#", bsvc.T_COMMENT),
			new BndEndOfLineRule("\\ ", bsvc.T_ERROR), new BndEndOfLineRule("\\\t", bsvc.T_ERROR),
		};

		setRules(rules);
		setDefaultReturnToken(bsvc.T_DEFAULT);
	}

	class BndWordRule implements IRule {

		Map<String, IToken> keyWords = new HashMap<>();

		public BndWordRule() {
			Set<String> instructions = Syntax.HELP.values()
				.stream()
				.map(Syntax::getHeader)
				.collect(Collectors.toSet());
			addWords(instructions, bsvc.T_INSTRUCTION);
			addWords(Constants.options, bsvc.T_OPTION);
			addWords(Constants.directives, bsvc.T_DIRECTIVE);
			addWords(Constants.COMPONENT_DIRECTIVES, bsvc.T_DIRECTIVE);
			addWords(Constants.COMPONENT_DIRECTIVES_1_1, bsvc.T_DIRECTIVE);
			addWords(Constants.COMPONENT_DIRECTIVES_1_2, bsvc.T_DIRECTIVE);
		}

		private boolean isWordStart(char c) {
			return Character.isJavaIdentifierStart(c);
		}

		private boolean isWordPart(char c) {
			return Character.isJavaIdentifierPart(c) || c == '-';
		}

		@Override
		public IToken evaluate(ICharacterScanner scanner) {
			StringBuffer sb = new StringBuffer();

			int c = scanner.read();
			if (isWordStart((char) c) || c == '-') {
				do {
					sb.append((char) c);
					c = scanner.read();
				} while (c != ICharacterScanner.EOF && isWordPart((char) c));
				scanner.unread();

				IToken token = keyWords.get(sb.toString());
				if (token != null)
					return token;
				return bsvc.T_DEFAULT;
			}
			scanner.unread();
			return Token.UNDEFINED;

		}

		private void addWords(Collection<String> words, IToken token) {
			for (String word : words) {
				keyWords.put(word, token);
			}
		}
	}

	class BndEndOfLineRule extends EndOfLineRule {

		public BndEndOfLineRule(String startSequence, IToken token) {
			super(startSequence, token);
		}

		@Override
		protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {

			boolean checkEof = eofAllowed;
			if (BndScanner.this.fOffset < BndScanner.this.fDocument.getLength()) {
				checkEof = false;
			}
			return super.sequenceDetected(scanner, sequence, checkEof);
		}
	}
}
