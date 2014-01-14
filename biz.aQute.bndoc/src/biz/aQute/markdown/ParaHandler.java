package biz.aQute.markdown;

import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Handler;
import biz.aQute.markdown.Markdown.Rover;

public class ParaHandler implements Handler {
	static Pattern PARA_P = Pattern.compile("(?<para>(.+\n)+)\n*");
	private Markdown	markdown;
	
	@Override
	public Block process(Rover rover) throws Exception {
		if ( rover.at(PARA_P)) {
			return markdown.paragraph(rover.consume(), "p");
		}
		return null;
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
	}

}
