package biz.aQute.bnd.reporter.codesnippet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class JavaSnippetReaderTest extends TestCase {

	public void testEmpty() {
		final List<Snippet> r = execute("JavaSnippetReaderTest");
		assertTrue(r.isEmpty());
	}

	public void testEmptyOnMethod() {
		final List<Snippet> r = execute("EmptyOnMethod");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "print", null, null,
			"public void print() {\n" + "  System.out.println(\"test\");\n" + "}");
	}

	public void testEmptyOnType() {
		final List<Snippet> r = execute("EmptyOnType");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "EmptyOnType", null, null,
			"import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;\n" + "\n" + "public class EmptyOnType {\n"
				+ "\n" + "  final MyClass test = new MyClass();\n" + "\n" + "  public void print() {\n"
				+ "    System.out.println(test);\n" + "  }\n" + "}");
	}

	public void testFullOnMethod() {
		final List<Snippet> r = execute("FullOnMethod");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "myPrint", "Test", "test\n\n test.",
			"import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;\n" + "\n" + "public void print() {\n"
				+ "  final MyClass c = new MyClass();\n" + "  System.out.println(c.toString());\n" + "}");
	}

	public void testFullOnType() {
		final List<Snippet> r = execute("FullOnType");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "myFullOnType", "Test", "test {}.",
			"import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;\n" + "\n"
				+ "public abstract class FullOnType {\n" + "\n" + "  String test = \"test\";\n" + "\n"
				+ "  public void print() {\n" + "    final MyClass c = new MyClass();\n"
				+ "    System.out.println(c.toString());\n" + "  }\n" + "}");
	}

	public void testOnlyContentMethod() {
		final List<Snippet> r = execute("OnlyContentMethod");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "print", null, null,
			"// Comment\n" + "final MyClass c = new MyClass();\n" + "System.out.println(c.toString());");
	}

	public void testOnlyContentType() {
		final List<Snippet> r = execute("OnlyContentType");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "OnlyContentType", null, null,
			"String test = \"test\";\n" + "\n" + "public void print() {\n" + "  // Comment\n"
				+ "  final MyClass c = new MyClass();\n" + "  System.out.println(c.toString());\n" + "}");
	}

	public void testWithoutDeclarationMethod() {
		final List<Snippet> r = execute("WithoutDeclarationMethod");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "print", null, null,
			"import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;\n" + "\n" + "// Comment\n"
				+ "final MyClass c = new MyClass();\n" + "System.out.println(c.toString());");
	}

	public void testWithoutDeclarationType() {
		final List<Snippet> r = execute("WithoutDeclarationType");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "WithoutDeclarationType", null, null,
			"import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;\n" + "\n" + "String test = \"test\";\n"
				+ "\n" + "/**\n" + " */\n" + "public void print() {\n" + "  // Comment\n"
				+ "  final MyClass c = new MyClass();\n" + "  System.out.println(c.toString());\n" + "}");
	}

	public void testWithoutImportMethod() {
		final List<Snippet> r = execute("WithoutImportMethod");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "print", null, null,
			"/**\n" + " * My Comment.\n" + " */\n" + "public void print() {\n" + "  final MyClass c = new MyClass();\n"
				+ "  System.out.println(c.toString());\n" + "}");
	}

	public void testWithoutImportType() {
		final List<Snippet> r = execute("WithoutImportType");

		assertEquals(1, r.size());
		checkSnippet(r.get(0), null, "WithoutImportType", null, null,
			"public class WithoutImportType {\n" + "\n" + "  String test = \"test\";\n" + "\n"
				+ "  public void print() {\n" + "    // Comment\n" + "    final MyClass c = new MyClass();\n"
				+ "    System.out.println(c.toString());\n" + "  }\n" + "}");
	}

	public void testWithGroup() {
		final List<Snippet> r = execute("WithGroup");

		assertEquals(1, r.size());
		assertEquals("test", r.get(0)
			.getGroupName());
		assertEquals(null, r.get(0)
			.getCodeSnippetProgram());
		assertEquals(null, r.get(0)
			.getParentGroup());
		assertEquals("WithGroup", r.get(0)
			.getCodeSnippetGroup().id);
		assertEquals(null, r.get(0)
			.getCodeSnippetGroup().title);
		assertEquals(null, r.get(0)
			.getCodeSnippetGroup().description);
		assertEquals(0, r.get(0)
			.getCodeSnippetGroup().steps.size());
	}

	public void testWithParentGroup() {
		final List<Snippet> r = execute("WithParentGroup");

		assertEquals(2, r.size());

		assertEquals("test", r.get(0)
			.getGroupName());
		assertEquals(null, r.get(0)
			.getCodeSnippetProgram());
		assertEquals(null, r.get(0)
			.getParentGroup());
		assertEquals("WithParentGroup", r.get(0)
			.getCodeSnippetGroup().id);
		assertEquals(null, r.get(0)
			.getCodeSnippetGroup().title);
		assertEquals(null, r.get(0)
			.getCodeSnippetGroup().description);
		assertEquals(0, r.get(0)
			.getCodeSnippetGroup().steps.size());

		assertEquals(null, r.get(1)
			.getGroupName());
		assertEquals(null, r.get(1)
			.getCodeSnippetGroup());
		assertEquals("test", r.get(1)
			.getParentGroup());
		assertEquals("WithParentGroup1", r.get(1)
			.getCodeSnippetProgram().id);
		assertEquals(null, r.get(1)
			.getCodeSnippetProgram().title);
		assertEquals(null, r.get(1)
			.getCodeSnippetProgram().description);
		assertEquals("java", r.get(1)
			.getCodeSnippetProgram().programmingLanguage);
		assertEquals("public abstract class WithParentGroup {\n" + "\n" + "  abstract public void print();\n" + "}",
			r.get(1)
				.getCodeSnippetProgram().codeSnippet);
	}

	private void checkSnippet(final Snippet s, final String parentGroup, final String id, final String title,
		final String description, final String code) {
		assertEquals(null, s.getGroupName());
		assertEquals(null, s.getCodeSnippetGroup());
		assertEquals(parentGroup, s.getParentGroup());
		assertEquals(id, s.getCodeSnippetProgram().id);
		assertEquals(title, s.getCodeSnippetProgram().title);
		assertEquals(description, s.getCodeSnippetProgram().description);
		assertEquals(code, s.getCodeSnippetProgram().codeSnippet);
		assertEquals("java", s.getCodeSnippetProgram().programmingLanguage);
	}

	private List<Snippet> execute(final String name) {
		try {
			return getReader().read(getFile(name));
		} catch (final FileNotFoundException exception) {
			throw new RuntimeException(exception);
		}
	}

	private JavaSnippetReader getReader() {
		final Map<String, Integer> idCache = new HashMap<>();
		final JavaSnippetReader r = new JavaSnippetReader();
		r.init((i) -> {
			final Integer count = idCache.put(i, Integer.valueOf(idCache.getOrDefault(i, Integer.valueOf(0))
				.intValue() + 1));
			if (count != null) {
				return i + count;
			} else {
				return i;
			}
		});
		return r;
	}

	private File getFile(final String name) {
		return new File("test/biz/aQute/bnd/reporter/codesnippet/", name + ".java");
	}
}
