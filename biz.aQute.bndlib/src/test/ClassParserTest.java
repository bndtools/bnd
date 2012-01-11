package test;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.service.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.libg.reporter.*;

class ConstantValues {
	public static final boolean	f		= false;
	public static final boolean	t		= true;
	public static final byte	bt		= Byte.MAX_VALUE;
	public static final short	shrt	= Short.MAX_VALUE;
	public static final char	chr		= Character.MAX_VALUE;
	public static final int		intgr	= Integer.MAX_VALUE;
	public static final long	lng		= Long.MAX_VALUE;
	public static final float	flt		= Float.MAX_VALUE;
	public static final double	dbl		= Double.MAX_VALUE;
	public static final String	strng	= "blabla";
	// Classes somehow are not treated as constants
//	public static final Class			clss	= Object.class;
}

interface WithGenerics<VERYLONGTYPE, X extends Jar> {
	List<? super VERYLONGTYPE> baz2();

	List<? extends Jar>		field	= null;

	WithGenerics<URL, Jar>	x		= null;
}

class Generics {
	Map<ClassParserTest, ?> baz() {
		return null;
	}

	Map<ClassParserTest, ?> baz1() {
		return null;
	}

	Map<? extends String, ?> baz2() {
		return null;
	}

	List<ClassParserTest> foo() {
		return null;
	}

	Map<ClassParserTest, Clazz> bar() {
		return null;
	}

	WithGenerics<List<Jar>, Jar> xyz() {
		return null;
	}
}

class Implemented implements Plugin {
	public void setProperties(Map<String, String> map) {
	}

	public void setReporter(Reporter processor) {
	}
}

public class ClassParserTest extends TestCase {
	Analyzer a = new Analyzer();

	/**
	 * Test the constant values
	 * 
	 * @throws Exception
	 */


	public void testConstantValues() throws Exception {
		final Map<String, Object> values = new HashMap<String, Object>();
		Clazz c = new Clazz(a,"ConstantValues", new FileResource(IO.getFile(
				new File("").getAbsoluteFile(), "bin/test/ConstantValues.class")));
		c.parseClassFileWithCollector(new ClassDataCollector() {
			Clazz.FieldDef	last;

			@Override public void field(Clazz.FieldDef referenced) {
				last = referenced;
			}

			@Override public void constant(Object value) {
				values.put(last.getName(), value);
			}

		});

		assertEquals(1, values.get("t"));
		assertEquals(0, values.get("f"));
		assertEquals((int) Byte.MAX_VALUE, values.get("bt"));
		assertEquals((int) Short.MAX_VALUE, values.get("shrt"));
		assertEquals((int) Character.MAX_VALUE, values.get("chr"));
		assertEquals(Integer.MAX_VALUE, values.get("intgr"));
		assertEquals(Long.MAX_VALUE, values.get("lng"));
		assertEquals(Float.MAX_VALUE, values.get("flt"));
		assertEquals(Double.MAX_VALUE, values.get("dbl"));
		assertEquals("blabla", values.get("strng"));

//		Classes are special
//		assertEquals("java.lang.Object", ((Clazz.ClassConstant) values.get("clss")).getName());
	}

	public void testGeneric() throws Exception {
		print(System.out, WithGenerics.class.getField("field").getGenericType());
		System.out.println();
		print(System.out, Class.class);
		System.out.println();
		print(System.out, WithGenerics.class);
		System.out.println();
	}

	public void print(Appendable sb, Type t) throws Exception {
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			Class<?> c = (Class<?>) pt.getRawType();
			sb.append("L");
			sb.append(c.getCanonicalName().replace('.', '/'));
			sb.append("<");
			for (Type arg : pt.getActualTypeArguments()) {
				print(sb, arg);
			}
			sb.append(">");
			sb.append(";");
			return;
		} else if (t instanceof WildcardType) {
			sb.append("yyyy");
		} else if (t instanceof GenericArrayType) {
			sb.append("[" + ((GenericArrayType) t).getGenericComponentType());
		} else if (t instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable<?>) t;
			sb.append("T");
			sb.append(tv.getName());
			for (Type upper : tv.getBounds()) {
				if (upper != Object.class) {
					sb.append(":");
					print(sb, upper);
				}
			}
			sb.append(";");
		} else {
			Class<?> c = (Class<?>) t;
			sb.append("L");
			sb.append(c.getCanonicalName().replace('.', '/'));
			if (c instanceof GenericDeclaration) {
				GenericDeclaration gd = (GenericDeclaration) c;
				if (gd.getTypeParameters().length != 0) {
					sb.append("<");
					for (Type arg : gd.getTypeParameters()) {
						print(sb, arg);
					}
					sb.append(">");
				}
			}
			sb.append(";");
		}
	}

	/**
	 * Included an aop alliance class that is not directly referenced.
	 * 
	 */
	public void testUnacceptableReference() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("jar/nl.fuji.general.jar"));
		b.addClasspath(new File("jar/spring.jar"));
		b.setProperty("Export-Package", "nl.fuji.log");
		b.build();
		assertFalse(b.getImports().containsKey("org.aopalliance.aop"));
	}

//	public void testImplemented() throws Exception {
//		Builder a = new Builder();
//		a.addClasspath(new File("bin"));
//		a.setProperty("Private-Package", "test");
//		a.build();
//		Clazz c = a.getClassspace().get("test/Implemented.class");
//		Set<PackageRef> s = Create.set();
//	
//		Clazz.getImplementedPackages(s, a, c);
//		assertTrue(s.contains( a.getPackageRef("aQute/bnd/service")));
//	}

	public void testWildcards() throws Exception {
		Clazz c = new Clazz(a,"genericstest", null);
		c.parseClassFile(getClass().getResourceAsStream("WithGenerics.class"));
		System.out.println(c.getReferred());
		assertEquals("size ", 5, c.getReferred().size());
		assertTrue(c.getReferred().contains(a.getPackageRef("aQute/lib/osgi")));
		assertTrue(c.getReferred().contains(a.getPackageRef("java/util")));
		assertTrue(c.getReferred().contains(a.getPackageRef("java/net")));
		assertTrue(c.getReferred().contains(a.getPackageRef("java/lang")));
	}

	public void testGenericsSignature3() throws Exception {
		Clazz c = new Clazz(a,"genericstest", null);
		c.parseClassFile(getClass().getResourceAsStream("Generics.class"));
		assertTrue(c.getReferred().contains(a.getPackageRef("test")));
		assertTrue(c.getReferred().contains(a.getPackageRef("aQute/lib/osgi")));
	}

	public void testGenericsSignature2() throws Exception {
		Clazz c = new Clazz(a,"genericstest", new FileResource(new File("src/test/generics.clazz")));
		c.parseClassFile();
		assertTrue(c.getReferred().contains(a.getPackageRef("javax/swing/table")));
		assertTrue(c.getReferred().contains(a.getPackageRef("javax/swing")));
	}

	public void testGenericsSignature() throws Exception {
		Clazz c = new Clazz(a,"genericstest", new FileResource(new File("src/test/generics.clazz")));
		c.parseClassFile();
		assertTrue(c.getReferred().contains(a.getPackageRef("javax/swing/table")));
		assertTrue(c.getReferred().contains(a.getPackageRef("javax/swing")));
	}

	/**
	 * @Neil: I'm trying to use bnd to bundleize a library called JQuantLib, but
	 *        it causes an ArrayIndexOutOfBoundsException while parsing a class.
	 *        The problem is reproducible and I have even rebuilt the library
	 *        from source and get the same problem.
	 * 
	 *        Here's the stack trace:
	 * 
	 *        java.lang.ArrayIndexOutOfBoundsException: -29373 at
	 *        aQute.lib.osgi.Clazz.parseClassFile(Clazz.java:262) at
	 *        aQute.lib.osgi.Clazz.<init>(Clazz.java:101) at
	 *        aQute.lib.osgi.Analyzer.analyzeJar(Analyzer.java:1647) at
	 *        aQute.lib.osgi.Analyzer.analyzeBundleClasspath(Analyzer.java:1563)
	 *        at aQute.lib.osgi.Analyzer.analyze(Analyzer.java:108) at
	 *        aQute.lib.osgi.Builder.analyze(Builder.java:192) at
	 *        aQute.lib.osgi.Builder.doConditional(Builder.java:158) at
	 *        aQute.lib.osgi.Builder.build(Builder.java:71) at
	 *        aQute.bnd.main.bnd.doBuild(bnd.java:379) at
	 *        aQute.bnd.main.bnd.run(bnd.java:130) at
	 *        aQute.bnd.main.bnd.main(bnd.java:39)
	 * 
	 * @throws Exception
	 */

	public void testJQuantlib() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("test/jquantlib-0.1.2.jar"));
		b.setProperty("Export-Package", "*");
		b.build();
	}

	public void testMissingPackage2() throws Exception {
		InputStream in = getClass().getResourceAsStream("JobsService.clazz");
		assertNotNull(in);
		Clazz clazz = new Clazz(a,"test", null);
		clazz.parseClassFile(in);
		assertTrue(clazz.getReferred().contains(a.getPackageRef("com/linkedin/member2/pub/profile/core/view")));
	}

	public void testMissingPackage1() throws Exception {
		InputStream in = getClass().getResourceAsStream("JobsService.clazz");
		assertNotNull(in);
		Clazz clazz = new Clazz(a,"test", null);
		clazz.parseClassFile(in);

		System.out.println(clazz.getReferred());
		clazz.parseDescriptor("(IILcom/linkedin/member2/pub/profile/core/view/I18nPositionViews;)Lcom/linkedin/leo/cloud/overlap/api/OverlapQuery;");
		assertTrue(clazz.getReferred().contains(a.getPackageRef("com/linkedin/member2/pub/profile/core/view")));
	}

	public void testGeneratedClass() throws Exception {
		InputStream in = getClass().getResourceAsStream("XDbCmpXView.clazz");
		assertNotNull(in);
		Clazz clazz = new Clazz(a,"test", null);
		clazz.parseClassFile(in);
		clazz.getReferred();
	}

	public void testParameterAnnotation() throws Exception {
		InputStream in = getClass().getResourceAsStream("Test2.jclass");
		assertNotNull(in);
		Clazz clazz = new Clazz(a,"test", null);
		clazz.parseClassFile(in);
		Set<PackageRef> set = clazz.getReferred();
		assertTrue(set.contains(a.getPackageRef("test")));
		assertTrue(set.contains(a.getPackageRef("test/annotations")));
	}

	public void testLargeClass2() throws IOException {
		try {
			URL url = new URL(
					"jar:file:jar/ecj_3.2.2.jar!/org/eclipse/jdt/internal/compiler/parser/Parser.class");
			InputStream in = url.openStream();
			assertNotNull(in);
			Clazz clazz = new Clazz(a,"test", null);
			clazz.parseClassFile(in);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * Still problems with the stuff in ecj
	 */
	public void testEcj() throws Exception {
		Builder builder = new Builder();
		builder.setClasspath(new File[] { new File("jar/ecj_3.2.2.jar") });
		builder.setProperty(Analyzer.EXPORT_PACKAGE, "org.eclipse.*");
		builder.build();
		System.out.println(builder.getErrors());
		assertEquals(0, builder.getErrors().size());
		assertEquals(0, builder.getWarnings().size());
		System.out.println(builder.getErrors());
		System.out.println(builder.getWarnings());
	}

	/**
	 * This class threw an exception because we were using skip instead of
	 * skipBytes. skip is not guaranteed to real skip the amount of bytes, not
	 * even if there are still bytes left. It seems to be able to stop skipping
	 * if it is at the end of a buffer or so :-( Idiots.
	 * 
	 * The DataInputStream.skipBytes works correctly.
	 * 
	 * @throws IOException
	 */
	public void testLargeClass() throws IOException {
		InputStream in = getClass().getResourceAsStream("Parser.jclass");
		assertNotNull(in);
		try {
			Clazz clazz = new Clazz(a,"test", null);
			clazz.parseClassFile(in);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testSimple() throws Exception {
		InputStream in = getClass().getResourceAsStream("WithAnnotations.jclass");
		assertNotNull(in);
		Clazz clazz = new Clazz(a,"test", null);
		clazz.parseClassFile(in);
		Set<PackageRef> set = clazz.getReferred();
		PackageRef test = a.getPackageRef("test");
		PackageRef testAnnotations = a.getPackageRef("test/annotations");
		assertTrue(set.contains(test));
		assertTrue(set.contains(testAnnotations));
	}
}
