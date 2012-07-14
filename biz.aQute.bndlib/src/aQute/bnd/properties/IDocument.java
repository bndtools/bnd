package aQute.bnd.properties;

public interface IDocument {

	int getNumberOfLines();

	IRegion getLineInformation(int lineNum) throws BadLocationException;

	String get();

	String get(int offset, int length) throws BadLocationException;

	String getLineDelimiter(int line) throws BadLocationException;

	int getLength();

	void replace(int offset, int length, String data) throws BadLocationException;

	char getChar(int offset) throws BadLocationException;

}
