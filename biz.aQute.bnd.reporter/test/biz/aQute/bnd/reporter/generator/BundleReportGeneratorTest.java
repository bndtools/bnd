package biz.aQute.bnd.reporter.generator;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.plugins.ComponentsEntryPlugin;
import biz.aQute.bnd.reporter.plugins.XsltTransformerPlugin;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;

public class BundleReportGeneratorTest extends TestCase {
	
	private final Set<String> a = new HashSet<>();
	
	public void testBundleReportModel()
	throws Exception {
		final Processor ws = new Processor();
		ws.setBase(new File("testresources"));
		final Jar jar = new Jar(new File("testresources/org.component.test.jar"));
		
		try (BundleReportGenerator rg = new BundleReportGenerator(jar, ws)) {
			rg.addClose(jar);
			
			a.clear();
			GeneratorAsserts.verify(rg, 1, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "excludes='manifest'");
			GeneratorAsserts.verify(rg, 0, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports='notFound'");
			GeneratorAsserts.verify(rg, 1, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports='../bnd.bnd:prop:properties'");
			GeneratorAsserts.verify(rg, 2, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports='../bnd.bnd::properties'");
			GeneratorAsserts.verify(rg, 2, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports='prop.properties: : '");
			GeneratorAsserts.verify(rg, 2, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports='prop.properties: '");
			GeneratorAsserts.verify(rg, 2, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports='prop.properties'");
			GeneratorAsserts.verify(rg, 2, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports='c ,:c'");
			GeneratorAsserts.verify(rg, 1, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "includes='manifest'");
			ws.addBasicPlugin(new ComponentsEntryPlugin());
			GeneratorAsserts.verify(rg, 1, a, 0);
			
			a.clear();
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "locales='en_US',@a=cool");
			GeneratorAsserts.verify(rg, 4, a, 0);
		}
	}
	
	public void testBundleReportGen()
	throws Exception {
		Processor ws = new Processor();
		ws.setBase(new File("testresources"));
		Jar jar = new Jar(new File("testresources/org.component.test.jar"));
		
		try (BundleReportGenerator rg = new BundleReportGenerator(jar, ws)) {
			rg.addClose(jar);
			
			a.clear();
			a.add(ws.getBase().getCanonicalPath() + File.separator + "test.json");
			ws.setProperty(Constants.REPORT_BUNDLE, "test.json");
			GeneratorAsserts.verify(rg, 1, a, 1);
		}
		
		ws = new Processor();
		ws.setBase(new File("testresources"));
		ws.addBasicPlugin(new XsltTransformerPlugin());
		jar = new Jar(new File("testresources/org.component.test.jar"));
		
		try (BundleReportGenerator rg = new BundleReportGenerator(jar, ws)) {
			rg.addClose(jar);
			
			a.clear();
			a.add(ws.getBase().getCanonicalPath() + File.separator + "test.json");
			ws.setProperty(Constants.REPORT_BUNDLE, "test.json");
			GeneratorAsserts.verify(rg, 1, a, 1);
			
			a.clear();
			a.add(ws.getBase().getCanonicalPath() + File.separator + "test2.json");
			a.add(ws.getBase().getCanonicalPath() + File.separator + "cool.xml");
			ws.setProperty(Constants.REPORT_MODEL_BUNDLE, "imports=@OSGI-INF/org.test.project1.xml,locales='en_US'");
			ws.setProperty(Constants.REPORT_BUNDLE, "cool.xml,test2.json;template:=xslt.xslt;param1=test");
			ws.setProperty(Constants.BUNDLE_SYMBOLICNAME, "org.component.test");
			GeneratorAsserts.verify(rg, 3, a, 2);
		}
	}
}
