package test;

import java.io.*;

import junit.framework.*;
import aQute.lib.index.*;

public class TestIndex extends TestCase {

	
	public void testPersistence() throws Exception {
		File test = new File("test.tmp");
		test.delete();
		Index index = new Index(test, 2000);
		index.insert( new byte[]{1}, 2);
		index.insert( new byte[]{2}, 4);
		index.insert( new byte[]{3}, 6);
		assertEquals( 2, index.search(new byte[] {1}));
		assertEquals( 4, index.search(new byte[] {2}));
		assertEquals( 6, index.search(new byte[] {3}));
		System.out.println(index.toString());
		index.close();
		
		index = new Index(test, 2000);
		System.out.println(index.toString());
		assertEquals( 2, index.search(new byte[] {1}));
		assertEquals( 4, index.search(new byte[] {2}));
		assertEquals( 6, index.search(new byte[] {3}));
		
		index.close();
	}
	
	
	
	public void testBasic() throws Exception {
		File test = new File("test.tmp");
		test.delete();
		Index index = new Index(test, 2000);
		
		index.insert( new byte[] {12}, 24);
		index.insert( new byte[] {13}, 26);
		index.insert( new byte[] {6}, 12);
		index.insert( new byte[] {16}, 32);
		index.insert( new byte[] {1}, 2);

		assertEquals( 24, index.search(new byte[] {12}));
		assertEquals( 26, index.search(new byte[] {13}), 26);
		assertEquals( 12, index.search(new byte[] {6}), 12);
	}

	public void testMany() throws Exception {
		File test = new File("test.tmp");
		test.delete();
		Index index = new Index(test, 1000);
		for ( int i = 1; i<127; i++)
			index.insert( new byte[]{(byte)i}, i*2);
		
		index.close();
		Index index2 = new Index(test, 1000);
		for ( int i = 1; i<127; i++)
			assertEquals(i*2, index2.search( new byte[(byte)i]));
		
	}

}
