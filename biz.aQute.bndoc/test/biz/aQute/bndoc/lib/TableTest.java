package biz.aQute.bndoc.lib;

import java.io.*;

import junit.framework.*;

public class TableTest extends TestCase {
	// 0000000000111111111122222222223333333333444444444455555555556666666666
	// 0123456789012345678901234567890123456789012345678901234567890123456789
	String	t1	= ""//
						+ "+===============+===================+======================+\n" //
						+ "| Head 1        | Head 2            | Head 3               |\n" //
						+ "+---------------+-------------------+----------------------+\n" //
						+ "| Text 1        | _spanning_ 2 cells                       |\n" //
						+ "+---------------+-------------------+----------------------+\n" //
						+ "| Text 1        | spanning 2 cells                         |\n" //
						+ "| Text 1'       | spanning 2 cells'                        |\n" //
						+ "+---------------+-------------------+----------------------+\n" //
				;

	public void testLarger() throws Exception {

		Table t = new Table(t1);
		t.parse();
		t.appendTo(System.out);
		assertEquals(60, t.width);
		assertEquals(3, t.columns.size());
		assertEquals(1, t.columns.get(0).cstart);
		assertEquals(15, t.columns.get(0).cwidth);
		assertEquals(17, t.columns.get(1).cstart);
		assertEquals(19, t.columns.get(1).cwidth);
		assertEquals(37, t.columns.get(2).cstart);
		assertEquals(22, t.columns.get(2).cwidth);

		assertEquals(" Head 1        ", t.table.get(0).get(0).text.toString());
		assertEquals(" Head 2            ", t.table.get(0).get(1).text.toString());
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

	public void testEmbedded() throws IOException {
		StringBuilder sb = new StringBuilder(" Hello world\n" + t1 + "\nHello world");
		Table.doTables(sb);
		System.out.println(sb);
	}

	static String	SIMPLE_TABLE	= //
									"  Right     Left     Center     Default\n" //
											+ "-------     ------ ----------   -------\n" //
											+ "12     12        12            12\n" //
											+ "123     123       123          123\n" //
											+ "1     1          1             1\n"; //

	public void testSimpleTable() throws Exception {
		StringBuilder sb = new StringBuilder(" Hello world\n" + SIMPLE_TABLE + "\nHello world");
		Table.doTables(sb);
		System.out.println(sb);
	}
}
