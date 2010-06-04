package test;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.service.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;
import aQute.libg.reporter.*;

interface WithGenerics<VERYLONGTYPE, X extends Jar> {
    List<? super VERYLONGTYPE> baz2();

    List<? extends Jar>    field = null;

    WithGenerics<URL, Jar> x     = null;
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

    
    /**
     * Included an aop alliance class that is not directly referenced.
     * 
     */
    public void testUnacceptableReference() throws Exception {
        Builder b = new Builder();
        b.addClasspath( new File("jar/nl.fuji.general.jar"));
        b.addClasspath( new File("jar/spring.jar"));
        b.setProperty("Export-Package", "nl.fuji.log");
        b.build();
        assertFalse(b.getImports().containsKey("org.aopalliance.aop"));
    }
    
    
    public void testImplemented() throws Exception {
        Builder a = new Builder();
        a.addClasspath( new File("bin"));
        a.setProperty("Private-Package", "test");
        a.build();
        Clazz c = a.getClassspace().get("test/Implemented.class");
        Set<String> s = Create.set();
        Clazz.getImplementedPackages(s, a, c);
        assertTrue(s.contains("aQute.bnd.service"));
    }

    public void testWildcards() throws Exception {
        Clazz c = new Clazz("genericstest", null);
        c.parseClassFile(getClass().getResourceAsStream("WithGenerics.class"));
        System.out.println(c.getReferred());
        assertEquals("size ", 5, c.getReferred().size());
        assertTrue(c.getReferred().contains("aQute.lib.osgi"));
        assertTrue(c.getReferred().contains("java.util"));
        assertTrue(c.getReferred().contains("java.net"));
        assertTrue(c.getReferred().contains("java.lang"));
    }

    public void testWeirdClass() throws Exception {
        Builder b = new Builder();
        b.setProperty("Private-Package", "crap");
        b.addClasspath(new File("test/craptest"));
        b.build();
        System.out.println(b.getWarnings());
        System.out.println(b.getErrors());
        assertEquals(1, b.getErrors().size());
        assertEquals(1, b.getWarnings().size());

    }

    public void testGenericsSignature3() throws Exception {
        Clazz c = new Clazz("genericstest", null);
        c.parseClassFile(getClass().getResourceAsStream("Generics.class"));
        assertTrue(c.getReferred().contains("test"));
        assertTrue(c.getReferred().contains("aQute.lib.osgi"));
    }

    public void testGenericsSignature2() throws Exception {
        Clazz c = new Clazz("genericstest", new FileResource(new File(
                "src/test/generics.clazz")));
        c.parseClassFile();
        assertTrue(c.getReferred().contains("javax.swing.table"));
        assertTrue(c.getReferred().contains("javax.swing"));
    }

    public void testGenericsSignature() throws Exception {
        Clazz c = new Clazz("genericstest", new FileResource(new File(
                "src/test/generics.clazz")));
        c.parseClassFile();
        assertTrue(c.getReferred().contains("javax.swing.table"));
        assertTrue(c.getReferred().contains("javax.swing"));
    }

    /**
     * @Neil: I'm trying to use bnd to bundleize a library called JQuantLib, but
     *        it causes an ArrayIndexOutOfBoundsException while parsing a class.
     *        The problem is reproducible and I have even rebuilt the library
     *        from source and get the same problem.
     * 
     * Here's the stack trace:
     * 
     * java.lang.ArrayIndexOutOfBoundsException: -29373 at
     * aQute.lib.osgi.Clazz.parseClassFile(Clazz.java:262) at
     * aQute.lib.osgi.Clazz.<init>(Clazz.java:101) at
     * aQute.lib.osgi.Analyzer.analyzeJar(Analyzer.java:1647) at
     * aQute.lib.osgi.Analyzer.analyzeBundleClasspath(Analyzer.java:1563) at
     * aQute.lib.osgi.Analyzer.analyze(Analyzer.java:108) at
     * aQute.lib.osgi.Builder.analyze(Builder.java:192) at
     * aQute.lib.osgi.Builder.doConditional(Builder.java:158) at
     * aQute.lib.osgi.Builder.build(Builder.java:71) at
     * aQute.bnd.main.bnd.doBuild(bnd.java:379) at
     * aQute.bnd.main.bnd.run(bnd.java:130) at
     * aQute.bnd.main.bnd.main(bnd.java:39)
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
        Clazz clazz = new Clazz("test", null);
        clazz.parseClassFile(in);
        assertTrue(clazz.getReferred().contains(
                "com.linkedin.member2.pub.profile.core.view"));
    }

    public void testMissingPackage1() throws Exception {
        InputStream in = getClass().getResourceAsStream("JobsService.clazz");
        assertNotNull(in);
        Clazz clazz = new Clazz("test", null);
        clazz.parseClassFile(in);

        System.out.println(clazz.getReferred());
        clazz
                .parseDescriptor("(IILcom/linkedin/member2/pub/profile/core/view/I18nPositionViews;)Lcom/linkedin/leo/cloud/overlap/api/OverlapQuery;");
        assertTrue(clazz.getReferred().contains(
                "com.linkedin.member2.pub.profile.core.view"));
    }

    public void testGeneratedClass() throws IOException {
        InputStream in = getClass().getResourceAsStream("XDbCmpXView.clazz");
        assertNotNull(in);
        Clazz clazz = new Clazz("test", null);
        clazz.parseClassFile(in);
        clazz.getReferred();
    }

    public void testParameterAnnotation() throws IOException {
        InputStream in = getClass().getResourceAsStream("Test2.jclass");
        assertNotNull(in);
        Clazz clazz = new Clazz("test", null);
        clazz.parseClassFile(in);
        Set<String> set = clazz.getReferred();
        assertTrue(set.contains("test"));
        assertTrue(set.contains("test.annotations"));
    }

    public void testLargeClass2() throws IOException {
        try {
            URL url = new URL(
                    "jar:file:jar/ecj_3.2.2.jar!/org/eclipse/jdt/internal/compiler/parser/Parser.class");
            InputStream in = url.openStream();
            assertNotNull(in);
            Clazz clazz = new Clazz("test", null);
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
            Clazz clazz = new Clazz("test", null);
            clazz.parseClassFile(in);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testSimple() throws IOException {
        InputStream in = getClass().getResourceAsStream(
                "WithAnnotations.jclass");
        assertNotNull(in);
        Clazz clazz = new Clazz("test", null);
        clazz.parseClassFile(in);
        Set<String> set = clazz.getReferred();
        assertTrue(set.contains("test"));
        assertTrue(set.contains("test.annotations"));
    }
}
