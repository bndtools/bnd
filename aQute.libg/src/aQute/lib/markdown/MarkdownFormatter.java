package aQute.lib.markdown;

import java.util.Formatter;

public class MarkdownFormatter {

	private Formatter f;

	public MarkdownFormatter(Appendable out) {
		f = new Formatter(out);
	}

	public MarkdownFormatter format(String format, Object... args) {
		f = f.format(format, args);
		return this;
	}

	public MarkdownFormatter h1(String format, Object... args) {
		f = f.format("# " + format + " #%n", args);
		return this;
	}

	public MarkdownFormatter h2(String format, Object... args) {
		f = f.format("## " + format + " ##%n", args);
		return this;
	}

	public MarkdownFormatter h3(String format, Object... args) {
		f = f.format("### " + format + " ###%n", args);
		return this;
	}

	public MarkdownFormatter list(String format, Object... args) {
		f = f.format("+ " + format + "%n", args);
		return this;
	}

	@Override
	public String toString() {
		return f.toString();
	}

	public MarkdownFormatter code(String format, Object... args) {
		f = f.format("\t" + format + "%n", args);
		return this;
	}

	public MarkdownFormatter inlineCode(String format, Object... args) {
		f = f.format("`" + format + "`", args);
		return this;
	}

	public MarkdownFormatter endP() {
		f = f.format("%n%n");
		return this;
	}

	public MarkdownFormatter flush() {
		f.flush();
		return this;
	}
}
