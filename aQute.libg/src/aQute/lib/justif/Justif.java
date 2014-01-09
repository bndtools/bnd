package aQute.lib.justif;

import java.util.*;

public class Justif {
	final int[]		tabs;
	final int		width;
	StringBuilder	sb	= new StringBuilder();
	Formatter		f	= new Formatter(sb);

	public Justif(int width, int... tabs) {
		this.tabs = tabs == null || tabs.length == 0 ? new int[] {
				30, 40, 50, 60, 70
		} : tabs;
		this.width = width == 0 ? 73 : width;
	}

	public Justif() {
		this(0);
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
					else {
						while (r < sb.length() && sb.charAt(r) == ' ')
							sb.delete(r, r + 1);
					}
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
					sb.setCharAt(r - 1, '\n');
					for (int i = 0; i < indent; i++) {
						sb.insert(r, ' ');
					}
					r += indent;
					while (r < sb.length() && sb.charAt(r) == ' ')
						sb.delete(r, r + 1);
					linelength = 0;
					lastSpace = 0;
					break;

				case '$' :
					if (sb.length() > r) {
						char c = sb.charAt(r);
						if (c == '-' || c == '_' || c == '—') {
							sb.delete(r-1,r); // remove $
							begin = false;
							linelength++;
							while ( linelength < width-1) {
								sb.insert(r++, c);
								linelength++;
							}
							break;
						}
					}

				default :
					linelength++;
					begin = false;
					if (lastSpace != 0 && linelength > width) {
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

	public String wrap() {
		wrap(sb);
		return sb.toString();
	}

	public Formatter formatter() {
		return f;
	}

	public String toString() {
		wrap(sb);
		return sb.toString();
	}

	public void indent(int indent, String string) {
		for (int i=0; i<string.length(); i++) {
			char c = string.charAt(i);
			if ( i==0 ) {
				for ( int j=0; j<indent; j++)
					sb.append(' ');
			} else {
				sb.append(c);
				if ( c == '\n')
					for ( int j=0; j<indent; j++)
						sb.append(' ');
			}
		}
	}
}
