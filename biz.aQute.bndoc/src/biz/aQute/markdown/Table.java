package biz.aQute.markdown;

import java.util.*;
import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.CompositeBlock;

class Table extends Markdown.Block {
	static Pattern LINE_P = Pattern.compile(".+", Pattern.UNIX_LINES);
	
	ArrayList<Column>	columns	= new ArrayList<>();
	ArrayList<Row>		heads	= new ArrayList<>();
	ArrayList<Row>		rows	= new ArrayList<>();
	private Markdown	md;
	String caption;

	enum HAlign {
		left, right, center;
	}

	static class Column {
		int		cend;
		int		cstart;
		int		column;
		HAlign	align;

		void alignment(CharSequence line) {
			Matcher m = LINE_P.matcher(line);
			if ( !m.lookingAt())
				return;
			
			int start = Math.min(m.end(), cstart);
			int end = Math.min(m.end(), cend);
			if ( start + 2 >= end)
				return;

			boolean left = line.charAt(start) != ' ' || line.charAt(start+1) != ' ';
			boolean right = end >= cend -1 && (line.charAt(end-1) != ' ' || line.charAt(end-2) != ' ');
			if ( left == right)
				align = HAlign.center;
			else if ( left )
				align = HAlign.left;
			else if ( right)
				align = HAlign.right;
		}
	}

	static class Cell {
		final Column	column;
		boolean			head;
		int				colspan	= 1;
		StringBuilder	text	= new StringBuilder();
		HAlign			halign;

		public Cell(Column column) {
			this.column = column;
		}

		public void extract(CharSequence line) {
			if (line.length() <= column.cstart)
				return;

			text.append(line, column.cstart, Math.min(line.length(), column.cend));
			while ( text.length() > 0 && text.charAt(text.length()-1)==' ') {
				text.delete(text.length()-1, text.length());
			}
			text.append("\n");
		}
	}

	class Row {
		Cell[]	cells	= new Cell[columns.size()];
		boolean	head;
		{
			for (int i = 0; i < cells.length; i++)
				cells[i] = new Cell(columns.get(i));

		}
	}

	public Table(Markdown md) {
		super("table");
		this.md = md;
	}

	/**
	 * Calculate column start/end from a a regular expression. We search for the
	 * regular exprs and every instance is regarded as a column definition.
	 * 
	 * @param string
	 * @param dashes
	 */
	public void columns(Pattern columnDef, String def) {
		Matcher m = columnDef.matcher(def);
		int n = 0;
		while (m.find()) {
			Column c = new Column();
			c.cstart = m.start();
			c.cend = m.end();

			char left = def.charAt(m.start(0));
			char right = def.charAt(m.end(0)-1);
			if ( left ==':' )
				if ( right == ':')
					c.align = HAlign.center;
				else
					c.align = HAlign.left;
			else if ( right == ':')
				c.align = HAlign.right;
			
			columns.add(c);
		}
	}

	public void addHead(String lines) {
		Row row = new Row();
		row.head = true;
		for (String line : lines.split("\n")) {
			for (Cell cell : row.cells) {
				cell.extract(line);
				cell.head = true;
			}
		}
		heads.add(row);
	}

	public void addRow(String lines) {
		Row row = new Row();
		for (String line : lines.split("\n")) {
			for (Cell cell : row.cells) {
				cell.extract(line);
			}
		}
		rows.add(row);
	}

	public void addHead(String[] cells) {
		Row row = new Row();
		row.head = true;
		int i=0;
		for (Cell cell : row.cells) {
			if ( i < cells.length)
				cell.text.append(cells[i].trim());
			cell.head = true;
			i++;
		}
		heads.add(row);
	}

	public void addRow(String[] cells) {
		Row row = new Row();
		row.head = false;
		int i=0;
		for (Cell cell : row.cells) {
			if ( i < cells.length)
				cell.text.append(cells[i].trim());
			i++;
		}
		rows.add(row);
	}

	public void alignment(String head) {
		for (Column column : columns) {
			column.alignment(head);
		}
	}

	public void append(Formatter f) {
		try {
			super.beginTag(f);
			if ( caption != null) {
				Block p = md.paragraph(caption, "caption");
				p.append(f);
			}
			f.format("<colgroup>");
			for (Column col : columns) {
				if ( col.align != null)
					f.format("<col style='width:%sem;' class=\"%s\"/>\n", col.cend-col.cstart, col.align);
				else
					f.format("<col style='width:%sem;'/>\n", col.cend-col.cstart, col.align);
			}
			f.format("</colgroup>\n");
			String group = null;

			appendRows(f, "thead", heads);
			appendRows(f, "tbody", rows);

			super.endTag(f);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void appendRows(Formatter f, String group, ArrayList<Row> rows) throws Exception {
		if (rows.isEmpty())
			return;

		f.format("<%s>\n", group);
		for (Row row : rows) {
			f.format("  <tr>\n");
			for (Cell cell : row.cells) {
				String tag = row.head ? "th" : "td";
				HAlign align = cell.halign;
				
				if ( align == null)
					f.format("  <%s>", tag);
				else
					f.format("  <%s class=\"%s\">", tag, align);
				
				String s = cell.text.toString();
				CompositeBlock cb = md.parseComposite(null,cell.text);
				cb.compress();
				cb.append(f);
				f.format("  </%s>\n", tag);
			}
			f.format("  </tr>\n");
		}
		f.format("</%s>\n", group);
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

}
