package bndtools.editor.completion;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

import aQute.bnd.osgi.Analyzer;

public class BndScanner extends RuleBasedScanner {
    BndSourceViewerConfiguration bsvc;

    public BndScanner(BndSourceViewerConfiguration manager) {
        bsvc = manager;
        IRule[] rules = new IRule[] {
                new WhitespaceRule(new IWhitespaceDetector() {
                    public boolean isWhitespace(char c) {
                        return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
                    }
                }), new BndWordRule(), new MacroRule(bsvc.T_MACRO), new EndOfLineRule("#", bsvc.T_COMMENT), new BndEndOfLineRule("\\ ", bsvc.T_ERROR), new BndEndOfLineRule("\\\t", bsvc.T_ERROR),
        };

        setRules(rules);
        setDefaultReturnToken(bsvc.T_DEFAULT);
    }

    class BndWordRule implements IRule {

        Map<String,IToken> keyWords = new HashMap<String,IToken>();

        public BndWordRule() {
            addWords(Analyzer.headers, bsvc.T_INSTRUCTION);
            addWords(Analyzer.options, bsvc.T_OPTION);
            addWords(Analyzer.directives, bsvc.T_DIRECTIVE);
            // TODO need to move these constants to Constants to avoid the
            // dependency on aQute.bnd.make.component which drags in half the
            // universe
            // addWords(ServiceComponent.componentDirectives, bsvc.T_COMPONENT);
        }

        private boolean isWordStart(char c) {
            return Character.isJavaIdentifierStart(c);
        }

        private boolean isWordPart(char c) {
            return Character.isJavaIdentifierPart(c) || c == '-';
        }

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

        private void addWords(String[] words, IToken token) {
            for (int i = 0; i < words.length; ++i) {
                keyWords.put(words[i], token);
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
