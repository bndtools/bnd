package aQute.bnd.plugin.editors;

import java.util.*;

import org.eclipse.jface.text.rules.*;

import aQute.lib.osgi.*;

public class BndScanner extends RuleBasedScanner {
    BndSourceViewerConfiguration bsvc;
    

    public BndScanner(BndSourceViewerConfiguration manager) {
        bsvc = manager;
        IRule[] rules = new IRule[] {
                new WhitespaceRule(new IWhitespaceDetector() {
                    public boolean isWhitespace(char c) {
                        return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
                    }
                }), new BndWordRule(), new MacroRule(bsvc.T_MACRO),
                new EndOfLineRule("#", bsvc.T_COMMENT),
                new EndOfLineRule("\\ ", bsvc.T_ERROR),
                new EndOfLineRule("\\\t", bsvc.T_ERROR), };

        setRules(rules);
        setDefaultReturnToken(bsvc.T_DEFAULT);
    }
    
    class BndWordRule implements IRule {

        Map<String, IToken> keyWords = new HashMap<String, IToken>();

        public BndWordRule() {
            addWords(Analyzer.headers, bsvc.T_INSTRUCTION);
            addWords(Analyzer.options, bsvc.T_OPTION);
            addWords(Analyzer.directives, bsvc.T_DIRECTIVE);
            addWords(Constants.componentDirectives, bsvc.T_COMPONENT);
        }

        private boolean isWordStart(char c) {
            return Character.isJavaIdentifierStart(c);
        }

        private boolean isWordPart(char c) {
            return Character.isJavaIdentifierPart(c) || c == '-';
        }

        public IToken evaluate(ICharacterScanner scanner) {
            StringBuilder sb = new StringBuilder();

            int c = scanner.read();
            if (isWordStart((char) c) || c == '-') {
                do {
                    sb.append((char) c);
                    c = scanner.read();
                } while (c != ICharacterScanner.EOF && isWordPart((char) c));
                scanner.unread();

                IToken token = (IToken) keyWords.get(sb.toString());
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
}
