package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import junit.framework.*;
import aQute.lib.osgi.*;

public class VerifierTest extends TestCase {
	
	
	public void testnativeCode() throws Exception {
		Builder b = new Builder();
		b.addClasspath( new File("bin"));
		b.setProperty("-resourceonly", "true");
		b.setProperty("Include-Resource", "native/win32/NTEventLogAppender-1.2.dll;literal='abc'");
		b.setProperty("Bundle-NativeCode", "native/win32/NTEventLogAppender-1.2.dll; osname=Win32; processor=x86");
		b.build();
		Verifier v = new Verifier(b);
		
		v.verifyNative();
		System.out.println( v.getErrors());
		assertEquals(0, v.getErrors().size());
		v.close();
		b.close();
	}
	
	

    public void testFilter() {

        testFilter("(&(a=b)(c=1))");
        testFilter("(&(a=b)(!(c=1))(&(c=1))(c=1)(c=1)(c=1)(c=1)(c=1)(c=1)(c=1)(c=1))");
        testFilter("(!(a=b))");
        testInvalidFilter("(!(a=b)(c=2))");
        testInvalidFilter("(axb)");
        testInvalidFilter("(a=3 ");
        testFilter("(room=*)");
        testFilter("(room=bedroom)");
        testFilter("(room~= B E D R O O M )");
        testFilter("(room=abc)");
        testFilter(" ( room >=aaaa)");
        testFilter("(room <=aaaa)");
        testFilter("  ( room =b*) ");
        testFilter("  ( room =*m) ");
        testFilter("(room=bed*room)");
        testFilter("  ( room =b*oo*m) ");
        testFilter("  ( room =*b*oo*m*) ");
        testFilter("  ( room =b*b*  *m*) ");
        testFilter("  (& (room =bedroom) (channel ~=34))");
        testFilter("  (&  (room =b*)  (room =*x) (channel=34))");
        testFilter("(| (room =bed*)(channel=222)) ");
        testFilter("(| (room =boom*)(channel=101)) ");
        testFilter("  (! (room =ab*b*oo*m*) ) ");
        testFilter("  (status =\\(o*\\\\\\)\\*) ");
        testFilter("  (canRecord =true\\(x\\)) ");
        testFilter("(max Record Time <=140) ");
        testFilter("(shortValue >=100) ");
        testFilter("(intValue <=100001) ");
        testFilter("(longValue >=10000000000) ");
        testFilter("  (  &  (  byteValue <=100)  (  byteValue >=10)  )  ");
        testFilter("(weirdValue =100) ");
        testFilter("(bigIntValue =4123456) ");
        testFilter("(bigDecValue =4.123456) ");
        testFilter("(floatValue >=1.0) ");
        testFilter("(doubleValue <=2.011) ");
        testFilter("(charValue ~=a) ");
        testFilter("(booleanValue =true) ");
        testFilter("(primIntArrayValue =1) ");
        testFilter("(primLongArrayValue =2) ");
        testFilter("(primByteArrayValue =3) ");
        testFilter("(primShortArrayValue =1) ");
        testFilter("(primFloatArrayValue =1.1) ");
        testFilter("(primDoubleArrayValue =2.2) ");
        testFilter("(primCharArrayValue ~=D) ");
        testFilter("(primBooleanArrayValue =false) ");
        testFilter("(& (| (room =d*m) (room =bed*) (room=abc)) (! (channel=999)))");
        testFilter("(room=bedroom)");
        testFilter("(*=foo)");
        testInvalidFilter("(!  ab=b)");
        testInvalidFilter("(|   ab=b)");
        testInvalidFilter("(&=c)");
        testInvalidFilter("(!=c)");
        testInvalidFilter("(|=c)");
        testInvalidFilter("(&    ab=b)");
        testInvalidFilter("(!ab=*)");
        testInvalidFilter("(|ab=*)");
        testInvalidFilter("(&ab=*)");
        testFilter("(empty=)");
        testFilter("(empty=*)");
        testFilter("(space= )");
        testFilter("(space=*)");
        testFilter("(intvalue=*)");
        testFilter("(intvalue=b)");
        testFilter("(intvalue=)");
        testFilter("(longvalue=*)");
        testFilter("(longvalue=b)");
        testFilter("(longvalue=)");
        testFilter("(shortvalue=*)");
        testFilter("(shortvalue=b)");
        testFilter("(shortvalue=)");
        testFilter("(bytevalue=*)");
        testFilter("(bytevalue=b)");
        testFilter("(bytevalue=)");
        testFilter("(charvalue=*)");
        testFilter("(charvalue=)");
        testFilter("(floatvalue=*)");
        testFilter("(floatvalue=b)");
        testFilter("(floatvalue=)");
        testFilter("(doublevalue=*)");
        testFilter("(doublevalue=b)");
        testFilter("(doublevalue=)");
        testFilter("(booleanvalue=*)");
        testFilter("(booleanvalue=b)");
        testFilter("(booleanvalue=)");

        testInvalidFilter("");
        testInvalidFilter("()");
        testInvalidFilter("(=foo)");
        testInvalidFilter("(");
        testInvalidFilter("(abc = ))");
        testInvalidFilter("(& (abc = xyz) (& (345))");
        testInvalidFilter("  (room = b**oo!*m*) ) ");
        testInvalidFilter("  (room = b**oo)*m*) ) ");
        testInvalidFilter("  (room = *=b**oo*m*) ) ");
        testInvalidFilter("  (room = =b**oo*m*) ) ");
        testFilter("(shortValue =100*) ");
        testFilter("(intValue =100*) ");
        testFilter("(longValue =100*) ");
        testFilter("(  byteValue =1*00  )");
        testFilter("(bigIntValue =4*23456) ");
        testFilter("(bigDecValue =4*123456) ");
        testFilter("(floatValue =1*0) ");
        testFilter("(doubleValue =2*011) ");
        testFilter("(charValue =a*) ");
        testFilter("(booleanValue =t*ue) ");
    }

    private void testFilter(String string) {
        int index = Verifier.verifyFilter(string, 0);
        while (index < string.length() && Character.isWhitespace(string.charAt(index)))
                index++;

        if ( index != string.length())
            throw new IllegalArgumentException("Characters after filter");
    }

    private void testInvalidFilter(String string) {
        try {
            testFilter(string);
            fail("Invalid filter");
        } catch (Exception e) {
        }
    }

    public void testBundleActivationPolicyNone() throws Exception {
        Builder v = new Builder();
        v.setProperty("Private-Package", "test.activator");
        v.addClasspath( new File("bin"));
        v.build();
        assertTrue(v.check());
    }

    public void testBundleActivationPolicyBad() throws Exception {
        Builder v = new Builder();
        v.setProperty("Private-Package", "test.activator");
        v.addClasspath( new File("bin"));
        v.setProperty(Constants.BUNDLE_ACTIVATIONPOLICY, "eager");
        v.build();
        assertTrue(v.check("Bundle-ActivationPolicy set but is not set to lazy: eager"));
    }

    public void testBundleActivationPolicyGood() throws Exception {
        Builder v = new Builder();
        v.setProperty("Private-Package", "test.activator");
        v.addClasspath( new File("bin"));
        v.setProperty(Constants.BUNDLE_ACTIVATIONPOLICY, "lazy   ;   hello:=1");
        v.build();
        assertTrue(v.check());
    }

    public void testBundleActivationPolicyMultiple() throws Exception {
        Builder v = new Builder();
        v.setProperty("Private-Package", "test.activator");
        v.addClasspath( new File("bin"));
        v.setProperty(Constants.BUNDLE_ACTIVATIONPOLICY, "lazy;hello:=1,2");
        v.build();
        assertTrue(v.check("Bundle-ActivationPolicy has too many arguments lazy;hello:=1,2"));
    }

    public void testInvalidCaseForHeader() throws Exception {
        Properties p = new Properties();
        p.put("Export-package", "org.apache.mina.*");
        p.put("Bundle-Classpath", ".");
        Analyzer analyzer = new Analyzer();
        analyzer.setProperties(p);
        analyzer.getProperties();
        System.out.println("Errors   " + analyzer.getErrors());
        System.out.println("Warnings " + analyzer.getWarnings());
        assertEquals(0, analyzer.getErrors().size());
        assertEquals(2, analyzer.getWarnings().size());
    }

    public void testSimple() throws Exception {
        Builder bmaker = new Builder();
        bmaker.addClasspath(new File("jar/mina.jar") );
        bmaker.set("Export-Package", "org.apache.mina.*;version=1");
        bmaker.set("DynamicImport-Package", "org.slf4j");
        Jar jar = bmaker.build();
        assertTrue(bmaker.check());
        
        Manifest m = jar.getManifest();
        m.write(System.out);
        assertFalse(m.getMainAttributes().getValue("Import-Package").indexOf(
                "org.slf4j") >= 0);
        assertTrue(m.getMainAttributes().getValue("DynamicImport-Package")
                .indexOf("org.slf4j") >= 0);
    }

}
