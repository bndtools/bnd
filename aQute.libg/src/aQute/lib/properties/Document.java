package aQute.lib.properties;

public class Document implements IDocument {

	public final static String[]	DELIMITERS	= {
			"\r", "\n", "\r\n"
												};

	private LineTracker				lineTracker	= new LineTracker();
	private ITextStore				textStore	= new CopyOnWriteTextStore(new GapTextStore());

	public Document(String text) {
		setText(text);
	}

	public int getNumberOfLines() {
		return lineTracker.getNumberOfLines();
	}

	public IRegion getLineInformation(int line) throws BadLocationException {
		return lineTracker.getLineInformation(line);
	}

	public String get(int offset, int length) throws BadLocationException {
		return textStore.get(offset, length);
	}

	public String getLineDelimiter(int line) throws BadLocationException {
		return lineTracker.getLineDelimiter(line);
	}

	public int getLength() {
		return textStore.getLength();
	}

	public void replace(int offset, int length, String text) throws BadLocationException {
		textStore.replace(offset, length, text);
		lineTracker.set(get());
	}

	public char getChar(int pos) {
		return textStore.get(pos);
	}

	public void setText(String text) {
		textStore.set(text);
		lineTracker.set(text);
	}

	public String get() {
		return textStore.get(0, textStore.getLength());
	}

	protected static class DelimiterInfo {
		public int		delimiterIndex;
		public int		delimiterLength;
		public String	delimiter;
	}
}
