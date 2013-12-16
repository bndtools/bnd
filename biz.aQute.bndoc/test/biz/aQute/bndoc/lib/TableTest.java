package biz.aQute.bndoc.lib;

import java.io.*;

import junit.framework.*;

public class TableTest extends TestCase {

	public void testLarger() throws Exception {
		String s = ""//
				+ "+===============+===================+======================+\n" //
				+ "| Head 1        | Head 2            | Head 3               |\n" //
				+ "+---------------+-------------------+----------------------+\n" //
				+ "| Text 1        | spanning 2 cells                         |\n" //
				+ "+---------------+-------------------+----------------------+\n" //
				+ "| Text 1        | spanning 2 cells                         |\n" //
				+ "| Text 1'       | spanning 2 cells'                        |\n" //
				+ "+---------------+-------------------+----------------------+\n" //
		;

		Table t = new Table(s);
		t.parse();
		t.appendTo(System.out);
	}

	public void testSpan() throws Exception {
		String s = ""//
				+ "+-+-+--+\n" //
				+ "| |    |\n" //
				+ "+-+-+--+\n" //
		;

		Table t = new Table(s);
		t.parse();
		t.appendTo(System.out);
	}

	public void testSimple() throws IOException {
		Table table = new Table("" //
				+ "+=+=+\n" //
				+ "|h|h|\n" //
				+ "+-+-+\n" //
				+ "|A|B|\n" //
				+ "+-+-+");
		table.parse();
		table.appendTo(System.out);
	}
}
