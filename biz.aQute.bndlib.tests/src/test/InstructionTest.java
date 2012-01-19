package test;

import java.util.*;

import junit.framework.*;
import aQute.lib.osgi.*;

public class InstructionTest extends TestCase {

	
	public void testSelect() {
		assertEquals( Arrays.asList("a","c"), new Instructions("b").reject( Arrays.asList("a","b","c")));
		assertEquals( Arrays.asList("a","c"), new Instructions("a,c").select( Arrays.asList("a","b","c")));
		assertEquals( Arrays.asList("a","c"), new Instructions("!b,*").select( Arrays.asList("a","b","c")));
	}
	
	
	
	public void testWildcard() {
		assertTrue( new Instruction("com.foo.*").matches("com.foo"));
		assertTrue( new Instruction("com.foo.*").matches("com.foo.bar"));
		assertTrue( new Instruction("com.foo.*").matches("com.foo.bar.baz"));

		assertTrue( new Instruction("!com.foo.*").matches("com.foo"));
		assertTrue( new Instruction("!com.foo.*").matches("com.foo.bar"));
		assertTrue( new Instruction("!com.foo.*").matches("com.foo.bar.baz"));

		assertTrue( new Instruction("com.foo.*~").matches("com.foo"));
		assertTrue( new Instruction("com.foo.*~").matches("com.foo.bar"));
		assertTrue( new Instruction("com.foo.*~").matches("com.foo.bar.baz"));
		
		assertTrue( new Instruction("!com.foo.*~").matches("com.foo"));
		assertTrue( new Instruction("!com.foo.*~").matches("com.foo.bar"));
		assertTrue( new Instruction("!com.foo.*~").matches("com.foo.bar.baz"));
		
		assertTrue( new Instruction("com.foo.*~").isDuplicate());
		assertTrue( new Instruction("!com.foo.*~").isDuplicate());
		assertTrue( new Instruction("!com.foo.*~").isNegated());
		
	}


	public void testLiteral() {
		assertTrue( new Instruction("literal").isLiteral());
		assertTrue( new Instruction("literal").matches("literal"));
		assertTrue( new Instruction("!literal").matches("literal"));
		assertTrue( new Instruction("=literal").matches("literal"));
		assertTrue( new Instruction("literal~").matches("literal"));
		assertTrue( new Instruction("!literal~").matches("literal"));
		assertTrue( new Instruction("=literal~").matches("literal"));
		assertFalse( new Instruction("=literal").matches(""));
		assertFalse( new Instruction("!literal").matches(""));
		assertFalse( new Instruction("literal").matches(""));
		assertTrue( new Instruction("literal").isLiteral());
		assertTrue( new Instruction("=literal").isLiteral());
		assertTrue( new Instruction("!literal").isNegated());
		assertTrue( new Instruction("!=literal").isNegated());
		assertTrue( new Instruction("=*********").isLiteral());
	}
}
