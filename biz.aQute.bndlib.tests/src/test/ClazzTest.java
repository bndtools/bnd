package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.Set;

import org.xml.sax.SAXException;

import aQute.bnd.component.AnnotationReader;
import aQute.bnd.component.DSAnnotations;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Jar;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.io.IO;
import junit.framework.TestCase;

@SuppressWarnings("restriction")
public class ClazzTest extends TestCase {

	/**
	 * <pre>
	 *  java.lang.ArrayIndexOutOfBoundsException: 43007 [bnd] at
	 * aQute.bnd.osgi.Clazz.classConstRef(Clazz.java:1880) [bnd] at
	 * aQute.bnd.osgi.Clazz.crawl(Clazz.java:1185)
	 * </pre>
	 * 
	 * This happened on the Jini platform
	 * 
	 * @throws Exception
	 */

	public void testJiniPlatformClasses() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/jsk-platform.jar"));
			b.setExportPackage("*");
			Jar build = b.build();
			assertTrue(b.check());
		}
	}

	/**
	 * Check that exceptions that are caught are added to the imports.
	 */

	// hide the direct reference in the throw
	public static class E extends SAXException {
		private static final long serialVersionUID = 1L;

	}

	public static class Catching {
		public void foo() {
			try {
				throw new E();
			} catch (SAXException sax) {

			}
		}
	}

	public void testCaughtExceptions() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("bin/test/ClazzTest$Catching.class"), new ClassDataCollector() {});
			assertTrue(c.getReferred()
				.toString()
				.contains("org.xml.sax"));
		}
	}

	/**
	 * There is an unused class constant in the This actually looks wrong since
	 */

	public void testUnusedClassConstant() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("testresources/TestWeavingHook.jclass"), new ClassDataCollector() {});
			// TODO test someething here
			System.out.println(c.getReferred());
		}
	}

	/**
	 * {@code java.lang.IllegalArgumentException: Expected IDENTIFIER:
	 * <S:Z>()V;} This actually looks wrong since
	 */

	public void test375() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseDescriptor("<S:[LFoo;>()V", Modifier.PUBLIC);
			c.parseDescriptor("<S:[Z>()V", Modifier.PUBLIC);
			// This is not legal per the JVMS spec
			// c.parseDescriptor("<S:Z>()V", Modifier.PUBLIC);
		}
	}

	public void testNoClassBound() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);

			// From aQute.lib.collections.SortedList.fromIterator()
			c.parseDescriptor(
				"<T::Ljava/lang/Comparable<*>;>(Ljava/util/Iterator<TT;>;)LaQute/lib/collections/SortedList<TT;>;",
				Modifier.PUBLIC);
		}
	}

	/**
	 * Complaint from Groovy that the dynamic instruction fails.
	 * 
	 * <pre>
	 *  [bndwrap]
	 * java.lang.ArrayIndexOutOfBoundsException: 15 [bndwrap] at
	 * aQute.bnd.osgi.Clazz.parseClassFile(Clazz.java:387) [bndwrap] at
	 * aQute.bnd.osgi.Clazz.parseClassFile(Clazz.java:308) [bndwrap] at
	 * aQute.bnd.osgi.Clazz.parseClassFileWithCollector(Clazz.java:297)
	 * [bndwrap] at aQute.bnd.osgi.Clazz.parseClassFile(Clazz.java:286)
	 * [bndwrap] at aQute.bnd.osgi.Analyzer.analyzeJar(Analyzer.java:1489)
	 * [bndwrap] at
	 * aQute.bnd.osgi.Analyzer.analyzeBundleClasspath(Analyzer.java:1387)
	 * [bndwrap] Invalid class file:
	 * groovy/inspect/swingui/AstNodeToScriptVisitor.class [bndwrap] Exception:
	 * 15
	 * </pre>
	 */
	public void testDynamicInstr() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("jar/AstNodeToScriptVisitor.jclass"), new ClassDataCollector() {});
			Set<PackageRef> referred = c.getReferred();
			Descriptors d = new Descriptors();
			assertFalse(referred.contains(d.getPackageRef("")));
			System.out.println(referred);
		}
	}

	public void testModuleInfo() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("jar/module-info.jclass"), new ClassDataCollector() {});
			assertTrue(c.isModule());
			Set<PackageRef> referred = c.getReferred();
			Descriptors d = new Descriptors();
			assertFalse(referred.contains(d.getPackageRef("")));
		}
	}

	/**
	 * Check if the class is not picking up false references when the
	 * CLass.forName name is constructed. The DeploymentAdminPermission.1.jclass
	 * turned out to use Class.forName with a name that was prefixed with a
	 * package from a property. bnd discovered the suffix
	 * (.DeploymentAdminPermission) but this ended up in the default package. So
	 * now the clazz parser tests that the name guessed for Class.forName must
	 * actually resemble a class name.
	 */

	public void testClassForNameFalsePickup() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("jar/DeploymentAdminPermission.1.jclass"),
				new ClassDataCollector() {});
			Set<PackageRef> referred = c.getReferred();
			Descriptors d = new Descriptors();
			assertFalse(referred.contains(d.getPackageRef("")));
			System.out.println(referred);
		}
	}

	/**
	 * Test the uncamel
	 */

	public void testUncamel() throws Exception {
		assertEquals("New", Clazz.unCamel("_new"));
		assertEquals("An XMLMessage", Clazz.unCamel("anXMLMessage"));
		assertEquals("A message", Clazz.unCamel("aMessage"));
		assertEquals("URL", Clazz.unCamel("URL"));
		assertEquals("A nice party", Clazz.unCamel("aNiceParty"));
	}

	public void testAnalyzerCrawlInvokeInterfaceAIOOBException() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("jar/AnalyzerCrawlInvokerInterfaceAIOOBTest.jclass"),
				new ClassDataCollector() {});
			Set<PackageRef> referred = c.getReferred();
			System.out.println(referred);
		}
	}

	public @interface RecursiveAnno {
		@RecursiveAnno("recursive")
		String value() default "";
	}

	public void testRecursiveAnnotation() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$RecursiveAnno.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			AnnotationReader.getDefinition(clazz, analyzer, EnumSet.noneOf(DSAnnotations.Options.class),
				new XMLAttributeFinder(analyzer), AnnotationReader.V1_3);
		}
	}

	@RecursiveAnno
	public @interface MetaAnnotated {}

	public void testIndirectlyAnnotated() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testAnnotated() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testHierarchyAnnotated() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(
				clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testHierarchyIndirectlyAnnotated() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"),
				analyzer));
			assertFalse(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"),
				analyzer));
		}
	}

	@MetaAnnotated
	public static class MetaAnnotated_b {}

	public void testIndirectlyAnnotated_b() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testAnnotated_b() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testHierarchyAnnotated_b() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testHierarchyIndirectlyAnnotated_b() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"),
				analyzer));
			assertFalse(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"),
				analyzer));
		}
	}

	public static class MetaAnnotated_c extends MetaAnnotated_b {}

	public void testIndirectlyAnnotated_c() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_c.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testAnnotated_c() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_c.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testHierarchyAnnotated_c() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_c.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("test.ClazzTest$MetaAnnotated"), analyzer));
			assertFalse(
				clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("!test.ClazzTest$MetaAnnotated"), analyzer));
		}
	}

	public void testHierarchyIndirectlyAnnotated_c() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$MetaAnnotated_c.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"),
				analyzer));
			assertFalse(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"),
				analyzer));
			assertTrue(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$MetaAnnotated"),
				analyzer));
			assertFalse(clazz.is(QUERY.HIERARCHY_INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$MetaAnnotated"),
				analyzer));
		}
	}

	public void testNamed() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.NAMED, new Instruction("test.*"), analyzer));
		}
	}

	public void testMultipleInstructions() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.EXTENDS, new Instruction("junit.framework.TestCase"), analyzer));
			assertTrue(clazz.is(QUERY.NAMED, new Instruction("!junit.framework.*"), analyzer));
		}
	}

	public static interface Foo<T> {}
	public static class Bar {}

	@Target(ElementType.TYPE_USE)
	public static @interface TypeUse {}

	public class AnnotationsOnTypeUseExtends extends @TypeUse Bar {}

	public void testAnnotationsOnTypeUseExtends() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$AnnotationsOnTypeUseExtends.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.getElementType()) {
						case TYPE_USE :
							assertEquals(Annotation.TARGET_INDEX_EXTENDS, annotation.getTargetIndex());
							break;
						default :
							fail("Didn't fine @TypeUse annotation");
					}
				}
			});
		}
	}

	public class AnnotationsOnTypeUseImplements0 implements @TypeUse Foo<String> {}

	public void testAnnotationsOnTypeUseImplements0() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$AnnotationsOnTypeUseImplements0.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.getElementType()) {
						case TYPE_USE :
							assertEquals(0, annotation.getTargetIndex());
							break;
						default :
							fail("Didn't fine @TypeUse annotation");
					}
				}
			});
		}
	}

	@SuppressWarnings("serial")
	public class AnnotationsOnTypeUseImplements1 implements Serializable, @TypeUse Foo<String> {}

	public void testAnnotationsOnTypeUseImplements1() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$AnnotationsOnTypeUseImplements1.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.getElementType()) {
						case TYPE_USE :
							assertEquals(1, annotation.getTargetIndex());
							break;
						default :
							fail("Didn't fine @TypeUse annotation");
					}
				}
			});
		}
	}

	@Target(ElementType.PARAMETER)
	public static @interface TypeParameter {}

	public class AnnotationsOnMethodParams0 {
		void bindChars(@TypeParameter Foo<Character> c) {}
	}

	public void testAnnotationsOnMethodParams0() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$AnnotationsOnMethodParams0.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef member;

				@Override
				public void method(MethodDef member) {
					this.member = member;
				}
				@Override
				public void memberEnd() {
					member = null;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.getElementType()) {
						case PARAMETER :
							assertEquals(0, annotation.getTargetIndex());
							assertEquals("bindChars", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

	public class AnnotationsOnMethodParams1 {
		void bindChars(Foo<Character> c, @TypeParameter String s) {}
	}

	public void testAnnotationsOnMethodParams1() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$AnnotationsOnMethodParams1.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef member;

				@Override
				public void method(MethodDef member) {
					this.member = member;
				}
				@Override
				public void memberEnd() {
					member = null;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.getElementType()) {
						case PARAMETER :
							assertEquals(1, annotation.getTargetIndex());
							assertEquals("bindChars", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

	public class AnnotationsOnCtorParams0 {
		public AnnotationsOnCtorParams0(@TypeParameter String s) {}
	}

	public void testAnnotationsOnCtorParams0() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$AnnotationsOnCtorParams0.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef member;

				@Override
				public void method(MethodDef member) {
					this.member = member;
				}
				@Override
				public void memberEnd() {
					member = null;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.getElementType()) {
						case PARAMETER :
							assertEquals(0, annotation.getTargetIndex());
							assertEquals("<init>", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

	public class AnnotationsOnCtorParams1 {
		public AnnotationsOnCtorParams1(String r, @TypeParameter String s) {}
	}

	public void testAnnotationsOnCtorParams1() throws Exception {
		File file = IO.getFile("bin/test/ClazzTest$AnnotationsOnCtorParams1.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef member;

				@Override
				public void method(MethodDef member) {
					this.member = member;
				}
				@Override
				public void memberEnd() {
					member = null;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.getElementType()) {
						case PARAMETER :
							assertEquals(1, annotation.getTargetIndex());
							assertEquals("<init>", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

}
