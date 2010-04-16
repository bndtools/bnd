package aQute.junit.runtime;

import java.io.*;

public class Tee extends OutputStream {
	PrintStream	oldStream;
	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
	public Tee(PrintStream oldOut) {
		oldStream =oldOut;
	}

	public PrintStream getStream() {
		return new PrintStream(buffer);
	}

	public void write(int character) throws IOException {
		buffer.write(character);
		oldStream.write(character);
	}

	public Tag getContent(String string) {
		Tag tag = new Tag(string);
		tag.addContent(buffer.toString());
		return tag;
	}	
}
