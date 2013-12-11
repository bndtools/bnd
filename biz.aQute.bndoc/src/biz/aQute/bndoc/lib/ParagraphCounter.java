package biz.aQute.bndoc.lib;

public class ParagraphCounter {
	int[]	counters	= new int[] {
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
						};

	public void level(int level) {
		counters[level-1]++;
		for (int i = level; i < counters.length; i++) {
			counters[i] = 0;
		}
	}

	public String toHtml(int levels, String sep) {
		return toHtml(levels, sep, counters);
	}

	public static String toHtml(int levels, String sep, int[] counters) {
		return "<span class=bndoc-counter>" + toString(levels, sep, counters) + "</span";
	}

	public String toString(int levels, String seperator) {
		return toString(levels, seperator, counters);
	}

	public static String toString(int levels, String seperator, int[] counters) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (int i = 0; i < levels; i++) {
			sb.append(del).append(counters[i]);
			del = seperator;
		}
		return sb.toString();
	}
}
