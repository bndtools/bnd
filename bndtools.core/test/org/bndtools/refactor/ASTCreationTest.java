package org.bndtools.refactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.junit.Test;

public class ASTCreationTest {

	@Test
	public void testCreateAST() throws Exception {
		String source = """
			import com.foobar.*;
			import org.osgi.annotations.Export;
			@Export
			package foo;
			public class HelloWorld {
				public static void main(String[] args) {
					System.out.println(\"Hello, World!\");
				}
			}
			""";

		RefactorAssistant bm = new RefactorAssistant(source);

		assertThat(bm.resolve("Export")).get()
			.isEqualTo("org.osgi.annotations.Export");

		System.out.println(bm.getImports());
		assertThat(bm.getPackageDeclaration()).isNotNull();
		PackageDeclaration pd = bm.getPackageDeclaration()
			.get();
		Stream<Annotation> annotations = bm.getAnnotations(pd);
		Annotation annotation2 = annotations.findAny()
			.get();

		Annotation newAnnotation = bm.newAnnotation("org.example.Foobar", 23);
		bm.add(pd, newAnnotation);

		newAnnotation = bm.newAnnotation("org.example.Foobar", bm.entry("foo", 12));
		bm.add(pd, newAnnotation);

		TextEdit apply = bm.getTextEdit();
		Document d = new Document(source);
		apply.apply(d);
		System.out.println(d.get());
	}

	@Test
	public void testFindClass() throws Exception {
		StringBuilder source = new StringBuilder("""
			package foo;

			class Bar {
				void foo() {}
			}
			""");
		RefactorAssistant bm = new RefactorAssistant(source.toString());

	}

	@Test
	public void testResolve() throws Exception {
		assertThat(getResolve(true, "org.osgi.annotations.*")).isNotPresent();
		assertThat(getResolve(true, "org.osgi.annotations.Export")).get()
			.isEqualTo("org.osgi.annotations.Export");
		assertThat(getResolve(false, "foobar.*", "barfoo.*")).isNotPresent();
		assertThat(getResolve(false, "foobar.Export")).get().isEqualTo("foobar.Export");
		assertThat(getResolve(true, "foobar.*", "org.osgi.annotations.Export")).get()
			.isEqualTo("org.osgi.annotations.Export");
		assertThat(getResolve(false)).isNotPresent();
	}

	Optional<String> getResolve(boolean testAnn, String... imports) throws JavaModelException {

		StringBuilder source = new StringBuilder("""
			@org.osgi.annotations.Version("1.2.3")
			@Export
			package foo;

			""");
		for (String im : imports) {
			source.append("\nimport ")
				.append(im)
				.append(";\n");
		}

		RefactorAssistant bm = new RefactorAssistant(source.toString());
		if (testAnn) {
			PackageDeclaration pd = bm.getPackageDeclaration()
				.get();
			Annotation a = bm.getAnnotation(pd, "Export")
				.get();
			Annotation b = bm.getAnnotation(pd, "org.osgi.annotations.Export")
				.get();
			assertThat(a).isEqualTo(b);
			Annotation c = bm.getAnnotation(pd, "org.osgi.annotations.Version")
				.get();
			assertThat(c).isNotNull();
		}
		return bm.resolve("Export");
	}

}
