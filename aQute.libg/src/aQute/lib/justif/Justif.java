package aQute.lib.justif;

import java.util.*;

public class Justif {
	int[]	tabs;

	public Justif(int width, int... tabs) {
		this.tabs = tabs;
	}

	/**
	 * Routine to wrap a stringbuffer. Basically adds line endings but has the
	 * following control characters:
	 * <ul>
	 * <li>Space at the beginnng of a line is repeated when wrapped for indent.</li>
	 * <li>A tab will mark the current position and wrapping will return to that
	 * position</li>
	 * <li>A form feed in a tabbed colum will break but stay in the column</li>
	 * </ul>
	 * 
	 * @param sb
	 */
	public void wrap(StringBuilder sb) {
		List<Integer> indents = new ArrayList<Integer>();

		int indent = 0;
		int linelength = 0;
		int lastSpace = 0;
		int r = 0;
		boolean begin = true;

		while (r < sb.length()) {
			switch (sb.charAt(r++)) {
				case '\n' :
					linelength = 0;

					indent = indents.isEmpty() ? 0 : indents.remove(0);
					begin = true;
					lastSpace = 0;
					break;

				case ' ' :
					if (begin)
						indent++;
					lastSpace = r - 1;
					linelength++;
					break;

				case '\t' :
					indents.add(indent);
					indent = linelength;
					sb.deleteCharAt(--r);

					if (r < sb.length()) {
						char digit = sb.charAt(r);
						if (Character.isDigit(digit)) {
							sb.deleteCharAt(r);

							int column = (digit - '0');
							if (column < tabs.length)
								indent = tabs[column];
							else
								indent = column * 8;

							int diff = indent - linelength;
							if (diff > 0) {
								for (int i = 0; i < diff; i++) {
									sb.insert(r, ' ');
								}
								r += diff;
								linelength += diff;
							}
						}
					}
					break;

				case '\f' :
					linelength = 100000; // force a break
					lastSpace = r - 1;

					//$FALL-THROUGH$

				default :
					linelength++;
					begin = false;
					if (lastSpace != 0 && linelength > 60) {
						sb.setCharAt(lastSpace, '\n');
						linelength = 0;

						for (int i = 0; i < indent; i++) {
							sb.insert(lastSpace + 1, ' ');
							linelength++;
						}
						r += indent;
						lastSpace = 0;
					}
			}
		}
	}

}
