package test.annotationheaders;

import java.io.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.regex.*;

import junit.framework.*;
import aQute.bnd.annotation.headers.*;
import aQute.bnd.annotation.licenses.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;

public class AnnotationHeadersTest extends TestCase {

	@RequireCapability(ns="osgi.webresource",filter="(&(osgi.webresource=/google/angular)${frange;${@version}})")
	@interface Angular {
		
	}
	
	@RequireCapability(ns="not.there",filter="(a=3)")
	@interface Notused {
		
	}
	@BundleDevelopers("Peter.Kriens@aQute.biz;name='Peter Kriens';organization=aQute")
	@interface pkriens {}
	
	@BundleContributors(value="Mieke.Kriens@aQute.biz", name="Mieke Kriens", organization="aQute")
	@interface mkriens {}
	@BundleContributors(value="Thomas.Kriens@aQute.biz", name="Thomas Kriens", organization="aQute")
	@interface tkriens {}
	
	@BundleContributors(value="Mischa.Kriens@aQute.biz", name="Mischa Kriens", organization="aQute")
	@interface mischakriens {}
	
	@RequireCapability(ns="abcdef", filter="(&(abcdef=xyz)${frange;${@version}})")
	@ASL_2_0
	@pkriens
	@mkriens
	@tkriens
	class A {
		
	}

	@BundleDocURL("http://www.aQute.biz")
	@BundleCopyright("(c) ${tstamp;yyyy} aQute All Rights Reserved and other baloney")
	@pkriens
	@mkriens
	@tkriens
	@BundleCategory(Category.adoption)
	
	class B {
		
	}
	
	@BundleCopyright("[[\n\rXyz: Hello world. , ; = \\]]")
	class Z {
		
	}
	
	
	@BundleCopyright("v=${@version} p=${@package} c=${@class} s=${@class-short}")
	@Angular
	class C {
		
	}

	//
	// Check if we can 
	interface X {
		@RequireCapability(ns="x",filter="(x=xx)")
		@interface Require {}
		@ProvideCapability(ns="x",name="xx")
		@interface Provide {}
	}
	@X.Provide
	class XImpl {
	}
		
	public void testBasic() throws Exception {
		Builder b = new Builder();
		b.addClasspath(new File("bin"));
		b.setProperty("Private-Package", "test.annotationheaders");
		b.build();
		assertTrue(b.check());
		Manifest manifest = b.getJar().getManifest();
		String rc = manifest.getMainAttributes().getValue(Constants.REQUIRE_CAPABILITY);
		assertNotNull(rc);
		System.out.println(rc);
		assertTrue(rc.contains("osgi.webresource=/google"));
		assertTrue(rc.contains(">=1.2.3")); // from package info
		assertTrue(rc.contains(">=2.0.0")); // from package info
		
		assertFalse(rc.contains("xx"));
		
		String pc = manifest.getMainAttributes().getValue(Constants.PROVIDE_CAPABILITY);
		assertNotNull(pc);
		System.out.println(pc);
		assertTrue(pc.contains("x=xx"));
		

		assertFalse(rc.contains("not.there"));
		
		String bl = manifest.getMainAttributes().getValue(Constants.BUNDLE_LICENSE);
		assertNotNull(bl);
		System.out.println(bl);
		assertTrue(bl.contains("http://www.opensource.org/licenses/apache2.0.php;"));
		assertTrue(bl.contains("MIT"));
		assertFalse(bl.contains("GPL"));
		
		String dv = manifest.getMainAttributes().getValue(Constants.BUNDLE_DEVELOPERS);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("Peter Kriens"));
		
		dv = manifest.getMainAttributes().getValue(Constants.BUNDLE_CONTRIBUTORS);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("Mieke Kriens"));
		assertTrue(dv.contains("Thomas Kriens"));
		assertFalse(dv.contains("Mischa Kriens"));
		
		dv = manifest.getMainAttributes().getValue(Constants.BUNDLE_COPYRIGHT);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("other baloney"));
		Matcher m = Pattern.compile("([0-9]{4})").matcher(dv);
		assertTrue( m.find());
		assertTrue( Integer.parseInt(m.group(1)) >= 2014);
		assertTrue(dv.contains("v=1.2.3"));
		assertTrue(dv.contains("p=test.annotationheaders"));
		assertTrue(dv.contains("c=test.annotationheaders.AnnotationHeadersTest$C"));
		assertTrue(dv.contains("s=AnnotationHeadersTest$C"));
		
		
		
		dv = manifest.getMainAttributes().getValue(Constants.BUNDLE_DOCURL);
		assertNotNull(dv);
		System.out.println(dv);
		assertTrue(dv.contains("http://www.aQute.biz"));
		

		Parameters cpr = new Parameters(manifest.getMainAttributes().getValue(Constants.BUNDLE_COPYRIGHT));
		for ( Entry<String,Attrs> e : cpr.entrySet()) {
			System.out.println("cpr: " + e);
		}
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Jar.writeManifest(manifest,bout);
		String s = new String( bout.toByteArray(), "UTF-8");
		System.out.println(s);
		ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
		Manifest m2 = new Manifest(bin);
		String v = m2.getMainAttributes().getValue(Constants.BUNDLE_COPYRIGHT);
		assertNotNull(v );
		assertTrue( v.contains("Hello world"));
		assertNull( m2.getMainAttributes().getValue("Xyz"));
		b.close();
	}
	
	
	
}
