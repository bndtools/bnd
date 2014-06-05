package org.bndtools.utils.parse.properties;

import static org.bndtools.utils.parse.properties.LineType.*;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class PropertiesLineReader {

    private final IDocument document;
    private final int lineCount;

    private int lineNum = 0;

    private IRegion lastRegion = null;
    private String lastKey = null;

    public PropertiesLineReader(IDocument document) {
        this.document = document;
        this.lineCount = document.getNumberOfLines();
    }

    public PropertiesLineReader(String data) {
        this(new Document(data));
    }

    public LineType next() throws Exception {
        int index = 0;
        char[] chars = null;

        StringBuilder keyData = new StringBuilder();
        StringBuilder valueData = new StringBuilder();
        StringBuilder currentBuffer = keyData;

        boolean started = false;

        mainLoop: while (true) {
            if (chars == null)
                chars = grabLine(false);
            if (chars == null)
                return eof;

            if (index >= chars.length)
                break;

            char c = chars[index];
            if (c == '\\') {
                index++;
                if (index == chars.length) {
                    chars = grabLine(true);
                    index = 0;
                    if (chars == null)
                        break; // The last line ended with a backslash
                }
                if (index < chars.length)
                    //
                    // protect against an index out of bound exception
                    // TODO Think we can exit here?
                    //
                    currentBuffer.append(chars[index]);
                index++;
                continue mainLoop;
            }

            if (c == '=' || c == ':')
                currentBuffer = valueData;

            if (!started && (c == '#' || c == '!'))
                return comment;

            if (Character.isWhitespace(c)) {
                if (started) {
                    // whitespace ends the key
                    currentBuffer = valueData;
                }
            } else {
                started = true;
                currentBuffer.append(c);
            }

            index++;
        }

        if (!started)
            return blank;

        lastKey = keyData.toString();
        return entry;
    }

    private char[] grabLine(boolean continued) throws BadLocationException {
        if (lineNum >= lineCount) {
            lastRegion = null;
            return null;
        }

        IRegion lineInfo = document.getLineInformation(lineNum);
        char[] chars = document.get(lineInfo.getOffset(), lineInfo.getLength()).toCharArray();

        if (continued) {
            int length = lastRegion.getLength();
            length += document.getLineDelimiter(lineNum - 1).length();
            length += lineInfo.getLength();
            lastRegion = new Region(lastRegion.getOffset(), length);
        } else {
            lastRegion = lineInfo;
        }

        lineNum++;
        return chars;
    }

    public IRegion region() {
        if (lastRegion == null)
            throw new IllegalStateException("Last region not available: either before start or after end of document.");
        return lastRegion;
    }

    public String key() {
        if (lastKey == null)
            throw new IllegalStateException("Last key not available: either before state or after end of document, or last line type was not 'entry'.");
        return lastKey;
    }
}
