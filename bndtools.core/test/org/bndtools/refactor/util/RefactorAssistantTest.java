package org.bndtools.refactor.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bndtools.refactor.util.RefactorAssistant.fromTextBoxEscaped;
import static org.bndtools.refactor.util.RefactorAssistant.toTextBoxEscaped;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import aQute.lib.collections.ExtList;

class RefactorAssistantTest {
	final static String exampleSource = """
		package com.example;
		import org.apache.felix.service.command.annotations.GogoCommand;
		import org.apache.felix.service.command.Parameter;
		import org.apache.felix.service.command.Description;
		@Component
		@GogoCommand( scope ="foo", function = { "command" })
		public class Foo {
		  @Description("command")
		  public String command( @Parameter(absentValue="absent", names="-p") String s, @Parameter(absentValue="false", presentValue="true", names={ "-f", "--flag" } ) boolean flag, int some) {
		     return "";
		  }
		}
		""";

	@Test
	void testTo() {
		assertThat(toTextBoxEscaped("abc", 0)).isEqualTo("\"\"\"\nabc\"\"\"");
		assertThat(toTextBoxEscaped("abc\n", 4)).isEqualTo("\"\"\"\n    abc\n    \"\"\"");
		assertThat(toTextBoxEscaped("  abc\nxx", 4)).isEqualTo("\"\"\"\n      abc\n    xx\"\"\"");
	}

	@Test
	void testFrom() {
		assertThat(fromTextBoxEscaped("\"\"\"\n      abc\n    xx\"\"\"")).isEqualTo("  abc\nxx");
		assertThat(fromTextBoxEscaped("\"\"\"\n    abc\"\"\"")).isEqualTo("abc");
		assertThat(fromTextBoxEscaped("\"\"\"\n    abc\n    \"\"\"")).isEqualTo("abc\n");
	}

	@Test
	void testgetCursorByIndex() throws JavaModelException {
		RefactorAssistant r = new RefactorAssistant(exampleSource);
		Cursor<?> cursor = r.getCursor("class\s+(Foo)");

		assertThat(cursor.getNode()).isPresent()
			.get()
			.isInstanceOf(SimpleName.class);

		cursor = r.getCursor("String com(ma)nd");
		assertThat(cursor.getNode()).isPresent()
			.get()
			.isInstanceOf(SimpleName.class);

	}

	@Test
	void newLiteral() throws JavaModelException {
		RefactorAssistant r = new RefactorAssistant("");

		assertThat(((StringLiteral) r.newLiteral("abc")).getEscapedValue()).isEqualTo("\"abc\"");
		assertThat(((BooleanLiteral) r.newLiteral(true)).booleanValue()).isEqualTo(true);
		assertThat(((CharacterLiteral) r.newLiteral('\n')).charValue()).isEqualTo('\n');
		assertThat(((NumberLiteral) r.newLiteral(1)).getToken()).isEqualTo("1");
		assertThat(((NumberLiteral) r.newLiteral(1L)).getToken()).isEqualTo("1");
		assertThat(((NumberLiteral) r.newLiteral(1F)).getToken()).isEqualTo("1.0");
		assertThat(((NumberLiteral) r.newLiteral(1D)).getToken()).isEqualTo("1.0");
		assertThat(((NumberLiteral) r.newLiteral(1_000_000_000_000_000_000L)).getToken())
			.isEqualTo("1000000000000000000L");
		assertThat(((NumberLiteral) r.newLiteral(1E50D)).getToken()).isEqualTo("1.0E50D");

		assertThat(((ArrayCreation) r.newLiteral(new String[] {
			"abc"
		})).toString()).isEqualTo("new String[]{\"abc\"}");
		assertThat(((ArrayCreation) r.newLiteral(new ExtList<>("abc", "def"))).toString())
			.isEqualTo("new String[]{\"abc\",\"def\"}");
		assertThat(((ArrayCreation) r.newLiteral(new ExtList<>("abc", 1))).toString())
			.isEqualTo("new Object[]{\"abc\",1}");

		assertThat((r.newTypeLiteral(String.class)).toString()).isEqualTo("String.class");
	}

	@Test
	void commonType() throws JavaModelException {
		class A {}
		class BA extends A {}
		class CA extends A {}
		class DBA extends BA {}
		class ECA extends CA {}

		assertThat(RefactorAssistant.commonType(null, null)).isEqualTo(Object.class);
		assertThat(RefactorAssistant.commonType(A.class, A.class)).isEqualTo(A.class);
		assertThat(RefactorAssistant.commonType(String.class, A.class)).isEqualTo(Object.class);
		assertThat(RefactorAssistant.commonType(ECA.class, DBA.class)).isEqualTo(A.class);
		assertThat(RefactorAssistant.commonType(CA.class, DBA.class)).isEqualTo(A.class);
		assertThat(RefactorAssistant.commonType(DBA.class, CA.class)).isEqualTo(A.class);
		assertThat(RefactorAssistant.commonType(DBA.class, String.class)).isEqualTo(Object.class);

		assertThat(RefactorAssistant.commonType(int.class, Number.class)).isEqualTo(Object.class);
		assertThat(RefactorAssistant.commonType(Integer.class, Float.class)).isEqualTo(Number.class);
		assertThat(RefactorAssistant.commonType(Integer.class, Character.class)).isEqualTo(Object.class);

	}

	@Test
	void info() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				int bar = ((Foo)foo.bar.bla.Foo).foo();
				List<String> list = (java.lang.Long) null;
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		System.out.println(r.getReferredTypes(r.getCompilationUnit()));

	}

	@Test
	void testUpTo() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				int bar = ((Foo)foo.bar.bla.Foo).foo();
				List<String> list = (java.lang.Long) null;
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		Cursor<?> selection = r.getCursor("class F");
		Cursor<TypeDeclaration> typeDeclaration = selection.cast(TypeDeclaration.class);
		assertThat(typeDeclaration.getNode()).isPresent();
		Cursor<TypeDeclaration> upTo = r.getCursor("null")
			.upTo(TypeDeclaration.class);
		assertThat(typeDeclaration).isEqualTo(upTo);

		Cursor<TypeDeclaration> notUpTo = r.getCursor("null")
			.upTo(TypeDeclaration.class, 1);
		assertThat(notUpTo.isEmpty()).isTrue();

		assertThat(selection.upTo(Block.class)
			.isEmpty()).isTrue();
		assertThat(selection.upTo(CompilationUnit.class)
			.isEmpty()).isFalse();

	}

	@Test
	void testNoneOfTheseAnnotations() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				int bar = ((Foo)foo.bar.bla.Foo).foo();
				List<String> list = (java.lang.Long) null;
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		Cursor<TypeDeclaration> selection = r.getCursor("class F")
			.cast(TypeDeclaration.class);
		assertThat(selection.noneOfTheseAnnotations("foobar")
			.getNodes()).hasSize(1);
		assertThat(selection.noneOfTheseAnnotations("com.example.Description")
			.getNodes()).hasSize(0);

	}

	@Test
	void testIsVoidMethod() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				void voidmethod() {}
				int intmethod() { return 1;}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		Cursor<MethodDeclaration> selection = r.getCursor("class F")
			.downTo(MethodDeclaration.class);
		assertThat(selection.getNodes()).hasSize(2);
		assertThat(selection.isVoidMethod()
			.hasName("voidmethod")
			.getNodes()).hasSize(1);
	}

	@Test
	void testMap() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				void voidmethod() {}
				int intmethod() { return 1;}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		Cursor<SimpleName> selection = r.getCursor("class F")
			.downTo(MethodDeclaration.class)
			.map(MethodDeclaration::getName);

		assertThat(selection.getNodes()
			.stream()
			.map(n -> n.toString())).containsExactly("voidmethod", "intmethod");
	}

	@Test
	void testFlatMap() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				void voidmethod(int a) {}
				int intmethod(String s, double d) { return 1;}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		Cursor<SingleVariableDeclaration> selection = r.getCursor("class F")
			.downTo(MethodDeclaration.class)
			.flatMap(md -> r.cursor(md)
				.downTo(SingleVariableDeclaration.class));

		assertThat(selection.getNodes()
			.stream()
			.map(n -> n.getName()
				.toString())).containsExactly("a", "s", "d");
	}

	@Test
	void testIsNotInstanceofAny() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				void voidmethod(int a) {}
				int intmethod(String s, double d) { return 1;}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		Cursor<TypeDeclaration> selection = r.getCursor("class F")
			.cast(TypeDeclaration.class)
			.isNotInstanceOfAny(MethodDeclaration.class, SingleVariableDeclaration.class);

		assertThat(selection.getNodes()).hasSize(1);

		selection = selection.isNotInstanceOfAny(TypeDeclaration.class);
		assertThat(selection.getNodes()).hasSize(0);
	}

	@Test
	void testCheckAnnotation() throws JavaModelException {
		String s = """
			import com.example.Description;
			@Description class F {
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		r.getCursor("class F")
			.cast(TypeDeclaration.class)
			.checkAnnotation((crs, present) -> {
				assertThat(present).isTrue();
			}, "com.example.Description");

		r.getCursor("class F")
			.cast(TypeDeclaration.class)
			.checkAnnotation((crs, present) -> {
				assertThat(present).isFalse();
			}, "foobar");
	}

	@Test
	void testPrimitive() throws JavaModelException {
		String s = """
			import com.example.Description;
			class F {
				void voidmethod(int par) {}
				int anInt=3;
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("par")
			.upTo(SingleVariableDeclaration.class)
			.isPrimitive()
			.getNodes()).hasSize(1);
		assertThat(r.getCursor("par")
			.upTo(SingleVariableDeclaration.class)
			.isNotPrimitive()
			.getNodes()).hasSize(0);

		assertThat(r.getCursor("voidmethod")
			.upTo(MethodDeclaration.class)
			.isPrimitive()
			.getNodes()).hasSize(1);
		assertThat(r.getCursor("voidmethod")
			.upTo(MethodDeclaration.class)
			.isNotPrimitive()
			.getNodes()).hasSize(0);

		assertThat(r.getCursor("anInt")
			.upTo(FieldDeclaration.class)
			.isPrimitive()
			.getNodes()).hasSize(1);
		assertThat(r.getCursor("anInt")
			.upTo(FieldDeclaration.class)
			.isNotPrimitive()
			.getNodes()).hasSize(0);

	}

	@Test
	void testNames() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			@Description class F {
				void abc() {}
				int bcd() { return 1;}
				int cde() { return 1;}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);
		Cursor<MethodDeclaration> methods = r.getCursor("class F")
			.upTo(TypeDeclaration.class)
			.downTo(MethodDeclaration.class);
		assertThat(methods.getNodes()).hasSize(3);

		assertThat(methods.nameMatches(".*a.*")
			.getNodes()).hasSize(1);
		assertThat(methods.nameMatches(".*b.*")
			.getNodes()).hasSize(2);

		assertThat(methods.nameMatches(".*c.*")
			.getNodes()).hasSize(3);
		assertThat(methods.nameMatches(".*d.*")
			.getNodes()).hasSize(2);
		assertThat(methods.nameMatches(".*e.*")
			.getNodes()).hasSize(1);

		assertThat(methods.hasName("abc")
			.getNodes()).hasSize(1);

	}

	@ParameterizedTest
	@MethodSource("sourceTypes")
	void testJavaSourceType(Scenario scenario) throws JavaModelException {

		RefactorAssistant r = new RefactorAssistant(scenario.source, 18, scenario.type, null);

		Cursor<?> field = r.getCursor("foo");

		ASTNode fieldDeclaration = field.getNode()
			.get();
		assertThat(r.getJavaSourceType(fieldDeclaration)).isEqualTo(scenario.type);

		assertThat(field.isJavaSourceType(scenario.type)
			.getNodes()).hasSize(1);
		Set<JavaSourceType> all = EnumSet.allOf(JavaSourceType.class);
		all.remove(JavaSourceType.UNKNOWN);
		all.remove(scenario.type);
		for (JavaSourceType jst : all) {
			assertThat(field.isJavaSourceType(jst)
				.getNodes()).describedAs(jst.toString())
					.hasSize(0);
		}
	}

	record Scenario(String source, JavaSourceType type) {}

	public static List<Scenario> sourceTypes() {
		return new ExtList<>(new Scenario("class F { int foo; }", JavaSourceType.CLASS),
			new Scenario("interface F { int foo(); }", JavaSourceType.INTERFACE),
			new Scenario("@interface Ann { int foo() default 1; }", JavaSourceType.ANNOTATION),
			new Scenario("enum AnEnum ( foo;)", JavaSourceType.ENUM),
			new Scenario("record ARecord(int bar,String s) { int foo;}", JavaSourceType.RECORD),
			new Scenario("package foo.bar;\nimport com.foo.Help;\n", JavaSourceType.PACKAGEINFO), //
			new Scenario("""
				module com.example.myModule {
				    requires java.base;
				    requires java.sql;

				    exports com.foo.myModule;
				}
								""", JavaSourceType.MODULEINFO));
	}

	@Test
	void testAnd() throws JavaModelException {
		String s = """
			import com.example.Foo;
			import com.example.Description;
			public @Description class F {
				void abc(int foo) {}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("foo")
			.cast(SimpleName.class)
			.and(c -> c.upTo(TypeDeclaration.class)
				.anyOfTheseAnnotations("com.example.Description"))
			.getNodes()).hasSize(1);

		assertThat(r.getCursor("foo")
			.cast(SimpleName.class)
			.and(c -> c.upTo(TypeDeclaration.class)
				.noneOfTheseAnnotations("com.example.Description"))
			.getNodes()).hasSize(0);
	}

	@Test
	void testProcess() throws JavaModelException {
		String s = """
			import com.example.Description;
			public class F {
				@Description void abc(int foo) {}
				@Description void def(int foo) {}
				void ghi(int foo) {}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("foo")
			.upTo(TypeDeclaration.class)
			.downTo(MethodDeclaration.class)
			.anyOfTheseAnnotations("com.example.Description")
			.processSingletons(c -> c.getNode()
				.map(n -> n.getName()
					.toString())
				.orElse("??"))).containsExactly("abc", "def");

	}

	@Test
	void testDownto() throws JavaModelException {
		String s = """
			import com.example.Description;
			public class F {
				@Description void abc(int foo) {}
				@Description void def(int foo) {}
				void ghi(int foo) {}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("foo")
			.upTo(CompilationUnit.class)
			.downTo(MethodDeclaration.class)
			.processSingletons(c -> c.getNode()
				.map(n -> n.getName()
					.toString())
				.orElse("??"))).containsExactly("abc", "def", "ghi");

	}

	@Test
	void testDownto2() throws JavaModelException {
		String s = """
			public class F {
				void abc(int foo) {}
				class G {
				  void ghi(int foo) {}
				}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("foo")
			.upTo(CompilationUnit.class)
			.downTo(MethodDeclaration.class)
			.processSingletons(c -> c.getNode()
				.map(n -> n.getName()
					.toString())
				.orElse("??"))).containsExactly("abc", "ghi");

	}

	@Test
	void testDescend() throws JavaModelException {
		String s = """
			public class F {
				void abc(int foo) {}
				class G {
				  void ghi(int foo) {}
				}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("foo")
			.upTo(CompilationUnit.class)
			.downTo(TypeDeclaration.class)
			.processSingletons(c -> c.getNode()
				.map(n -> n.getName()
					.toString())
				.orElse("??"))).containsExactly("F");

	}

	@Test
	void testParentType() throws JavaModelException {
		String s = """
			public class F {
				void abc(int foo) {}
				class G {
				  void ghi(int foo) {}
				}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("abc")
			.upTo(MethodDeclaration.class)
			.parentType(TypeDeclaration.class)
			.getNode()).isPresent();
		assertThat(r.getCursor("abc")
			.upTo(MethodDeclaration.class)
			.parentType(ModuleDeclaration.class)
			.getNode()).isNotPresent();

	}

	@Test
	void testTypeIn() throws JavaModelException {
		String s = """
			import com.example.Foo;
			public class F {
				void abc(int foo) {}
				class G {
				  int ghi(String bar) {}
				}
				  Foo jkl(String bar) {}
			}
			""";

		RefactorAssistant r = new RefactorAssistant(s);

		assertThat(r.getCursor("bar")
			.upTo(SingleVariableDeclaration.class)
			.typeIn("java.lang.String")
			.getNode()).isPresent();

		assertThat(r.getCursor("abc")
			.upTo(MethodDeclaration.class)
			.typeIn("void")
			.getNode()).isPresent();

		assertThat(r.getCursor("jkl")
			.upTo(MethodDeclaration.class)
			.typeIn("com.example.Foo")
			.getNode()).isPresent();
	}

}
