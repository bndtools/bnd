package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.xml.sax.SAXException;

import aQute.bnd.component.DSAnnotationReader;
import aQute.bnd.component.DSAnnotations;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.JAVA;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Resource;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.io.IO;
import aQute.lib.manifest.ManifestUtil;

public class ClazzTest {

	/**
	 * <pre>
	 *  java.lang.ArrayIndexOutOfBoundsException: 43007 [bnd] at
	 * aQute.bnd.osgi.Clazz.classConstRef(Clazz.java:1880) [bnd] at
	 * aQute.bnd.osgi.Clazz.crawl(Clazz.java:1185)
	 * </pre>
	 *
	 * This happened on the Jini platform. This test stops working with Java
	 * >=21 (https://bugs.openjdk.org/browse/JDK-8313765)
	 *
	 * @throws Exception
	 */

	@Test
	@EnabledForJreRange(max = JRE.JAVA_20)
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

	@Test
	public void testCaughtExceptions() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("bin_test/test/ClazzTest$Catching.class"),
				new ClassDataCollector() {});
			assertTrue(c.getReferred()
				.toString()
				.contains("org.xml.sax"));
		}
	}

	/**
	 * There is an unused class constant in the This actually looks wrong since
	 */

	@Test
	public void testUnusedClassConstant() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			c.parseClassFile(new FileInputStream("testresources/TestWeavingHook.jclass"), new ClassDataCollector() {});
			// TODO test something here
			System.out.println(c.getReferred());
		}
	}

	/**
	 * {@code java.lang.IllegalArgumentException: Expected IDENTIFIER:
	 * <S:Z>()V;} This actually looks wrong since
	 */

	@Test
	public void test375() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.getMethodSignature("<S:[LFoo;>()V");
			a.getMethodSignature("<S:[Z>()V");
			// This is not legal per the JVMS spec
			// a.getMethodSignature("<S:Z>()V");
		}
	}

	@Test
	public void testNoClassBound() throws Exception {
		try (Analyzer a = new Analyzer()) {
			// From aQute.lib.collections.SortedList.fromIterator()
			a.getMethodSignature(
				"<T::Ljava/lang/Comparable<*>;>(Ljava/util/Iterator<TT;>;)LaQute/lib/collections/SortedList<TT;>;");
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
	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testUncamel() throws Exception {
		assertEquals("New", Clazz.unCamel("_new"));
		assertEquals("An XMLMessage", Clazz.unCamel("anXMLMessage"));
		assertEquals("A message", Clazz.unCamel("aMessage"));
		assertEquals("URL", Clazz.unCamel("URL"));
		assertEquals("A nice party", Clazz.unCamel("aNiceParty"));
	}

	@Test
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

	@Test
	public void testRecursiveAnnotation() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$RecursiveAnno.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			DSAnnotationReader.getDefinition(clazz, analyzer, EnumSet.noneOf(DSAnnotations.Options.class),
				new XMLAttributeFinder(analyzer), DSAnnotationReader.V1_3);
		}
	}

	@RecursiveAnno
	public @interface MetaAnnotated {}

	@Test
	public void testIndirectlyAnnotated() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testAnnotated() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testHierarchyAnnotated() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(
				clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testHierarchyIndirectlyAnnotated() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated.class");
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

	@Test
	public void testIndirectlyAnnotated_b() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testAnnotated_b() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testHierarchyAnnotated_b() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testHierarchyIndirectlyAnnotated_b() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_b.class");
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

	@Test
	public void testIndirectlyAnnotated_c() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_c.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(
				clazz.is(QUERY.INDIRECTLY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testAnnotated_c() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_c.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	@Test
	public void testHierarchyAnnotated_c() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_c.class");
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

	@Test
	public void testHierarchyIndirectlyAnnotated_c() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_c.class");
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

	@Test
	public void testNamed() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.NAMED, new Instruction("test.*"), analyzer));
		}
	}

	@Test
	public void testMultipleInstructions() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.EXTENDS, new Instruction("java.lang.Object"), analyzer));
			assertTrue(clazz.is(QUERY.NAMED, new Instruction("!junit.framework.*"), analyzer));
		}
	}

	public interface Foo<T> {}

	public static class Bar {}

	@Target(ElementType.TYPE_USE)
	public static @interface TypeUse {}

	public static class AnnotationsOnTypeUseExtends extends @TypeUse Bar {}

	@Test
	public void testAnnotationsOnTypeUseExtends() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnnotationsOnTypeUseExtends.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				int	target_index;
				int	target_type;

				@Override
				public void typeuse(int target_type, int target_index, byte[] target_info, byte[] type_path) {
					this.target_type = target_type;
					this.target_index = target_index;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.elementType()) {
						case TYPE_USE :
							assertEquals(0x10, target_type);
							assertEquals(Clazz.TYPEUSE_TARGET_INDEX_EXTENDS, target_index);
							break;
						default :
							fail("Didn't fine @TypeUse annotation");
					}
				}
			});
		}
	}

	public static class AnnotationsOnTypeUseImplements0 implements @TypeUse Foo<String> {}

	@Test
	public void testAnnotationsOnTypeUseImplements0() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnnotationsOnTypeUseImplements0.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				int	target_index;
				int	target_type;

				@Override
				public void typeuse(int target_type, int target_index, byte[] target_info, byte[] type_path) {
					this.target_type = target_type;
					this.target_index = target_index;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.elementType()) {
						case TYPE_USE :
							assertEquals(0x10, target_type);
							assertEquals(0, target_index);
							break;
						default :
							fail("Didn't fine @TypeUse annotation");
					}
				}
			});
		}
	}

	@SuppressWarnings("serial")
	public static class AnnotationsOnTypeUseImplements1 implements Serializable, @TypeUse Foo<String> {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	}

	@Test
	public void testAnnotationsOnTypeUseImplements1() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnnotationsOnTypeUseImplements1.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				int	target_index;
				int	target_type;

				@Override
				public void typeuse(int target_type, int target_index, byte[] target_info, byte[] type_path) {
					this.target_type = target_type;
					this.target_index = target_index;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.elementType()) {
						case TYPE_USE :
							assertEquals(0x10, target_type);
							assertEquals(1, target_index);
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

	public static class AnnotationsOnMethodParams0 {
		void bindChars(@TypeParameter Foo<Character> c) {}
	}

	@Test
	public void testAnnotationsOnMethodParams0() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnnotationsOnMethodParams0.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef	member;
				int			parameter;

				@Override
				public void parameter(int p) {
					parameter = p;
				}

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
					switch (annotation.elementType()) {
						case PARAMETER :
							assertEquals(0, parameter);
							assertEquals("bindChars", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

	public static class AnnotationsOnMethodParams1 {
		void bindChars(Foo<Character> c, @TypeParameter String s) {}
	}

	@Test
	public void testAnnotationsOnMethodParams1() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnnotationsOnMethodParams1.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef	member;
				int			parameter;

				@Override
				public void parameter(int p) {
					parameter = p;
				}

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
					switch (annotation.elementType()) {
						case PARAMETER :
							assertEquals(1, parameter);
							assertEquals("bindChars", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

	public static class AnnotationsOnCtorParams0 {
		public AnnotationsOnCtorParams0(@TypeParameter String s) {}
	}

	@Test
	public void testAnnotationsOnCtorParams0() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnnotationsOnCtorParams0.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef	member;
				int			parameter;

				@Override
				public void parameter(int p) {
					parameter = p;
				}

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
					switch (annotation.elementType()) {
						case PARAMETER :
							assertEquals(0, parameter);
							assertEquals("<init>", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

	public static class AnnotationsOnCtorParams1 {
		public AnnotationsOnCtorParams1(String r, @TypeParameter String s) {}
	}

	@Test
	public void testAnnotationsOnCtorParams1() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnnotationsOnCtorParams1.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				MethodDef	member;
				int			parameter;

				@Override
				public void parameter(int p) {
					parameter = p;
				}

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
					switch (annotation.elementType()) {
						case PARAMETER :
							assertEquals(1, parameter);
							assertEquals("<init>", member.getName());
							break;
						default :
							fail("Didn't fine @TypeParameter annotation");
					}
				}
			});
		}
	}

	/**
	 * See 4.7.20.2 in JVMS spec.
	 */
	@Test
	public void testTypeUseTypePath() throws Exception {
		File file = IO.getFile("bin_test/test/typeuse/TypePath.class");
		try (Analyzer analyzer = new Analyzer()) {
			List<String> tested = new ArrayList<>();
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				FieldDef	member;
				byte[]		type_path;

				@Override
				public void typeuse(int target_type, int target_index, byte[] target_info, byte[] type_path) {
					this.type_path = type_path;
				}

				@Override
				public void field(FieldDef member) {
					this.member = member;
				}

				@Override
				public void memberEnd() {
					member = null;
				}

				@Override
				public void annotation(Annotation annotation) throws Exception {
					switch (annotation.elementType()) {
						case TYPE_USE :
							switch (member.getName()) {
								case "b" :
									switch (annotation.getName()
										.getBinary()) {
										case "test/typeuse/A" :
											assertThat(type_path).hasSize(0);
											break;
										case "test/typeuse/B" :
											assertThat(type_path).hasSize(2)
												.containsExactly(3, 0);
											break;
										case "test/typeuse/C" :
											assertThat(type_path).hasSize(4)
												.containsExactly(3, 0, 2, 0);
											break;
										case "test/typeuse/D" :
											assertThat(type_path).hasSize(2)
												.containsExactly(3, 1);
											break;
										case "test/typeuse/E" :
											assertThat(type_path).hasSize(4)
												.containsExactly(3, 1, 3, 0);
											break;
										default :
											fail("Unexpected annotation " + annotation);
									}
									break;
								case "c" :
									switch (annotation.getName()
										.getBinary()) {
										case "test/typeuse/F" :
											assertThat(type_path).hasSize(0);
											break;
										case "test/typeuse/G" :
											assertThat(type_path).hasSize(2)
												.containsExactly(0, 0);
											break;
										case "test/typeuse/H" :
											assertThat(type_path).hasSize(4)
												.containsExactly(0, 0, 0, 0);
											break;
										case "test/typeuse/I" :
											assertThat(type_path).hasSize(6)
												.containsExactly(0, 0, 0, 0, 0, 0);
											break;
										default :
											fail("Unexpected annotation " + annotation);
									}
									break;
								case "d" :
									switch (annotation.getName()
										.getBinary()) {
										case "test/typeuse/A" :
											assertThat(type_path).hasSize(0);
											break;
										case "test/typeuse/B" :
											assertThat(type_path).hasSize(2)
												.containsExactly(3, 0);
											break;
										case "test/typeuse/C" :
											assertThat(type_path).hasSize(4)
												.containsExactly(3, 0, 3, 0);
											break;
										case "test/typeuse/D" :
											assertThat(type_path).hasSize(6)
												.containsExactly(3, 0, 3, 0, 0, 0);
											break;
										case "test/typeuse/E" :
											assertThat(type_path).hasSize(8)
												.containsExactly(3, 0, 3, 0, 0, 0, 0, 0);
											break;
										case "test/typeuse/F" :
											assertThat(type_path).hasSize(10)
												.containsExactly(3, 0, 3, 0, 0, 0, 0, 0, 0, 0);
											break;
										default :
											fail("Unexpected annotation " + annotation);
									}
									break;
								case "e" :
									switch (annotation.getName()
										.getBinary()) {
										case "test/typeuse/A" :
											assertThat(type_path).hasSize(4)
												.containsExactly(1, 0, 1, 0);
											break;
										case "test/typeuse/B" :
											assertThat(type_path).hasSize(2)
												.containsExactly(1, 0);
											break;
										case "test/typeuse/C" :
											assertThat(type_path).hasSize(0);
											break;
										default :
											fail("Unexpected annotation " + annotation);
									}
									break;
								case "f" :
									switch (annotation.getName()
										.getBinary()) {
										case "test/typeuse/A" :
											assertThat(type_path).hasSize(6)
												.containsExactly(1, 0, 1, 0, 3, 0);
											break;
										case "test/typeuse/B" :
											assertThat(type_path).hasSize(8)
												.containsExactly(1, 0, 1, 0, 3, 0, 0, 0);
											break;
										case "test/typeuse/C" :
											assertThat(type_path).hasSize(6)
												.containsExactly(1, 0, 3, 0, 1, 0);
											break;
										case "test/typeuse/D" :
											assertThat(type_path).hasSize(4)
												.containsExactly(1, 0, 3, 0);
											break;
										default :
											fail("Unexpected annotation " + annotation);
									}
									break;
								default :
									break;
							}
							tested.add(member.getName() + "|" + annotation.getName()
								.getShortName());
							break;
						default :
							fail("Didn't find TYPE_USE annotation");
					}
				}
			});
			assertThat(tested).containsExactlyInAnyOrder("b|A", "b|B", "b|C", "b|D", "b|E", "c|F", "c|G", "c|H", "c|I",
				"d|A", "d|B", "d|C", "d|D", "d|E", "d|F", "e|A", "e|B", "e|C", "f|A", "f|B", "f|C", "f|D");
		}
	}


	@Test
	public void testTopLevelClass() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isFalse();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isTrue();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isFalse();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEqualTo("test.ClazzTest");
			assertThat(replacer.process("${classes;INNER}")).isEmpty();
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	public static class Nested {}

	@Test
	public void testNestedClass() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$Nested.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isFalse();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isTrue();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isFalse();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEqualTo("test.ClazzTest$Nested");
			assertThat(replacer.process("${classes;INNER}")).isEmpty();
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	public class Inner {}

	@Test
	public void testInnerClass() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$Inner.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isFalse();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isTrue();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEmpty();
			assertThat(replacer.process("${classes;INNER}")).isEqualTo("test.ClazzTest$Inner");
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void testAnonymousClass() throws Exception {
		Object anon = new Object() {};
		String name = anon.getClass()
			.getName();
		File file = IO.getFile("bin_test/" + name.replace('.', '/') + ".class");
		assertThat(file).exists();
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isFalse();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isTrue();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEmpty();
			assertThat(replacer.process("${classes;INNER}")).isEqualTo(name);
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void testLocalClass() throws Exception {
		class Local {}
		Local local = new Local();
		String name = local.getClass()
			.getName();
		File file = IO.getFile("bin_test/" + name.replace('.', '/') + ".class");
		assertThat(file).exists();
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isFalse();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isTrue();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEmpty();
			assertThat(replacer.process("${classes;INNER}")).isEqualTo(name);
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void testVersion() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			AtomicReference<String> version = new AtomicReference<>();
			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				@Override
				public void version(int minor, int major) {
					version.set(major + "." + minor);
				}
			});
			assertThat(clazz.is(QUERY.VERSION, new Instruction(version.get()), analyzer)).isTrue();
			assertThat(clazz.is(QUERY.VERSION, new Instruction("46.0"), analyzer)).isFalse();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;VERSION;" + version.get() + "}"))
				.isEqualTo("test.ClazzTest");
			assertThat(replacer.process("${classes;VERSION;46.0}")).isEmpty();
		}
	}

	@Test
	public void kotlin_TopLevelClass() throws Exception {
		File file = IO.getFile("testresources/kotlin/innerclasses/Example.kclass");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isFalse();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isTrue();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isFalse();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEqualTo("com.example.Example");
			assertThat(replacer.process("${classes;INNER}")).isEmpty();
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void kotlin_NestedClass() throws Exception {
		File file = IO.getFile("testresources/kotlin/innerclasses/Example$Nested.kclass");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isFalse();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isTrue();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isFalse();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEqualTo("com.example.Example$Nested");
			assertThat(replacer.process("${classes;INNER}")).isEmpty();
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void kotlinInnerClass() throws Exception {
		File file = IO.getFile("testresources/kotlin/innerclasses/Example$Inner.kclass");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isFalse();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isTrue();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEmpty();
			assertThat(replacer.process("${classes;INNER}")).isEqualTo("com.example.Example$Inner");
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void kotlin_AnonymousClass() throws Exception {
		File file = IO.getFile("testresources/kotlin/innerclasses/Example$anonClass$anon$1.kclass");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isFalse();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isTrue();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEmpty();
			assertThat(replacer.process("${classes;INNER}")).isEqualTo("com.example.Example$anonClass$anon$1");
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void kotlin_LocalClass() throws Exception {
		File file = IO.getFile("testresources/kotlin/innerclasses/Example$localClass$Local.kclass");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isFalse();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isTrue();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEmpty();
			assertThat(replacer.process("${classes;INNER}")).isEqualTo("com.example.Example$localClass$Local");
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}

	@Test
	public void kotlin_LambdaClass() throws Exception {
		File file = IO.getFile("testresources/kotlin/innerclasses/Example$lambdaClass$lambda$1.kclass");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
			assertThat(clazz.is(QUERY.STATIC, null, analyzer)).isFalse();
			assertThat(clazz.is(QUERY.INNER, null, analyzer)).isTrue();
			analyzer.getClassspace()
				.put(clazz.getClassName(), clazz);
			Macro replacer = analyzer.getReplacer();
			assertThat(replacer.process("${classes;STATIC}")).isEmpty();
			assertThat(replacer.process("${classes;INNER}")).isEqualTo("com.example.Example$lambdaClass$lambda$1");
			assertThat(replacer.process("${classes;STATIC;INNER}")).isEmpty();
		}
	}


	/**
	 * This test creates a fake java .class file with an imaginary large major
	 * version (10000) to test how our Analyzer handles class files of a yet
	 * unknown future JDK.
	 */
	@Test
	void testUnknownJDKClass() throws Exception {


		File file = IO.getFile("bin_test/test/ClazzTest$Inner.class");
		byte[] fakeClassBytes = Files.readAllBytes(file.toPath());

		int newMajor = 10_000;
		int expected = 9955; // newMajor - 45;
		// Patch major version (u2 big-endian)
		// https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html
		fakeClassBytes[6] = (byte) ((newMajor >> 8) & 0xFF);
		fakeClassBytes[7] = (byte) (newMajor & 0xFF);

		try (Jar jar = new Jar("future");
			Analyzer a = new Analyzer(jar)) {

			Resource r = new EmbeddedResource(fakeClassBytes, fakeClassBytes.length);

			Clazz c = new Clazz(a, "", r);
			c.parseClassFile(new ByteArrayInputStream(fakeClassBytes), new ClassDataCollector() {});
			a.getClassspace()
			.put(c.getClassName(), c);

			assertThat(c.getMajorVersion()).isEqualTo(newMajor);

			Manifest calcManifest = a.calcManifest();
			ManifestUtil.write(calcManifest, System.out);
			Attributes mainAttributes = calcManifest.getMainAttributes();
			assertThat(mainAttributes.getValue(Constants.REQUIRE_CAPABILITY))
				.contains("(&(osgi.ee=JavaSE)(version=" + expected + "))");
			assertThat(a.getEEs()).containsExactly(JAVA.UNKNOWN);


		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	@Test
	void testBuildEEFilter_lenientKnown() {
		// lenient = true but version within known range
		String result = Clazz.JAVA.buildEEFilterLenient(55); // within range
		assertEquals("(&(osgi.ee=JavaSE)(version=11))", result);
	}


	@Test
	void testBuildEEFilter_lenientTooLow() {
		// version < 0 -> UNKNOWN
		String result = Clazz.JAVA.buildEEFilterLenient(40);
		assertEquals("(&(osgi.ee=JavaSE)(version=UNKNOWN))", result);
	}

	@Test
	void testBuildEEFilter_lenientTooHigh() {
		// version >= JAVA.values.length - 1 -> eeFilter(version)
		int maxmajor = Clazz.JAVA.values()[Clazz.JAVA.values().length - 2].getMajor();
		int tooHigh = maxmajor + 10;
		int expected = tooHigh - 45;
		String result = Clazz.JAVA.buildEEFilterLenient(tooHigh);
		// version = max. known version + 1
		assertEquals("(&(osgi.ee=JavaSE)(version=" + expected + "))", result);
	}
}
