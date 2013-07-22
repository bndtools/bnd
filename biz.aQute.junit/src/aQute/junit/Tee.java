package aQute.junit;

import java.io.*;
import java.nio.charset.*;

public class Tee extends OutputStream {
	PrintStream				oldStream;
	ByteArrayOutputStream	buffer	= new ByteArrayOutputStream();
	boolean					capture;
	boolean					echo;

	public Tee(PrintStream oldOut) {
		oldStream = oldOut;
	}

	public PrintStream getStream() {
		return new PrintStream(this);
	}

	@Override
	public void write(int character) throws IOException {
		if (capture)
			buffer.write(character);
		if (echo)
			oldStream.write(character);
	}

	public String getContent() {
		if (buffer.size() == 0)
			return null;
		try {
			return buffer.toString(Charset.defaultCharset().toString());
		}
		catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public Tee clear() {
		buffer.reset();
		return this;
	}

	public Tee capture(boolean capture) {
		this.capture = capture;
		return this;
	}

	public Tee echo(boolean echo) {
		this.echo = echo;
		return this;
	}

}
