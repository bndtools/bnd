package aQute.lib.index;

import java.io.File;

import aQute.lib.io.IO;
import junit.framework.TestCase;

public class TestIndex extends TestCase {

	public void testPersistence() throws Exception {
		File test = IO.getFile("tmp/" + getName() + ".tmp");
		test.delete();
		test.getParentFile()
			.mkdirs();
		Index index = new Index(test, 2000);
		try {
			index.insert(new byte[] {
				1
			}, 2);
			index.insert(new byte[] {
				2
			}, 4);
			index.insert(new byte[] {
				3
			}, 6);
			assertEquals(2, index.search(new byte[] {
				1
			}));
			assertEquals(4, index.search(new byte[] {
				2
			}));
			assertEquals(6, index.search(new byte[] {
				3
			}));
			System.err.println(index.toString());
		} finally {
			index.close();
		}

		index = new Index(test, 2000);
		try {
			System.err.println(index.toString());
			assertEquals(2, index.search(new byte[] {
				1
			}));
			assertEquals(4, index.search(new byte[] {
				2
			}));
			assertEquals(6, index.search(new byte[] {
				3
			}));

			index.close();

		} finally {
			index.close();
			IO.delete(test.getParentFile());
		}
	}

	public void testBasic() throws Exception {
		File test = IO.getFile("tmp/" + getName() + ".tmp");
		test.delete();
		test.getParentFile()
			.mkdirs();
		Index index = new Index(test, 2000);
		try {
			index.insert(new byte[] {
				12
			}, 24);
			index.insert(new byte[] {
				13
			}, 26);
			index.insert(new byte[] {
				6
			}, 12);
			index.insert(new byte[] {
				16
			}, 32);
			index.insert(new byte[] {
				1
			}, 2);

			assertEquals(24, index.search(new byte[] {
				12
			}));
			assertEquals(26, index.search(new byte[] {
				13
			}), 26);
			assertEquals(12, index.search(new byte[] {
				6
			}), 12);
		} finally {
			index.close();
			IO.delete(test.getParentFile());
		}
	}

	// public void testMany() throws Exception {
	// File test = IO.getFile("tmp/"+getName()+".tmp");
	// test.delete();
	// Index index = new Index(test, 1000);
	// for ( int i = 1; i<127; i++)
	// index.insert( new byte[]{(byte)i}, i*2);
	//
	// index.close();
	// Index index2 = new Index(test, 1000);
	// for ( int i = 1; i<127; i++)
	// assertEquals(i*2, index2.search( new byte[(byte)i]));
	//
	// }

}
