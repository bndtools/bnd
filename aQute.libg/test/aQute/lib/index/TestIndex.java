package aQute.lib.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class TestIndex {

	@Test
	public void testPersistence(@InjectTemporaryDirectory
	File test) throws Exception {
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

	@Test
	public void testBasic(@InjectTemporaryDirectory
	File test) throws Exception {
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

}
