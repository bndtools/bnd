package org.bndtools.templating.engine;


import org.junit.Test;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupDir;

// This doesn't actually test anything, but can be used for experimenting with StringTemplate.
public class STTests {

	@Test
	public void test() {
		STGroupDir group = new STGroupDir("testdata/stuff", '$', '$');
		ST st = group.getInstanceOf("foo");
		st.add("name", "Neil");
		
		System.out.println(st.render());
	}

}
