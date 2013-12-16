package biz.aQute.bndoc.lib;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.tag.*;

public class Table {
	int						line	= -1;
	int						index	= 0;
	CharSequence			text;
	ArrayList<Column>		columns	= new ArrayList<>();
	ArrayList<List<Cell>>	table	= new ArrayList<>();
	int						width;
	private String			current;
	private String			error;

	enum HAlign {
		LEFT, RIGHT, CENTER, JUSTIFY;
	}

	class Column {
		int		cwidth;
		int		cstart;
		int		column;
		boolean	head;

		Column(int i) {
			this.column = i;
		}

		public Tag getTag() {
			Tag col = new Tag("col");
			col.addAttribute("style", "width:" + cwidth + "em;");
			return col;
		}
		
		@Override
		public String toString() {
			return "Column [cwidth=" + cwidth + ", cstart=" + cstart + ", column=" + column + ", head=" + head + "]";
		}

	}

	class Cell {
		int				index;
		boolean			head;
		Column			column;
		int				colspan	= 1;
		StringBuilder	text	= new StringBuilder();
		HAlign			halign	= HAlign.LEFT;

		public Tag getTag() {
			Tag cell = new Tag(head ? "th" : "td");
			cell.addAttribute("class", "bndoc-" + halign);
			if ( colspan != 1) 
				cell.addAttribute( "colspan", colspan);

			cell.addContent(text.toString().trim());
			return cell;
		}
		@Override
		public String toString() {
			return "Cell [index=" + index + ", head=" + head + ", column=" + column + ", colspan=" + colspan + ", text="
					+ text + ", halign=" + halign + "]";
		}

	}

	static Pattern	DEF_P	= Pattern.compile("((\\+=+)+\\+)|((\\+-+)+\\+)");
	static Pattern	ROW_P	= Pattern.compile("(\\|[^|]+)+\\|");

	/**
	 * <pre>
	 *       column
	 * def: +=========+==============+==============+
	 * row: |Left     |   center     |         Right|
	 * def  +---------+--------------+--------------+
	 * row  |         |              |              |
	 * def  +---------+--------------+--------------+
	 * row  |         | colspan=2                   |
	 *      |         |                             |
	 * def  +---------+--------------+--------------+
	 * 
	 * 
	 * @param text
	 */
	public Table(CharSequence text, int where) {
		this.text = text;
		this.index = where;
	}

	public Table(String string) {
		this(string, 0);
	}

	void parse() {
		String head = getLine();

		if (!DEF_P.matcher(head).matches()) {
			error("First line is neither a head nor a def");
		}

		this.width = head.length();

		String columns[] = head.substring(1).split("\\+");
		this.columns = new ArrayList<Column>();
		int start = 1;
		for (int i = 0; i < columns.length; i++) {
			Column c = new Column(i);
			c.cwidth = columns[i].length();
			c.cstart = start;
			c.head = columns[i].charAt(0) == '=';
			start += c.cwidth + 1;
			this.columns.add(c);
		}

		int row = 1;
		while (true) {

			List<Cell> cells = new ArrayList<>();
			int column = 0;
			int nrcells = -1;
			String line;
			while ((line = getLine()) != null && ROW_P.matcher(line).matches()) {

				if (line.length() > width) {
					error("Row is too wide");
				}
				if (line.length() < width) {
					error("Row is too small");
				}

				String parts[] = line.substring(1).split("\\|");
				
				if (parts.length > this.columns.size())
					error("Too many cells");

				if (nrcells == -1) {
					// first row
					nrcells = parts.length;

					for (int i = 0; i < nrcells; i++) {
						Cell cell = new Cell();
						cell.index = i;
						Column col = this.columns.get(column);
						cell.head = head.charAt(col.cstart) == '=';
						cell.column = col;

						boolean left = parts[i].startsWith("  ");
						boolean right = parts[i].endsWith("  ");
						if (left)
							if (right)
								cell.halign = HAlign.CENTER;
							else
								cell.halign = HAlign.LEFT;
						else if (right)
							cell.halign = HAlign.RIGHT;
						else
							cell.halign = HAlign.JUSTIFY;

						cell.text.append(parts[i]);

						int cwidth = col.cwidth;
						while (parts[i].length() > cwidth) {
							column++;
							cell.colspan++;
							cwidth += this.columns.get(column).cwidth + 1;
						}
						cells.add(cell);
						column++;
					}

				} else {
					// continuation row
					if (nrcells != parts.length) {
						error("Different nr of cells in adjacent rows");
					}

					for (int i = 0; i < nrcells; i++) {
						Cell cell = cells.get(i);
						cell.text.append(parts[i]);
					}
				}
			}
			if (!cells.isEmpty())
				table.add(cells);

			if (line == null)
				break;

			if (!DEF_P.matcher(line).matches()) {
				error("Invalid row");
			}
			head = line;
		}

	}

	private void error(String message) {
		try (Formatter f = new Formatter()) {
			f.format("%d: %s\n%s\n", line, current, message);
			throw new IllegalArgumentException(f.toString());
		}
	}

	private String getLine() {
		StringBuilder sb = new StringBuilder();
		outer: while (index < text.length()) {
			char c = text.charAt(index++);
			switch (c) {
				case '\r' :
					break;
				case '\n' :
					break outer;
				default :
					sb.append(c);
			}
		}
		line++;
		current = sb.toString().trim();
		if (current.isEmpty() && index < text.length())
			return getLine();

		if (current.isEmpty())
			return null;

		return current;
	}

	public void appendTo(Appendable out) throws IOException {
		Tag table = new Tag("table");
		table.addAttribute("class", "table table-condensed table-bordered table-striped");

		Tag colgroup = new Tag(table, "colgroup");

		for (Column c : columns) {
			colgroup.addContent(c.getTag());
		}
		
		Iterator<List<Cell>> it = this.table.iterator();
		List<Cell> row = it.next();

		if (row != null && row.get(0).head) {
			Tag thead = new Tag(table, "thead");

			while (row != null && row.get(0).head) {
				Tag tr = new Tag(thead, "tr");
				for (Cell cell : row) {
					tr.addContent(cell.getTag());
				}
				if (it.hasNext())
					row = it.next();
				else
					row = null;
			}
		}
		Tag tbody = new Tag(table,"tbody");
		while (row != null) {
			Tag tr = new Tag(tbody,"tr");
			for (Cell cell : row)
				tr.addContent(cell.getTag());

			if (it.hasNext())
				row = it.next();
			else
				row = null;
		}

		out.append(table.toString());
	}

	public static Table parse(CharSequence out, int start) {
		Table table = new Table(out, start);
		String first = table.getLine();
		if (!DEF_P.matcher(first).matches())
			return null;

		table.index = start;
		try {
			table.parse();
		}
		catch (Exception e) {
			e.printStackTrace();
			table.error = e.getMessage();
		}
		return table;
	}

	public String getError() {
		return error;
	}
}
