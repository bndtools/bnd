package biz.aQute.markdown;

import java.util.regex.*;

import biz.aQute.markdown.Markdown.Block;
import biz.aQute.markdown.Markdown.Rover;

public class TableHandler implements Markdown.Handler {
	static class Column {
		int	start;
		int	end;
		int	align;
	}

	static class Cell {
		StringBuilder	content	= new StringBuilder();
	}

	static Pattern		WIDTHS_P		= Pattern.compile(":?-+:?");

	/*
	 * A caption may optionally be provided (as illustrated in the example
	 * above). A caption is a paragraph beginning with the string Table: (or
	 * just :), which will be stripped off. It may appear either before or after
	 * the table.
	 */

	static Pattern		CAPTION_P		= Pattern.compile("((Table)?: *(?<caption>(.+\n)+)\n)");

	// @formatter:off
	//     Right     Left     Center     Default
	//   -------     ------ ----------   -------
	//        12     12        12            12
	//       123     123       123          123
	//         1     1          1             1
	//   -------     ------ ----------   ------- ] optional
	// @formatter:on
	static Pattern		SIMPLE_TABLE_P	= Pattern.compile(//
												/*
												 * The headers and table rows
												 * must each fit on one line.
												 * Column alignments are
												 * determined by the position of
												 * the header text relative to
												 * the dashed line below it.
												 */
												"(?<head> *[^-=+:#<>\n][^:#\n]+)\n"
												/*
												 * Dashed line for column widths
												 */
												+ "(?<widths>-+( +-+)+) *\n"
												/*
												 * Rows
												 */
												+ "(?<rows>(\\s*[^-=+].+\n)+)"
												/*
												 * The table must end with a
												 * blank line, or a line of
												 * dashes followed by a blank
												 * line.
												 */
												+ "(-+( +-+)* *\n)?"
												/*
												 * A blank line
												 */
												+ "\n+");

	// @formatter:off
	//   -------     ------ ----------   -------
	//        12     12        12            12
	//       123     123       123          123
	//         1     1          1             1
	//   -------     ------ ----------   -------

	//   --------------------------------------- ]
	//      Head     Head      Head       Head   ] optional
	//   -------     ------ ----------   ------- 
	//        12     12        12            12
	//       123     123       123          123
	//         1     1          1             1
	//   -------     ------ ----------   -------
	// @formatter:on

	static Pattern		MULTILINE_P		= Pattern.compile(
										/*
										 * Multiline tables always start with
										 * dashes, However, these are either the
										 * header start or the widths
										 */
										"(-+( +-+)* *\n(?<head>([^-].+\n){1,5}))?"
										/*
										 * Dashed line for column widths
										 */
										+ "(?<widths>-+( +-+)+) *\n"
										/*
										 * Rows
										 */
										+ "(?<rows>([^-=+].+\n)+\n?)"
										/*
										 * The table must end with a blank line,
										 * or a line of dashes followed by a
										 * blank line.
										 */
										+ "(-+( +-+)* *\n)"
										/*
										 * A blank line
										 */
										+ "\n+");

	// @formatter: off
	// +----+----+----+
	// |A | B| C |
	// +====+====+====+
	// |a |b | c |
	// +----+----+----+
	// |a |b | c |
	// +----+----+----+
	// @formatter: on

	static Pattern		GRID_P			= Pattern.compile(
										/*
										 * The beginning
										 */
										"(?<widths>\\+(-+\\+)+) *\n"
										/*
										 * Header lines
										 */
										+ "(" + "(?<head>(\\|([^\n|]+\\|)+ *\n)+)"
										/*
										 * === line defines header
										 */
										+ "\\+(=+\\+)+ *\n"
										/*
										 * and optional
										 */
										+ ")?"
										/*
										 * rows
										 */
										+ "(?<rows>(([|].+[|] *\n)+\\+(-+\\+)+ *\n)+)");

	// @formatter: off
	// | Right | Left | Default | Center |
	// |------:|:-----|---------|:------:|
	// | 12 | 12 | 12 | 12 |
	// | 123 | 123 | 123 | 123 |
	// | 1 | 1 | 1 | 1 |
	// @formatter: on

	static Pattern		PIPE_P			= Pattern.compile(
										/*
										 * The head is optional
										 */
										"((?<head>[|]?[^\n|+-][^\n|+]*([|][^-\n|+][^\n|+]*)+[|]?) *\n)?"
										/*
										 * The width defs line is mandatory
										 */
										+ "(?<widths>[|]?:?-+:?([|+]:?-+:?)+[|]? *\n)"
										/*
										 * Text lines
										 */
										+ "(?<rows>([|]?[^\n|]+([|][^\n|]+)+[|]? *\n)+)");

	/*
	 * Used to remove the pipes at the sides of a table since they are optional
	 */
	static Pattern		PIPE_ROW_P		= Pattern.compile("\\|?(.*)\\|? *");
	static Pattern		COLDEF_P		= Pattern.compile(":?-+:?");
	static Pattern		TABLE_EXT_P		= Pattern.compile("\\[ *Table* (: *(?<caption>.*) *)?\\] *\n");

	private Markdown	markdown;

	@Override
	public Block process(Rover rover) throws Exception {
		if (rover.at(TABLE_EXT_P) || rover.at(CAPTION_P)) {
			String caption = rover.group("caption");
			rover.next();
			Table table = tables(rover);
			if (table != null) {
				table.setCaption(caption);
				return table;
			}
			rover.error("Table extension found but no table pattern matched");
		} else {
			Table table = tables(rover);
			if (table != null) {
				if (rover.at(CAPTION_P)) {
					table.setCaption(rover.group("caption"));
					rover.consume();
				}
				return table;
			}
		}

		return null;
	}

	private Table tables(Rover rover) {
		if (rover.at(SIMPLE_TABLE_P) || rover.at(MULTILINE_P))
			return fixedTable(rover, "\n\n");

		if (rover.at(GRID_P))
			return fixedTable(rover, "\\+(-+\\+)+ *\n");

		if (rover.at(PIPE_P))
			return pipeTable(rover);

		return null;
	}

	private Table pipeTable(Rover rover) {
		String head = rover.group("head");
		String widths = rover.group("widths");
		String rows = rover.group("rows");

		Table table = new Table(markdown);
		String[] splitted = rows.split("\n");

		table.columns(COLDEF_P, widths);

		if (head != null)
			table.addHead(pipeSplit(head));

		for (String row : splitted)
			table.addRow(pipeSplit(row));

		rover.next();
		return table;
	}

	private String[] pipeSplit(String head) {
		Matcher m = PIPE_ROW_P.matcher(head);
		m.matches();
		String cells[] = m.group(1).split(" *\\| *");
		return cells;
	}

	private Table fixedTable(Rover rover, String rowbreak) {

		String head = rover.group("head");
		String widths = rover.group("widths");
		String rows = rover.group("rows");

		Table table = new Table(markdown);

		String[] splitted = rows.split(rowbreak);
		if (splitted.length == 1)
			splitted = rows.split("\n");

		table.columns(COLDEF_P, widths);

		if (head != null) {
			table.addHead(head);
		}

		if (!widths.contains(":"))
			if (head != null)
				table.alignment(head);
			else
				table.alignment(splitted[0]);

		for (String row : splitted) {
			table.addRow(row);
		}
		rover.next();
		return table;
	}

	@Override
	public void configure(Markdown markdown) {
		this.markdown = markdown;
	}
}
