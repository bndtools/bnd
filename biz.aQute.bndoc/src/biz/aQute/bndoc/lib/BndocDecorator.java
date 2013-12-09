package biz.aQute.bndoc.lib;

import com.github.rjeschke.txtmark.*;

public class BndocDecorator extends DefaultDecorator {
	int			toclevel	= 2;
	int[]		counters	= new int[] {
			0, 0, 0, 0, 0, 0, 0
							};

	int			codeStart	= -1;
	int			imageCounter;
	Generator	generator;

	BndocDecorator(Generator generator) {
		this.generator = generator;
	}

	@Override
	public void openHeadline(StringBuilder out, int level) {
		super.openHeadline(out, level);
		if (level <= toclevel) {
			counters[level - 1]++;
			for (int i = level; i < counters.length; i++) {
				counters[i] = 0;
			}
			String del = "><span class=bndoc-counter>";
			for (int i = 0; i < level; i++) {
				out.append(del).append(counters[i]);
				del = ".";
			}
			out.append("</span");
		}
	}

	@Override
	public void openCodeBlock(StringBuilder out) {
		codeStart = out.length();
	}

	@Override
	public void closeCodeBlock(StringBuilder out) {
		int lstart = codeStart;
		int row = 0;
		int artCharacters = 0;
		int otherCharacters = 0;

		for (int i = lstart; i < out.length(); i++) {
			char c = out.charAt(i);

			if (c == '\n') {
				lstart = i + 1;
			} else if ("-|+/\\><:".indexOf(c) >= 0)
				artCharacters++;
			else
				otherCharacters++;
		}

		if (artCharacters > otherCharacters) {
			out.insert(codeStart, "<pre class=textdiagram>");
		} else {
			out.insert(codeStart, "<pre>");
		}
		out.append("</pre>\n");
	}
}
