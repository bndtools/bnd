package biz.aQute.markdown;

import java.util.*;
import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class CodeHandler implements Markdown.Handler {
	static Pattern	CODE_INDENTED_P	= Pattern.compile(//
											"    (?<content>.*\n+)", Pattern.UNIX_LINES);

	static Pattern	CODE_MARKED_P	= Pattern.compile(//
											"(~~~+)(?<content>(.*\n)+)\\1", Pattern.UNIX_LINES);

	@Override
	public Block process(Rover rover) throws Exception {
		if (rover.at(CODE_INDENTED_P)) {
			final StringBuilder content = new StringBuilder();

			do {
				String block = rover.group("content");
				if (block != null)
					content.append(block);
				else
					content.append("\n");
				rover.next();
			} while (rover.at(CODE_INDENTED_P));

			return new Block() {
				public void append(Formatter a) {
					a.format("<pre><code>\n");
					a.format("%s", Markdown.escape(content, false));
					a.format("</code></pre>\n");
				}

			};
		}
		if (rover.at(CODE_MARKED_P)) {
			final CharSequence content = rover.group("content");

			return new Block() {
				public void append(Formatter a) {
					a.format("<pre><code>\n");
					a.format("%s", Markdown.escape(content, false));
					a.format("</code></pre>\n");
				}

			};
		}

		return null;
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
	}
}
