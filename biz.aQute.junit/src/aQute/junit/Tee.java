package aQute.junit;

import java.io.*;

public class Tee extends OutputStream {
	PrintStream				oldStream;
	ByteArrayOutputStream	buffer	= new ByteArrayOutputStream();
	boolean					capture;
	boolean					echo;

	public Tee(PrintStream oldOut) {
		oldStream = oldOut;
	}

	public PrintStream getStream() {
		try {
			return new PrintStream(this, false, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			return null;
		}
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
			return buffer.toString("UTF-8");
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
