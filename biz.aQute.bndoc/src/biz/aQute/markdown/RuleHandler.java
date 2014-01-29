package biz.aQute.markdown;

import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class RuleHandler implements Markdown.Handler {
	/*
	 * You can produce a horizontal rule tag (<hr />) by placing three or more
	 * hyphens, asterisks, or underscores on a line by themselves. If you wish,
	 * you may use spaces between the hyphens or asterisks. Each of the
	 * following lines will produce a horizontal rule:
	 */
	static Pattern	RULE_P	= Pattern.compile(" {0,3}([-*_] *){3,}\n");
	private Markdown	markdown;

	@Override
	public Block process(Rover rover) throws Exception {

		if (rover.at(RULE_P)) {
			rover.consume();
			return markdown.paragraph(null, "hr");
		}

		return null;
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
	}
}
