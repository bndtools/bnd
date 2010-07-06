package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.lib.osgi.*;

public class MacroTest extends TestCase {
    Processor proc = new Processor();
    
    public void testEnv() {
        String s= proc.getReplacer().process("${env;USER}");
        assertNotNull(s);
    }
    
    /**
     * Testing an example with nesting that was supposd not to work
     */
    
    public void testSuper() {
        Processor top = new Processor();
        Processor bottom = new Processor(top);
        top.setProperty("a", "top.a");
        top.setProperty("b", "top.b");
        top.setProperty("c", "top.c");
        bottom.setProperty("a", "bottom.a");
        bottom.setProperty("b", "${super;a}");
        bottom.setProperty("c", "-${super;c}-");
        assertEquals("bottom.a", bottom.getProperty("a"));
        assertEquals("top.a", bottom.getProperty("b"));
        assertEquals("-top.c-", bottom.getProperty("c"));
    }
    
    /**
     * Testing an example with nesting that was supposd not to work
     */
    
    public void testNesting2() {
        Processor p = new Processor();
        p.setProperty("groupId", "com.trivadis.tomas");
        p.setProperty("artifactId", "common");
        p.setProperty("bsn", "${if;${symbolicName};${symbolicName};${groupId}.${artifactId}}");
        p.setProperty("Bundle-SymbolicName", "${bsn}");       
        p.setProperty("symbolicName", "");

        // Not set, so get the maven name
        assertEquals("com.trivadis.tomas.common", p.getProperty("Bundle-SymbolicName"));
        
        // Set it
        p.setProperty("symbolicName", "testing");
        assertEquals("testing", p.getProperty("Bundle-SymbolicName"));

        // And remove it
        p.setProperty("symbolicName", "");
        assertEquals("com.trivadis.tomas.common", p.getProperty("Bundle-SymbolicName"));
    }
    
    /** 
     * Verify system command
     */
    
    public void testSystem() throws Exception {
        Properties p = new Properties();
        Macro macro = new Macro(p,proc);
        assertEquals("Hello World", macro.process("${system;echo Hello World}"));        
        assertTrue( macro.process("${system;wc;Hello World}").matches("[0-9]+\\s*[0-9]+\\s*[0-9]+"));        
    }

    /**
     * Check that variables override macros.
     */
    public void testPriority(){
        Properties p = new Properties();
        p.put("now", "not set");
        Macro macro = new Macro(p,proc);
        assertEquals("not set", macro.process("${now}"));
        
    }
    public void testNames() {
        Properties p = new Properties();
        p.put("a", "a");
        p.put("aa", "aa");
        Macro macro = new Macro(p,proc);
        
        assertEquals("aa", macro.process("${${a}${a}}"));
    }
    
    public void testVersion() throws Exception {
        Properties p = new Properties();
        Macro macro = new Macro(p,proc);
        assertEquals( "1.0.0", macro.process("${version;===;1.0.0}"));
        assertEquals( "1.0.1", macro.process("${version;==+;1.0.0}"));
        assertEquals( "1.1.1", macro.process("${version;=++;1.0.0}"));
        assertEquals( "2.1.1", macro.process("${version;+++;1.0.0}"));
        assertEquals( "0.1.1", macro.process("${version;-++;1.0.0}"));
        assertEquals( "0.1.1", macro.process("${version;-++;1.0.0}"));
        assertEquals( "0.0.0", macro.process("${version;---;1.1.1}"));
        assertEquals( "0.0", macro.process("${version;--;1.1.1}"));
        assertEquals( "1", macro.process("${version;=;1.1.1}"));
        assertEquals( "[1.1,1.2)", macro.process("[${version;==;1.1.1},${version;=+;1.1.1})"));
        assertEquals( "1.1", macro.process("${version;==;1.1.1}"));
        assertEquals( "0.1.0", macro.process("${version;=+0;0.0.1}"));
        assertEquals( "1.0.0", macro.process("${version;+00;0.1.1}"));
        
    }
    
    
    /**
     * Test the wc function
     */
    
    public void testWc() {
        Properties p = new Properties();
        Macro macro = new Macro(p,proc);
        String a = macro.process("${lsr;" + new File("src/test").getAbsolutePath() + ";*.java}");
        assertTrue( a.contains("MacroTest.java"));
        assertTrue( a.contains("ManifestTest.java"));
        assertFalse( a.contains(".classpath"));
        assertFalse( a.contains("src/test/MacroTest.java"));
        assertFalse( a.contains("src/test/ManifestTest.java"));

        String b = macro.process("${lsa;" + new File("src/test").getAbsolutePath() + ";*.java}");
        assertTrue( b.contains("src/test/MacroTest.java"));
        assertTrue( b.contains("src/test/ManifestTest.java"));
    }
    /**
     * Check the uniq command
     */
    
    public void testUniq() {
        Builder builder = new Builder();
        Properties p = new Properties();
        p.setProperty("a" , "${uniq;1}");
        p.setProperty("b" , "${uniq;1,2}");
        p.setProperty("c" , "${uniq;1;2}");
        p.setProperty("d" , "${uniq;1; 1,  2 , 3}");
        p.setProperty("e" , "${uniq;1; 1 , 2 ;      3;3,4,5,6}");
        builder.setProperties(p);
        assertEquals( "1,2,3", builder.getProperty("d"));
        assertEquals( "1,2", builder.getProperty("b"));
        assertEquals( "1", builder.getProperty("a"));
        assertEquals( "1,2", builder.getProperty("c"));
        assertEquals( "1,2,3", builder.getProperty("d"));
        assertEquals( "1,2,3,4,5,6", builder.getProperty("e"));
        
    }
    /**
     * Test arguments with difficult characters like ;
     */
    
    public void testEscapedArgs() {
        Builder builder = new Builder();
        Properties p = new Properties();
        p.setProperty("x" , "${replace;1,2,3;.+;$0\\;version=1}");
        builder.setProperties(p);
        assertEquals( "1;version=1, 2;version=1, 3;version=1", builder.getProperty("x"));
        
    }
    /**
     * Check if variables that contain variables, ad nauseum, really wrk
     */
    public void testNested() {
        Builder builder = new Builder();
        Properties p = new Properties();
        p.setProperty("a", ".");
        p.setProperty("b", "${a}");
        p.setProperty("c", "${b}");
        
        p.setProperty("d", "${tstamp;${format};${aug152008}}");
        p.setProperty("format", "yyyy");
        p.setProperty("aug152008", "1218810097322");

        p.setProperty("f", "${d}");
        p.setProperty("aug152008", "1218810097322");

        builder.setProperties(p);
        assertEquals( ".", builder.getProperty("c"));
        assertEquals( "2008", builder.getProperty("d"));
        assertEquals( builder.getProperty("f"), builder.getProperty("d"));
    }
    
    public void testLoop() {
        Builder builder = new Builder();
        Properties p = new Properties();
        p.setProperty("a", "${b}");
        p.setProperty("b", "${a}");
        
        p.setProperty("d", "${e}");
        p.setProperty("e", "${f}");
        p.setProperty("f", "${g}");
        p.setProperty("g", "${h}");
        p.setProperty("h", "${d}");
        
        builder.setProperties(p);
        assertEquals( "${infinite:[a,b]}", builder.getProperty("a"));
        assertEquals( "${infinite:[d,h,g,f,e]}", builder.getProperty("d"));
    }
    
    public void testTstamp() {
        String aug152008 = "1218810097322";
        Properties p = new Properties();
        Macro m = new Macro(p, proc);
        assertEquals("200808151621", m.process("${tstamp;yyyyMMddHHmm;"+aug152008+"}"));
        //assertEquals( "2008", m.process("${tstamp;yyyy}"));
    }
    public void testIsfile() {
        Properties p = new Properties();
        Macro m = new Macro(p, proc);
        assertEquals("true", m.process("${isfile;.project}"));
        assertEquals("false", m.process("${isfile;thisfiledoesnotexist}"));
    }

    public void testParentFile() {
        Properties p = new Properties();
        Macro m = new Macro(p, proc);
        assertTrue(m.process("${dir;.project}").endsWith("biz.aQute.bndlib"));
    }

    public void testBasename() {
        Properties p = new Properties();
        Macro m = new Macro(p, proc);
        assertEquals("biz.aQute.bndlib", m.process("${basename;${dir;.project}}"));
    }

    public void testMavenVersionMacro() throws Exception {
        Builder builder = new Builder();
        Properties p = new Properties();
        p.setProperty("Export-Package", "org.objectweb.*;version=1.5-SNAPSHOT");
        builder.setProperties(p);
        builder.setClasspath(new File[] { new File("jar/asm.jar") });
        Jar jar = builder.build();
        Manifest manifest = jar.getManifest();
        String export = manifest.getMainAttributes().getValue("Export-Package");
        assertNotNull(export);
        assertTrue("Test snapshot version", export.contains("1.5.0.SNAPSHOT"));
    }

    /**
     * Check if we can check for the defintion of a variable
     */

    public void testDef() {
        Properties p = new Properties();
        p.put("set.1", "1");
        p.put("set.2", "2");
        Macro m = new Macro(p, proc);
        assertEquals("NO", m.process("${if;${def;set.3};YES;NO}"));
        assertEquals("YES", m.process("${if;${def;set.1};YES;NO}"));
        assertEquals("YES", m.process("${if;${def;set.2};YES;NO}"));
    }

    /**
     * NEW
     */
    public void testReplace() {
        Properties p = new Properties();
        p.put("specs", "a,b, c,    d");
        Macro m = new Macro(p, proc);
        assertEquals("xay, xby, xcy, xdy", m
                .process("${replace;${specs};([^\\s]+);x$1y}"));
    }

    public void testToClassName() {
        Properties p = new Properties();
        proc = new Processor();
        Macro m = new Macro(p, proc);
        assertEquals("com.acme.test.Test", m
                .process("${toclassname;com/acme/test/Test.class}"));
        assertEquals("Test", m.process("$<toclassname;Test.class>"));
        assertEquals("Test,com.acme.test.Test", m
                .process("${toclassname;Test.class, com/acme/test/Test.class}"));
        assertEquals("", m.process("$(toclassname;Test)"));
        assertEquals("com/acme/test/Test.class", m
                .process("$[toclasspath;com.acme.test.Test]"));
        assertEquals("Test.class", m.process("${toclasspath;Test}"));
        assertEquals("Test.class,com/acme/test/Test.class", m
                .process("${toclasspath;Test,com.acme.test.Test}"));
    }

    public void testFindPath() throws IOException {
        Analyzer analyzer = new Analyzer();
        analyzer.setJar(new File("jar/asm.jar"));
        Properties p = new Properties();
        Macro m = new Macro(p, analyzer);

        assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}").indexOf(
                "FieldVisitor.xyz,") >= 0);
        assertTrue(m.process("${findname;(.*)\\.class;$1.xyz}").indexOf(
                "MethodVisitor.xyz,") >= 0);
        assertTrue(m.process("${findpath;(.*)\\.class}").indexOf(
                "org/objectweb/asm/AnnotationVisitor.class,") >= 0);
        assertTrue(m
                .process("${findpath;(.*)\\.class}")
                .indexOf(
                        "org/objectweb/asm/ByteVector.class, org/objectweb/asm/ClassAdapter.class,") >= 0);
        assertEquals("META-INF/MANIFEST.MF", m
                .process("${findpath;META-INF/MANIFEST.MF}"));
        assertEquals("Label.class", m.process("${findname;Label\\..*}"));
        assertEquals("Adapter, Visitor, Writer", m
                .process("${findname;Method(.*)\\.class;$1}"));
    }

    public void testWarning() {
        Properties p = new Properties();
        p.put("three", "333");
        p.put("empty", "");
        p.put("real", "true");
        proc = new Processor();
        Macro m = new Macro(p, proc);

        m.process("    ${warning;xw;1;2;3 ${three}}");
        m.process("    ${error;xe;1;2;3 ${three}}");
        m.process("    ${if;1;$<a>}");
        assertEquals("xw", proc.getWarnings().get(0));
        assertEquals("1", proc.getWarnings().get(1));
        assertEquals("2", proc.getWarnings().get(2));
        assertEquals("3 333", proc.getWarnings().get(3));

        assertEquals("xe", proc.getErrors().get(0));
        assertEquals("1", proc.getErrors().get(1));
        assertEquals("2", proc.getErrors().get(2));
        assertEquals("3 333", proc.getErrors().get(3));
    }

    public void testNestedReplace() {
        Properties p = new Properties();
        Macro m = new Macro(p, proc);
        String value = m
                .process("xx$(replace;1.2.3-SNAPSHOT;(\\d(\\.\\d)+).*;$1)xx");
        System.out.println(proc.getWarnings());
        assertEquals("xx1.2.3xx", value);

        assertEquals(
                "xx1.222.3xx",
                m
                        .process("xx$(replace;1.222.3-SNAPSHOT;(\\d+(\\.\\d+)+).*;$1)xx"));

        p.put("a", "aaaa");
        assertEquals("[cac]", m.process("$[replace;acaca;a(.*)a;[$1]]"));
        assertEquals("xxx", m.process("$(replace;yxxxy;[^x]*(x+)[^x]*;$1)"));
        assertEquals("xxx", m.process("$(replace;yxxxy;([^x]*(x+)[^x]*);$2)"));

    }

    public void testParentheses() {
        Properties p = new Properties();
        Macro m = new Macro(p, proc);
        String value = m.process("$(replace;();(\\(\\));$1)");
        assertEquals("()", value);
    }

    public void testSimple() {
        Properties p = new Properties();
        p.put("a", "aaaa");
        Macro m = new Macro(p, proc);
        assertEquals("aaaa", m.process("${a}"));
        assertEquals("aaaa", m.process("$<a>"));
        assertEquals("aaaa", m.process("$(a)"));
        assertEquals("aaaa", m.process("$[a]"));
        assertEquals("aaaa", m.process("$‹a›"));

        assertEquals("xaaaax", m.process("x${a}x"));
        assertEquals("xaaaaxaaaax", m.process("x${a}x${a}x"));
    }

    public void testFilter() {
        Properties p = new Properties();
        p.put("a", "aaaa");
        Macro m = new Macro(p, proc);
        assertEquals("aa,cc,ee", m
                .process("${filter;aa,bb,cc,dd,ee,ff;[ace]+}"));
        assertEquals("aaaa,cc,ee", m
                .process("${filter;${a},bb,cc,dd,ee,ff;[ace]+}"));
        assertEquals("bb,dd,ff", m
                .process("${filter;${a},bb,cc,dd,ee,ff;[^ace]+}"));
    }

    public void testFilterOut() {
        Properties p = new Properties();
        p.put("a", "aaaa");
        Macro m = new Macro(p, proc);
        assertEquals("bb,dd,ff", m
                .process("${filterout;aa,bb,cc,dd,ee,ff;[ace]+}"));
        assertEquals("bb,dd,ff", m
                .process("${filterout;${a},bb,cc,dd,ee,ff;[ace]+}"));
        assertEquals("aaaa,cc,ee", m
                .process("${filterout;${a},bb,cc,dd,ee,ff;[^ace]+}"));
    }

    public void testSort() {
        Properties p = new Properties();
        p.put("a", "aaaa");
        Macro m = new Macro(p, proc);
        assertEquals("aa,bb,cc,dd,ee,ff", m
                .process("${sort;aa,bb,cc,dd,ee,ff}"));
        assertEquals("aa,bb,cc,dd,ee,ff", m
                .process("${sort;ff,ee,cc,bb,dd,aa}"));
        assertEquals("aaaa,bb,cc,dd,ee,ff", m
                .process("${sort;ff,ee,cc,bb,dd,$<a>}"));
    }

    public void testJoin() {
        Properties p = new Properties();
        p.put("a", "aaaa");
        Macro m = new Macro(p, proc);
        assertEquals("aa,bb,cc,dd,ee,ff", m
                .process("${join;aa,bb,cc,dd,ee,ff}"));
        assertEquals("aa,bb,cc,dd,ee,ff", m
                .process("${join;aa,bb,cc;dd,ee,ff}"));
        assertEquals("aa,bb,cc,dd,ee,ff", m
                .process("${join;aa;bb;cc;dd;ee,ff}"));
    }

    public void testIf() {
        Properties p = new Properties();
        p.put("a", "aaaa");
        Macro m = new Macro(p, proc);
        assertEquals("aaaa", m.process("${if;1;$<a>}"));
        assertEquals("", m.process("${if;;$<a>}"));
        assertEquals("yes", m.process("${if;;$<a>;yes}"));
    }

    public void testLiteral() {
        Properties p = new Properties();
        p.put("a", "aaaa");
        Macro m = new Macro(p, proc);
        assertEquals("${aaaa}", m.process("${literal;$<a>}"));
    }
}
