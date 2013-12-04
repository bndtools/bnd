package biz.aQute.bndoc.lib;

import com.github.rjeschke.txtmark.*;

public class BndocDecorator extends DefaultDecorator {
	int		toclevel	= 2;
	int[]	counters	= new int[] {
			0, 0, 0, 0, 0, 0, 0
						};
	Bndoc	bndoc;

	public BndocDecorator(Bndoc bndoc) {
		this.bndoc = bndoc;
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
}
