package aQute.libg.glob;

import junit.framework.TestCase;

public class AntGlobTest extends TestCase {
	public void testSimple() {
		Glob glob;

		glob = new AntGlob("*foo*");
		assertTrue(glob.matcher("foo")
			.matches());
		assertTrue(glob.matcher("food")
			.matches());
		assertTrue(glob.matcher("Xfoo")
			.matches());
		assertTrue(glob.matcher("XfooY")
			.matches());
		assertFalse(glob.matcher("ood")
			.matches());
		assertFalse(glob.matcher("/foo")
			.matches());
		assertFalse(glob.matcher("foo/")
			.matches());

		glob = new AntGlob("?foo?");
		assertFalse(glob.matcher("foo")
			.matches());
		assertFalse(glob.matcher("food")
			.matches());
		assertFalse(glob.matcher("Xfoo")
			.matches());
		assertTrue(glob.matcher("XfooY")
			.matches());
		assertFalse(glob.matcher("ood")
			.matches());
		assertFalse(glob.matcher("/foo")
			.matches());
		assertFalse(glob.matcher("foo/")
			.matches());

		glob = new AntGlob("/?abc/*/*.java");
		assertTrue(glob.matcher("/xabc/foobar/test.java")
			.matches());
		assertFalse(glob.matcher("xabc/foobar/test.java")
			.matches());
		assertFalse(glob.matcher("/xabc/foobar/test_java")
			.matches());

		glob = new AntGlob("*");
		assertTrue(glob.matcher("test")
			.matches());
		assertFalse(glob.matcher("org/apache/jakarta/test/ant/CVS/Entries")
			.matches());
		assertFalse(glob.matcher("org/apache/test")
			.matches());
		assertFalse(glob.matcher("org/apache/test/")
			.matches());
		assertFalse(glob.matcher("org\\apache\\test")
			.matches());
		assertFalse(glob.matcher("org\\apache\\test\\")
			.matches());
		assertFalse(glob.matcher("test/apache/jakarta")
			.matches());
		assertFalse(glob.matcher("org/apache/jakarta")
			.matches());
	}

	public void testMultiple() {
		Glob glob;

		glob = new AntGlob("**/CVS/*");
		assertTrue(glob.matcher("CVS/Repositories")
			.matches());
		assertTrue(glob.matcher("org/apache/CVS/Entries")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta/tools/ant/CVS/Entries")
			.matches());
		assertFalse(glob.matcher("org/apache/CVS/foo/bar/Entries")
			.matches());
		assertTrue(glob.matcher("/CVS/Repositories")
			.matches());
		assertTrue(glob.matcher("CVS/")
			.matches());
		assertTrue(glob.matcher("/CVS/")
			.matches());
		assertFalse(glob.matcher("CVS")
			.matches());
		assertFalse(glob.matcher("/CVS")
			.matches());
		assertTrue(glob.matcher("xxx/CVS/")
			.matches());

		glob = new AntGlob("org/apache/jakarta/**");
		assertTrue(glob.matcher("org/apache/jakarta/tools/ant/docs/index.html")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta/test.xml")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta/")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta")
			.matches());
		assertFalse(glob.matcher("org/apache/xyz.java")
			.matches());

		glob = new AntGlob("org/apache/**/CVS/*");
		assertTrue(glob.matcher("org/apache/CVS/Entries")
			.matches());
		assertTrue(glob.matcher("org/apache/CVS/")
			.matches());
		assertTrue(glob.matcher("org\\apache\\CVS\\")
			.matches());
		assertFalse(glob.matcher("org/apache/CVS")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta/tools/ant/CVS/Entries")
			.matches());
		assertFalse(glob.matcher("org/apache/CVS/foo/bar/Entries")
			.matches());

		glob = new AntGlob("**/test/**");
		assertTrue(glob.matcher("test")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta/test/ant/CVS/Entries")
			.matches());
		assertTrue(glob.matcher("org/apache/test")
			.matches());
		assertTrue(glob.matcher("org/apache/test/")
			.matches());
		assertTrue(glob.matcher("org\\apache\\test")
			.matches());
		assertTrue(glob.matcher("org\\apache\\test\\")
			.matches());
		assertTrue(glob.matcher("test/apache/jakarta")
			.matches());
		assertFalse(glob.matcher("org/apache/jakarta")
			.matches());

		glob = AntGlob.ALL;
		assertTrue(glob.matcher("test")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta/test/ant/CVS/Entries")
			.matches());
		assertTrue(glob.matcher("org/apache/test")
			.matches());
		assertTrue(glob.matcher("org/apache/test/")
			.matches());
		assertTrue(glob.matcher("org\\apache\\test")
			.matches());
		assertTrue(glob.matcher("org\\apache\\test\\")
			.matches());
		assertTrue(glob.matcher("test/apache/jakarta")
			.matches());
		assertTrue(glob.matcher("org/apache/jakarta")
			.matches());

		glob = new AntGlob("**/CVS/");
		assertTrue(glob.matcher("CVS/Repositories")
			.matches());
		assertTrue(glob.matcher("/CVS/Repositories")
			.matches());
		assertTrue(glob.matcher("xxx/CVS/Repositories")
			.matches());
		assertTrue(glob.matcher("CVS/")
			.matches());
		assertTrue(glob.matcher("CVS")
			.matches());
		assertTrue(glob.matcher("/CVS/")
			.matches());
		assertTrue(glob.matcher("/CVS")
			.matches());
		assertTrue(glob.matcher("xxx/CVS/")
			.matches());
		assertTrue(glob.matcher("CVS/xx/yy")
			.matches());
		assertTrue(glob.matcher("/CVS/xx/yy")
			.matches());
		assertTrue(glob.matcher("xxx/CVS/xx/yy")
			.matches());

		glob = new AntGlob("/test/**");
		assertTrue(glob.matcher("/test/x.java")
			.matches());
		assertTrue(glob.matcher("/test/foo/bar/xyz.html")
			.matches());
		assertFalse(glob.matcher("/xyz.xml")
			.matches());

		glob = new AntGlob("**/**");
		assertTrue(glob.matcher("/test/x.java")
			.matches());
		assertTrue(glob.matcher("/test/foo/bar/xyz.html")
			.matches());
		assertTrue(glob.matcher("test/foo/bar/xyz.html")
			.matches());
		assertTrue(glob.matcher("test/xyz.xml")
			.matches());
		assertTrue(glob.matcher("/xyz.xml")
			.matches());
		assertTrue(glob.matcher("/xyz.xml/")
			.matches());
		assertTrue(glob.matcher("xyz.xml/")
			.matches());
		assertTrue(glob.matcher("xyz.xml")
			.matches());

		glob = new AntGlob("**\\apache\\test/**");
		assertTrue(glob.matcher("org/apache/test/ant/CVS/Entries")
			.matches());
		assertTrue(glob.matcher("org/apache/test")
			.matches());
		assertTrue(glob.matcher("org/apache/test/")
			.matches());
		assertTrue(glob.matcher("org\\apache\\test")
			.matches());
		assertTrue(glob.matcher("org\\apache\\test\\")
			.matches());
		assertFalse(glob.matcher("org/apache/jakarta")
			.matches());

		glob = new AntGlob("\\**");
		assertTrue(glob.matcher("/CVS")
			.matches());
		assertTrue(glob.matcher("/CVS/Foo")
			.matches());
		assertTrue(glob.matcher("\\CVS")
			.matches());
		assertFalse(glob.matcher("CVS")
			.matches());

		glob = new AntGlob("\\**\\");
		assertTrue(glob.matcher("/CVS")
			.matches());
		assertTrue(glob.matcher("/CVS/Foo")
			.matches());
		assertTrue(glob.matcher("\\CVS")
			.matches());
		assertFalse(glob.matcher("CVS")
			.matches());
	}

	public void testEscape() {
		Glob glob;

		glob = new AntGlob("(CVS)/*");
		assertTrue(glob.matcher("(CVS)/Repositories")
			.matches());
		assertFalse(glob.matcher("CVS/Repositories")
			.matches());

		glob = new AntGlob("[abc]");
		assertFalse(glob.matcher("a")
			.matches());
		assertTrue(glob.matcher("[abc]")
			.matches());

		glob = new AntGlob("abc{2}");
		assertFalse(glob.matcher("abcc")
			.matches());
		assertTrue(glob.matcher("abc{2}")
			.matches());

		glob = new AntGlob("abc|def");
		assertFalse(glob.matcher("abc")
			.matches());
		assertFalse(glob.matcher("def")
			.matches());
		assertTrue(glob.matcher("abc|def")
			.matches());

		glob = new AntGlob("abc,def");
		assertFalse(glob.matcher("abc")
			.matches());
		assertFalse(glob.matcher("def")
			.matches());
		assertTrue(glob.matcher("abc,def")
			.matches());

		glob = new AntGlob("^abc");
		assertFalse(glob.matcher("abc")
			.matches());
		assertTrue(glob.matcher("^abc")
			.matches());

		glob = new AntGlob("abc$");
		assertFalse(glob.matcher("abc")
			.matches());
		assertTrue(glob.matcher("abc$")
			.matches());

		glob = new AntGlob("abc+");
		assertFalse(glob.matcher("abc")
			.matches());
		assertFalse(glob.matcher("abcc")
			.matches());
		assertTrue(glob.matcher("abc+")
			.matches());
	}
}
