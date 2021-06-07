package aQute.junit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

public class Tee extends PrintStream {
	private final PrintStream			wrapped;
	private final ByteArrayOutputStream	buffer	= new ByteArrayOutputStream();
	private volatile boolean			capture;
	private volatile boolean			echo;

	public Tee(PrintStream wrapped) {
		super(wrapped);
		this.wrapped = wrapped;
	}

	protected Object lock() {
		return out;
	}

	@Override
	public void write(int b) {
		synchronized (lock()) {
			if (capture)
				buffer.write(b);
			if (echo)
				wrapped.write(b);
		}
	}

	@Override
	public void write(byte b[], int off, int len) {
		synchronized (lock()) {
			if (capture)
				buffer.write(b, off, len);
			if (echo)
				wrapped.write(b, off, len);
		}
	}

	public String getContent() {
		synchronized (lock()) {
			if (buffer.size() == 0)
				return null;
			try {
				return buffer.toString(Charset.defaultCharset()
					.name());
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
	}

	public Tee clear() {
		synchronized (lock()) {
			buffer.reset();
		}
		return this;
	}

	public Tee capture(boolean capture) {
		this.capture = capture;
		return this;
	}

	public boolean capture() {
		return capture;
	}

	public Tee echo(boolean echo) {
		this.echo = echo;
		return this;
	}

	public boolean echo() {
		return echo;
	}

	@Override
	public void print(boolean b) {
		synchronized (lock()) {
			super.print(b);
		}
	}

	@Override
	public void print(char c) {
		synchronized (lock()) {
			super.print(c);
		}
	}

	@Override
	public void print(int i) {
		synchronized (lock()) {
			super.print(i);
		}
	}

	@Override
	public void print(long l) {
		synchronized (lock()) {
			super.print(l);
		}
	}

	@Override
	public void print(float f) {
		synchronized (lock()) {
			super.print(f);
		}
	}

	@Override
	public void print(double d) {
		synchronized (lock()) {
			super.print(d);
		}
	}

	@Override
	public void print(char[] s) {
		synchronized (lock()) {
			super.print(s);
		}
	}

	@Override
	public void print(String s) {
		synchronized (lock()) {
			super.print(s);
		}
	}

	@Override
	public void print(Object obj) {
		synchronized (lock()) {
			super.print(obj);
		}
	}

	@Override
	public void println() {
		synchronized (lock()) {
			super.println();
		}
	}

	@Override
	public void println(boolean x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(char x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(int x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(long x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(float x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(double x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(char[] x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(String x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public void println(Object x) {
		synchronized (lock()) {
			super.println(x);
		}
	}

	@Override
	public PrintStream printf(String format, Object... args) {
		synchronized (lock()) {
			return super.printf(format, args);
		}
	}

	@Override
	public PrintStream printf(Locale l, String format, Object... args) {
		synchronized (lock()) {
			return super.printf(l, format, args);
		}
	}

	@Override
	public PrintStream format(String format, Object... args) {
		synchronized (lock()) {
			return super.format(format, args);
		}
	}

	@Override
	public PrintStream format(Locale l, String format, Object... args) {
		synchronized (lock()) {
			return super.format(l, format, args);
		}
	}

	@Override
	public PrintStream append(CharSequence csq) {
		synchronized (lock()) {
			return super.append(csq);
		}
	}

	@Override
	public PrintStream append(CharSequence csq, int start, int end) {
		synchronized (lock()) {
			return super.append(csq, start, end);
		}
	}

	@Override
	public PrintStream append(char c) {
		synchronized (lock()) {
			return super.append(c);
		}
	}

}
