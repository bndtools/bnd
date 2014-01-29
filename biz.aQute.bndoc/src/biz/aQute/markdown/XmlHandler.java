package biz.aQute.markdown;

import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class XmlHandler implements Markdown.Handler {
	static Pattern COMMENT_P = Pattern.compile("\\s{0,3}<!--(.*?)-->\\s*", Pattern.DOTALL);
	
	static Pattern	XML_P	= Pattern.compile("<\\s*?(?<tag>[-_\\w\\d]+)( +|>|/>).*?\n\n", Pattern.DOTALL);
	boolean			unsafe;

	private Markdown	markdown;

	@Override
	public Block process(final Rover rover) throws Exception {
		if ( rover.at(COMMENT_P)) {
			return markdown.literal(rover.consume());
		}
		
		if (!rover.at(XML_P))
			return null;

		String xml = rover.consume();
		return markdown.literal(xml);
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
	}
}
