package biz.aQute.markdown;

import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class LinkHandler implements Markdown.Handler {
	static Pattern	LINK_DEF_P		= Pattern.compile(
									/*
									 * (optionally indented from the left margin
									 * using up to three spaces);
									 */
									" {0,3}"
									/*
									 * Square brackets containing the link
									 * identifier
									 */
									+ "\\[\\s*(?<id>[^\n\\]\\s]+)\\s*\\]"
									/*
									 * followed by a colon;
									 */
									+ ":"
									/*
									 * followed by one or more spaces (or tabs);
									 */
									+ "\\s+"
									/*
									 * followed by the URL for the link;
									 * The link URL may, optionally, be surrounded by angle brackets:
									 */
									+ "(?<url><[^\n>]+>|[^\\s\n]+)"
									/*
									 * optionally followed by a title attribute
									 * for the link, enclosed in double or
									 * single quotes, or enclosed in
									 * parentheses.
									 */
									+ "(\\s+\n?\\s*['\"(](?<title>.+)['\")]\\s*)?\n", Pattern.UNIX_LINES);
	private Markdown	markdown;


	@Override
	public Block process(Rover rover) throws Exception {
		while (rover.at(LINK_DEF_P)) {
			markdown.link(rover.group("id"), rover.group("url"), rover.group("title"));
			rover.next();
		}
		return null;
	}


	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
	}

}
