package biz.aQute.markdown;

import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class HeaderHandler implements Markdown.Handler {
	static Pattern		ATX_HEADER	= Pattern.compile("(?<level>#{1,6})\\s+(?<title>[^#\n]*)\\s*#*\\s*\n\n?");
	static Pattern		SE_HEADER	= Pattern.compile("(?<title>.*)\n(?<level>[-=]{2,})\n\n?");

	final int[]			counters	= new int[7];
	int					level		= Integer.MIN_VALUE;
	private Markdown	markdown;

	@Override
	public Block process(Rover rover) throws Exception {
		if (rover.at(ATX_HEADER)) {
			int level = rover.group("level").length();
			Block b = markdown.paragraph(rover.group("title"), "h" + level);
			rover.next();
			setLevels(level, b);
			return b;
		}

		if (rover.at(SE_HEADER)) {
			int level = rover.group("level").startsWith("=") ? 1 : 2;
			Block b = markdown.paragraph(rover.group("title"), "h" + level);
			rover.next();
			setLevels(level, b);
			return b;
		}
		return null;
	}

	private void setLevels(int level, Block b) {
		if (this.level == Integer.MIN_VALUE) {
			configure();
		}

		counters[level]++;
		for (int i = level + 1; i < counters.length; i++) {
			counters[i] = 0;
		}
		if (level <= this.level)
			b.style("counter-reset: h" + level + " " + counters[level]);
	}

	@Override
	public void configure(Markdown markdown) throws Exception {
		this.markdown = markdown;
	}

	private void configure() {
		level = markdown.getConfiguration().header_number();
	}

}
