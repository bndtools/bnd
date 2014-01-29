package biz.aQute.markdown;

import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class BlockQuoteHandler implements Markdown.Handler {
	static Pattern	QUOTE_P	= Pattern.compile(//
									/*
									 * Markdown uses email-style > characters
									 * for blockquoting. If you’re familiar with
									 * quoting passages of text in an email
									 * message, then you know how to create a
									 * blockquote in Markdown. It looks best if
									 * you hard wrap the text and put a > before
									 * every line:
									 */
									" {0,1}> ?"
									/*
									 * And line continued paragraph
									 */
									+ "(?<content>.*\n((?! {0,1}>).+\n)*)", Pattern.UNIX_LINES);
	private Markdown	markdown;

	@Override
	public Block process(Rover rover) throws Exception {
		if (!rover.at(QUOTE_P))
			return null;

		StringBuilder content = new StringBuilder();

		do {
			String block = rover.group("content");
			if (block != null)
				content.append(block);
			else
				content.append("\n");
			rover.next();
		} while (rover.at(QUOTE_P));

		return new Markdown.CompositeBlock("blockquote", markdown.parseContent(content));
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
	}

}
