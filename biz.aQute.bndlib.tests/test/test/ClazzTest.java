package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.SAXException;

import aQute.bnd.component.DSAnnotationReader;
import aQute.bnd.component.DSAnnotations;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
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
			a.getMethodSignature("<S:[LFoo;>()V");
			a.getMethodSignature("<S:[Z>()V");
			// This is not legal per the JVMS spec
			// a.getMethodSignature("<S:Z>()V");
		}
	}

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

	public void testAnnotated() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

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

	public void testAnnotated_b() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

	public void testHierarchyAnnotated_b() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_b.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.HIERARCHY_ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

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

	public void testAnnotated_c() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$MetaAnnotated_c.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertFalse(clazz.is(QUERY.ANNOTATED, new Instruction("test.ClazzTest$RecursiveAnno"), analyzer));
			assertTrue(clazz.is(QUERY.ANNOTATED, new Instruction("!test.ClazzTest$RecursiveAnno"), analyzer));
		}
	}

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

	public void testNamed() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.NAMED, new Instruction("test.*"), analyzer));
		}
	}

	public void testMultipleInstructions() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertTrue(clazz.is(QUERY.EXTENDS, new Instruction("junit.framework.TestCase"), analyzer));
			assertTrue(clazz.is(QUERY.NAMED, new Instruction("!junit.framework.*"), analyzer));
		}
	}

	public interface Foo<T> {}

	public static class Bar {}

	@Target(ElementType.TYPE_USE)
	public static @interface TypeUse {}

	public static class AnnotationsOnTypeUseExtends extends @TypeUse Bar {}

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

	public static class Nested {}

	public class Inner {}

	public void testNestedClass() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$Nested.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isFalse();
		}
	}

	public void testInnerClass() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$Inner.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
		}
	}

	public static class AnonymousClassHolder {
		Object anon = new Object() {};
	}

	public void testInnerAnonymousClass() throws Exception {
		File file = IO.getFile("bin_test/test/ClazzTest$AnonymousClassHolder$1.class");
		try (Analyzer analyzer = new Analyzer()) {
			Clazz clazz = new Clazz(analyzer, file.getPath(), new FileResource(file));
			clazz.parseClassFile();
			assertThat(clazz.isInnerClass()).isTrue();
		}
	}
}
