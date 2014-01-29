package biz.aQute.markdown;

import java.io.*;

import junit.framework.*;
import aQute.lib.io.*;

public class MarkdownTest extends TestCase {

	
	public void testHeaders() throws Exception {
		assertMdt("testdocs/markdownpro/headers.mdt");		
	}
	
	public void testMacros() throws Exception {
		assertMdt("testdocs/markdownpro/macros.mdt");		
	}
	
	public void testTable() throws Exception {
		assertMdt("testdocs/markdownpro/tables.mdt");		
	}
	
	public void testSpan() throws Exception {
		assertMdt("testdocs/markdownpro/spans.mdt");		
	}
	public void testEscaping() throws Exception {
		assertMdt("testdocs/markdownpro/escaping.mdt");		
	}
	public void testDefinitionList() throws Exception {
		assertMdt("testdocs/markdownpro/definition-list.mdt");		
	}
	
	public void testTest() throws Exception {
		assertFiles("testdocs/markdownpro/test.text");		
	}

	public void testTidyness() throws Exception {
		assertFiles("testdocs/markdown/Tidyness.text");
	}
	
	public void testTabs() throws Exception {
		assertFiles("testdocs/markdown/Tabs.text");
	}
	
	public void testAutoLinks() throws Exception {
		assertFiles("testdocs/markdown/Auto links.text");
	}

	public void testAmpsAndANgleEncoding() throws Exception {
		assertFiles("testdocs/markdown/Amps and angle encoding.text");
	}

	public void testBackslasgWithCodeBlocks() throws Exception {
		assertFiles("testdocs/markdown/Backslash escapes.text");
	}

	public void testBlockQuotesWithCodeBlocks() throws Exception {
		assertFiles("testdocs/markdown/Blockquotes with code blocks.text");
	}

	public void testHardWrappedParas() throws Exception {
		assertFiles("testdocs/markdown/Hard-wrapped paragraphs with list-like lines.text");
	}

	public void testHorizontalRules() throws Exception {
		assertFiles("testdocs/markdown/Horizontal rules.text");
	}

	public void testInlineHtmlAdvanced() throws Exception {
		assertFiles("testdocs/markdown/Inline HTML (Advanced).text");
	}

	public void testInlinHtmlSimple() throws Exception {
		assertFiles("testdocs/markdown/Inline HTML (Simple).text");
	}

	public void testInlineHtmlComments() throws Exception {
		assertFiles("testdocs/markdown/Inline HTML comments.text");
	}

	public void testLinksInlin() throws Exception {
		assertFiles("testdocs/markdown/Links, inline style.text");
	}

	public void testLinksReference() throws Exception {
		assertFiles("testdocs/markdown/Links, reference style.text");
	}

	public void testLiteralQuotesInTitles() throws Exception {
		assertFiles("testdocs/markdown/Literal quotes in titles.text");
	}

	public void testMarkdownDocumentationBasic() throws Exception {
		assertFiles("testdocs/markdown/Markdown Documentation - Basics.text");
	}

	public void testMarkdownDocSyntax() throws Exception {
		assertFiles("testdocs/markdown/Markdown Documentation - Syntax.text");
	}

	public void testNestedBlockQuotes() throws Exception {
		assertFiles("testdocs/markdown/Nested blockquotes.text");
	}

	public void testOrderedAndUnorderedLists() throws Exception {
		assertFiles("testdocs/markdown/Ordered and unordered lists.html");
	}

	public void testStrongAndEM() throws Exception {
		assertFiles("testdocs/markdown/Strong and em together.text");
	}
	
	
	public void testUnshift() throws Exception {
		assertEquals( "Tab\n\n* Tab\n* Tab\n", ListHandler.unshift("Tab\n    * Tab\n    * Tab\n").toString());
		assertEquals( "Tab\n\n* Tab\n* Tab\n", ListHandler.unshift("Tab\n    * Tab\n    * Tab\n").toString());
		assertEquals( "Tab\n\n* Tab\n    * Tab\n", ListHandler.unshift("Tab\n    * Tab\n        * Tab\n").toString());
	}
	public void testListPatterns() throws Exception {
		System.out.println(ListHandler.LIST_P.pattern().replaceAll("\n", "\\\\n"));

		assertFalse(ListHandler.LIST_P.matcher("12. a\nc\n    *\n    abc\n\n    abc\n    def\n13. def").matches());
		assertTrue(ListHandler.LIST_P.matcher("12. a\nc\n    *\n    abc\n\n    abc\n    def\n").matches());
		assertTrue(ListHandler.LIST_P.matcher("* a\nc\n    *\n    abc\n\n    abc\n    def\n").matches());
		assertFalse(ListHandler.LIST_P.matcher("* a\nc\n    *\n    abc\n\n    abc\n    def\n * b").matches());
		assertFalse(ListHandler.LIST_P.matcher("* a\nc\n    *\n * b").matches());
		assertTrue(ListHandler.LIST_P.matcher("* a\nc\n    *\n").matches());
		assertFalse(ListHandler.LIST_P.matcher("* a\n* b").matches());
		assertTrue(ListHandler.LIST_P.matcher("* a\n").matches());
	}

	public void testLists() throws Exception {
		assertFiles("testdocs/markdown/Ordered and unordered lists.text");
		assertNormalized("<ul>\n<li>a</li>\n<li>b</li>\n</ul>\n", new Markdown("* a\n* b\n").toString());
		assertFiles("testdocs/markdownpro/simple-lists.text");
	}

	public void testQuote() throws Exception {
		assertFiles("testdocs/markdownpro/nested-quotes.text");
	}

	public void testParagraph() throws Exception {
		assertNormalized("<p>x\nz</p>\n<div>\n<div>\n\nm\n</div>\n</div>\n<p>y</p>\n", new Markdown(
				"x\nz\n\n<div>\n<div>\nm\n</div>\n</div>\n\ny").toString());
		assertNormalized("<p>x\nz</p>\n<p>y</p>\n", new Markdown("x\nz\n\ny\n\n\n\n\n").toString());
		assertNormalized("<p>x\nz</p>\n<p>y</p>\n", new Markdown("x\nz\n\ny\n\n").toString());
		assertNormalized("<p>x\nz</p>\n<p>y</p>\n", new Markdown("x\nz\n\ny").toString());
		assertNormalized("<p>x</p>\n<p>y</p>\n", new Markdown("x\n\ny").toString());
	}

	public void testPara() throws Exception {
		Markdown md = new Markdown();
		assertEquals("hello <a href=\"\">w<em>or</em>ld</a>\n", md.toHtml("hello [w*or*ld]()"));
		assertEquals("hello [w[or]ld][]", md.toHtml("hello [w[or]ld][]"));
		assertEquals("hello [world][1]", md.toHtml("hello [world][1]"));
		assertEquals("hello <img src=\"xx\" title=\"title &copy;\" alt=\"world\" />",
				md.toHtml("hello ![world](xx \"title &copy;\")"));
		assertEquals("hello world \nHello world\n", md.toHtml("hello world \nHello world\n"));
		assertEquals("hello world<br />\nHello world\n",
				md.toHtml("hello world                                 \nHello world\n"));
		assertEquals("hello world<br />\nHello world\n", md.toHtml("hello world  \nHello world\n"));
		assertEquals("hello <code>w_or_ld</code>", md.toHtml("hello `w_or_ld`"));
		assertEquals("hello <code>world</code>", md.toHtml("hello `world`"));
		assertEquals("hello <a href=\"xx\" title=\"title &copy;\">world</a>\n",
				md.toHtml("hello [world](xx \"title &copy;\")"));
		assertEquals("hello <a href=\"xx\" title=\"title\">world</a>\n", md.toHtml("hello [world](xx \"title\")"));
		assertEquals("hello <a href=\"xx\">world</a>\n", md.toHtml("hello [world](xx)"));
		assertEquals("hello <a href=\"\">world</a>\n", md.toHtml("hello [world]()"));
		assertEquals("hello * world *", md.toHtml("hello * world *"));
		assertEquals("hello <code>*world*</code>", md.toHtml("hello <code>*world*</code>"));
		assertEquals("hello *world*", md.toHtml("hello \\*world\\*"));
		assertEquals("hello *world*", md.toHtml("hello \\*world\\*"));
		assertEquals("hello _world_", md.toHtml("hello \\_world\\_"));
		assertEquals("hello <em>wo<strong>rl</strong>d</em>", md.toHtml("hello _wo**rl**d_"));
		assertEquals("hello <em>world</em>", md.toHtml("hello *world*"));
		assertEquals("hello <strong>world</strong>", md.toHtml("hello __world__"));
		assertEquals("hello <strong>world</strong>", md.toHtml("hello **world**"));
		assertEquals("hello <span>world</span>", md.toHtml("hello <span>world</span>"));
		assertEquals("hello &lt;&lt; world", md.toHtml("hello << world"));
		assertEquals("hello &amp; world", md.toHtml("hello &amp; world"));
		assertEquals("hello &amp; amp; world", md.toHtml("hello & amp; world"));
		assertEquals("hello &copy; world", md.toHtml("hello &copy; world"));
		assertEquals("hello &amp; world", md.toHtml("hello & world"));
		assertEquals("hello world", md.toHtml("hello world"));
	}

	public void assertFiles(String textFile) throws Exception {
		File f = new File(textFile);
		String text = IO.collect(f);
		String htmlName = f.getAbsolutePath().replaceAll("\\.text$", ".html");
		String html = IO.collect(new File(htmlName));
		Markdown md = new Markdown();
		Configuration c = md.getConfiguration();
		c.header_number(0);
		md.parse(text);
		assertNormalized(html, md.toString());
	}

	public void assertMdt(String file) throws Exception {
		File f = new File(file);
		String parts[] = IO.collect(f).split("\n\\.{50,}\n");
		assertTrue( "Not an even number of parts in mdt test", (parts.length %2) == 0);

		for ( int i=0; i<parts.length-1; i+=2) {
			assertNormalized(parts[i+1], new Markdown(parts[i]).toString());
		}
	}

	public void assertNormalized(Object a, Object b) {
		assertEquals(normalize(a.toString()), normalize(b.toString()));
	}

	// public void testStandard() throws IOException {
	// File f= new File("testdocs/markdown");
	// for ( File sub : f.listFiles()) {
	// if ( sub.getName().endsWith(".text")) {
	// String s= IO.collect( sub);
	// Markdown md = new Markdown();
	// md.parse(s);
	//
	// String html = IO.collect( new File(
	// sub.getAbsolutePath().replace("\\.text$", ".html")));
	// assertEquals( sub.getName(), html, md.toString());
	// }
	// }
	// }
	private String normalize(String html) {
		StringBuilder sb = new StringBuilder(html.length());
		boolean intag = false;
		boolean nonspace = false;
		boolean prevspace = false;

		for (int i = 0; i < html.length(); i++) {
			char c = html.charAt(i);
			switch (c) {
				case '\n' :
					nonspace = true;
					break;

				case '\r' :
					break;

				case ' ' :
				case '\t' :
					if (!nonspace && !prevspace) {
						prevspace = true;
						sb.append(" ");
					}
					break;

				case '\'' :
					if (intag)
						sb.append('"');
					else
						sb.append('\'');
					break;

				case '<' :
					prevspace = true;
					intag = true;
					sb.append('<');
					break;

				case '>' :
					nonspace = true;
					intag = false;
					sb.append(">\n");
					break;

				default :
					nonspace = true;
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}
}
