package aQute.junit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Tee extends OutputStream {
	private final PrintStream			wrapped;
	private final ByteArrayOutputStream	buffer	= new ByteArrayOutputStream();
	private volatile boolean			capture;
	private volatile boolean			echo;

	public Tee(PrintStream toWrap) {
		wrapped = toWrap;
	}

	public PrintStream getStream() {
		return new PrintStream(this);
	}

	@Override
	public void write(int b) throws IOException {
		if (capture)
			buffer.write(b);
		if (echo)
			wrapped.write(b);
	}

	public String getContent() {
		if (buffer.size() == 0)
			return null;
		try {
			return buffer.toString(Charset.defaultCharset()
				.name());
		} catch (UnsupportedEncodingException e) {
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
