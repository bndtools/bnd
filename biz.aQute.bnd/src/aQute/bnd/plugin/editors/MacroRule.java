package aQute.bnd.plugin.editors;

import org.eclipse.jface.text.rules.*;

import aQute.lib.osgi.*;

public class MacroRule implements IRule {

    private StringBuilder buffer = new StringBuilder();
    private IToken       token;

    public MacroRule(IToken token) {
        this.token = token;
    }

    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (c == '$') {
            buffer.setLength(0);
            buffer.append('$');
            if ( scan(scanner, buffer) )
                return token;
        }
        scanner.unread();
        return Token.UNDEFINED;

    }

    boolean scan( ICharacterScanner scanner, StringBuilder buffer ) {
        int c = scanner.read();
        if (c == ICharacterScanner.EOF)
            return false;
        int terminator = Macro.getTerminator((char) c);
        
        if (terminator == 0)
            return false;


        while(true) {
            c = scanner.read();
            buffer.append((char)c);
            if ( c == terminator )
                return true;
            else
            if ( c == '$') {
                if ( !scan(scanner, buffer) )
                    return false;
            }
            else         
            if ( c == '\\') {
                c = scanner.read();
                if ( c == ICharacterScanner.EOF)
                    return false;
                buffer.append((char)c);
            }
        }
   }
}
