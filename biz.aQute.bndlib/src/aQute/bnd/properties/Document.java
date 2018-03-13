package aQute.bnd.properties;

public class Document implements IDocument {

	public final static String[]	DELIMITERS	= {
		"\r", "\n", "\r\n"
	};

	private LineTracker				lineTracker	= new LineTracker();
	private ITextStore				textStore	= new CopyOnWriteTextStore(new GapTextStore());

	public Document(String text) {
		setText(text);
	}

	@Override
	public int getNumberOfLines() {
		return lineTracker.getNumberOfLines();
	}

	@Override
	public IRegion getLineInformation(int line) throws BadLocationException {
		return lineTracker.getLineInformation(line);
	}

	@Override
	public String get(int offset, int length) throws BadLocationException {
		return textStore.get(offset, length);
	}

	@Override
	public String getLineDelimiter(int line) throws BadLocationException {
		return lineTracker.getLineDelimiter(line);
	}

	@Override
	public int getLength() {
		return textStore.getLength();
	}

	@Override
	public void replace(int offset, int length, String text) throws BadLocationException {
		textStore.replace(offset, length, text);
		lineTracker.set(get());
	}

	@Override
	public char getChar(int pos) {
		return textStore.get(pos);
	}

	public void setText(String text) {
		textStore.set(text);
		lineTracker.set(text);
	}

	@Override
	public String get() {
		return textStore.get(0, textStore.getLength());
	}

	protected static class DelimiterInfo {
		public int		delimiterIndex;
		public int		delimiterLength;
		public String	delimiter;
	}
}
