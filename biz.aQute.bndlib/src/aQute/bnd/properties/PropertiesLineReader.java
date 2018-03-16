package aQute.bnd.properties;

import static aQute.bnd.properties.LineType.blank;
import static aQute.bnd.properties.LineType.comment;
import static aQute.bnd.properties.LineType.entry;
import static aQute.bnd.properties.LineType.eof;

public class PropertiesLineReader {

	private final IDocument	document;
	private final int		lineCount;

	private int				lineNum		= 0;

	private IRegion			lastRegion	= null;
	private String			lastKey		= null;
	private String			lastValue	= null;

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
					if (chars == null || chars.length == 0)
						break; // The last line ended with a backslash
				}
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
		char[] chars = document.get(lineInfo.getOffset(), lineInfo.getLength())
			.toCharArray();

		if (continued) {
			int length = lastRegion.getLength();
			length += document.getLineDelimiter(lineNum - 1)
				.length();
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
			throw new IllegalStateException(
				"Last key not available: either before state or after end of document, or last line type was not 'entry'.");
		return lastKey;
	}

	public String value() {
		if (lastValue == null)
			throw new IllegalStateException(
				"Last value not available: either before state or after end of document, or last line type was not 'entry'.");
		return lastValue;
	}
}
