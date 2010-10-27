package test;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import junit.framework.*;

import org.osgi.framework.*;
import org.osgi.service.component.*;
import org.osgi.service.event.*;
import org.osgi.service.log.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import aQute.bnd.annotation.component.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Constants;

public class ComponentTest extends TestCase {


	/**
	 * Test an attribute on an annotation
	 */
	
    @Component(name="annotated")
    static class Annotated {

        @Reference
        protected void setLog(LogService log) {
        }

    }
    public void testAnnotatedWithAttribute() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*NoUnbind;log=org.osgi.service.log.LogService");
        b.setProperty("Private-Package", "test");
        Jar jar = b.build();
        Manifest manifest = jar.getManifest();
        String sc = manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT);
        assertFalse( sc.contains(";"));
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

    }
    
	
	/**
	 * A non-FQN entry but we demand no annotations, should generate an
	 * error and no component
	 */
	
    public void testNonFQNAndNoAnnotations() throws Exception {
        Builder b = new Builder();
        b.setProperty("Include-Resource", "org/osgi/impl/service/coordinator/AnnotationWithJSR14.class=jar/AnnotationWithJSR14.jclass");
        b.setProperty("Service-Component", "*;" + Constants.NOANNOTATIONS+"=true");
        b.setProperty("-resourceonly", "true");
        Jar jar = b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(1, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Manifest manifest = jar.getManifest();
        String component = manifest.getMainAttributes().getValue("Service-Component");
        System.out.println(component);
        assertNull( component );
    }

	
	/**
	 * Imported default package because JSR14 seems to do
	 * something weird with annotations.
	 * 
	 * @throws Exception
	 */
    public void testJSR14ComponentAnnotations() throws Exception {
        Builder b = new Builder();
        b.setProperty("Include-Resource", "org/osgi/impl/service/coordinator/AnnotationWithJSR14.class=jar/AnnotationWithJSR14.jclass");
        b.setProperty("Service-Component", "*");
        b.setProperty("-resourceonly", "true");
        Jar jar = b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(1, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Manifest manifest = jar.getManifest();
        String component = manifest.getMainAttributes().getValue("Service-Component");
        System.out.println(component);
        assertNull( component );
    }
    
	
	
    @Component(name="nounbind")
    static class NoUnbind {

        @Reference
        protected void setLog(LogService log) {
        }

    }
    
    
    
    public void testNoUnbind() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*NoUnbind");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "nounbind");
        XPath xp = XPathFactory.newInstance().newXPath();
        assertEquals("setLog", xp.evaluate("component/reference/@bind", doc));
        assertEquals("", xp.evaluate("component/reference/@unbind", doc));
    }
    
    @Component(name="explicitunbind")
    static class ExplicitUnbind {

        @Reference(unbind="killLog")
        protected void setLog(LogService log) {
        }

    }
    
    
    
    public void testExplicitUnbind() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*ExplicitUnbind");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(1, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "explicitunbind");
        XPath xp = XPathFactory.newInstance().newXPath();
        assertEquals("setLog", xp.evaluate("component/reference/@bind", doc));
        assertEquals("killLog", xp.evaluate("component/reference/@unbind", doc));
    }
    
    /**
     * Test to see if we get a namespace when we use 1.1 semantics on the
     * activate and deactivate
     */

    @Component(name = "ncomp", provide = {})
    static class NewActivateVersion {

        @Activate
        protected void activate() {
        }

    }

    @Component(name = "ndcomp", provide = {})
    static class NewDeActivateVersion {
        @Deactivate
        protected void deactivate() {
        }

    }

    @Component(name = "nbcomp", provide = {})
    static class NewBindVersion {

        @Reference
        protected void bind(ServiceReference ref, Map<String,Object> map) {
        }

    }

    public void testNewVersion() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*Version");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "ncomp");
        assertNotNull(doc.getElementsByTagName("component").item(0)
                .getNamespaceURI());
        doc = doc(b, "ndcomp");
        assertNotNull(doc.getElementsByTagName("component").item(0)
                .getNamespaceURI());
        doc = doc(b, "nbcomp");
        assertNotNull(doc.getElementsByTagName("component").item(0)
                .getNamespaceURI());
    }

    /**
     * Test the same bind method names
     */

    @Component(name = "cpcomp")
    static class SameRefName {

        @Reference
        protected void bind(LogService log) {

        }

        @Reference
        protected void bind(EventAdmin event) {

        }
    }

    public void testSameRefName() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.SameRefName");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(1, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        // Document doc = doc(b, "cpcomp");
    }

    /**
     * Test the configuration policy
     */

    @Component(name = "cpcomp", configurationPolicy = ConfigurationPolicy.require, provide = {
            Serializable.class, EventAdmin.class })
    static class ConfigurationPolicyTest {

    }

    public void testConfigurationPolicy() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.ConfigurationPolicyTest");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "cpcomp");
        assertEquals("java.io.Serializable", doc
                .getElementsByTagName("provide").item(0).getAttributes()
                .getNamedItem("interface").getTextContent());
        assertEquals("org.osgi.service.event.EventAdmin", doc
                .getElementsByTagName("provide").item(1).getAttributes()
                .getNamedItem("interface").getTextContent());

    }

    /**
     * Test to see if we use defaults we get the old version of the namespace
     */

    @Component(name = "vcomp", provide = {})
    static class OldVersion {

        @Activate
        protected void activate(ComponentContext cc) {
        }

        @Deactivate
        protected void deactivate(ComponentContext cc) {
        }

        @Reference
        protected void bindLog(LogService log) {

        }
    }

    public void testOldVersion() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.OldVersion");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "vcomp");
        assertEquals(null, doc.getElementsByTagName("component").item(0)
                .getNamespaceURI());
    }

    /**
     * Test if an activate/deactivate method that has wrong prototype
     */

    @Component(name = "wacomp", provide = {})
    static class ActivateWithWrongArguments {

        @Activate
        // Is not allowed, must give an error
        protected void whatever(String x) {
        }

    }

    public void testActivateWithActivateWithWrongArguments() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.ActivateWithWrongArguments");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(1, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "wacomp");
        assertAttribute(doc, "whatever", "component", "activate");
        assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", doc
                .getElementsByTagName("component").item(0).getNamespaceURI());
    }

    /**
     * Test if an activate/deactivate method can have multiple args
     */

    @Component(name = "amcomp", provide = {})
    static class ActivateWithMultipleArguments {

        @Activate
        protected void whatever(Map<?, ?> map, ComponentContext cc,
                BundleContext bc, Map<?, ?> x) {
        }

    }

    public void testActivateWithMultipleArguments() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.ActivateWithMultipleArguments");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "amcomp");
        assertAttribute(doc, "whatever", "component", "activate");
        assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", doc
                .getElementsByTagName("component").item(0).getNamespaceURI());
    }

    /**
     * Test components with references that have multiple arguments.
     */
    @Component(name = "mcomp", provide = {})
    static class MultipleArguments {

        @Reference
        protected void bindWithMap(LogService log, Map<?, ?> map) {
        }

    }

    @Component(name = "rcomp", provide = {})
    static class ReferenceArgument {

        @Reference(service = LogService.class)
        protected void bindReference(ServiceReference ref) {
        }

    }

    public void testMultipleArguments() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component",
                "*.(MultipleArguments|ReferenceArgument)");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "mcomp");
        assertAttribute(doc, "bindWithMap", "reference", 0, "bind");
        assertAttribute(doc, "org.osgi.service.log.LogService", "reference", 0,
                "interface");

        doc = doc(b, "rcomp");
        assertAttribute(doc, "bindReference", "reference", 0, "bind");
        assertAttribute(doc, "org.osgi.service.log.LogService", "reference", 0,
                "interface");
    }

    /**
     * Test components with weird bind methods.
     */
    @Component(name = "xcomp", provide = {})
    static class TypeVersusDetailed {

        @Reference(type = '*')
        protected void bind(LogService log) {
        }

        @Reference(multiple = true, optional = true, dynamic = true)
        protected void bind2(LogService log) {
        }
    }

    public void testTypeVersusDetailed() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.TypeVersusDetailed");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "xcomp");
        assertAttribute(doc, "bind", "reference", 0, "bind");
        assertAttribute(doc, "dynamic", "reference", 0, "policy");
        assertAttribute(doc, "0..n", "reference", 0, "cardinality");
        assertAttribute(doc, "bind2", "reference", 1, "bind");
        assertAttribute(doc, "dynamic", "reference", 1, "policy");
        assertAttribute(doc, "0..n", "reference", 1, "cardinality");
    }

    /**
     * Test components with weird bind methods.
     */
    @Component(name = "acomp", provide = {})
    static class MyComponent4 {
        @Activate
        protected void xyz() {
        }
    }

    public void testAnnotationsNamespaceVersion() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.MyComponent4");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "acomp");
        System.out.println(doc.getDocumentElement().getNamespaceURI());
    }

    /**
     * Test components with weird bind methods.
     */
    @Component(name = "acomp", configurationPolicy = ConfigurationPolicy.require)
    static class MyComponent2 {
        @Reference
        protected void addLogMultiple(LogService log) {

        }
    }

    public void testAnnotationsStrangeBindMethods() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.MyComponent2");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "acomp");
        XPath xp = XPathFactory.newInstance().newXPath();
        assertEquals("addLogMultiple", xp.evaluate("//@bind", doc));
        assertEquals("", xp.evaluate("//@unbind", doc));
        
        assertAttribute(doc, "logMultiple", "reference", 0, "name");
        assertAttribute(doc, "addLogMultiple", "reference", 0, "bind");
        
    }

    /**
     * Test setting the unbind method
     */
    @Component(name = "acomp", configurationPolicy = ConfigurationPolicy.ignore)
    static class MyComponent3 {
        @Reference(unbind = "destroyX")
        protected void putX(LogService log) {

        }
    }

    public void testAnnotationsSettingUnbind() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.MyComponent3");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(1, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "acomp");
        assertAttribute(doc, "putx", "reference", 0, "name");
        assertAttribute(doc, "putX", "reference", 0, "bind");
        assertAttribute(doc, "destroyX", "reference", 0, "unbind");
    }

    /**
     * Test some more components
     * 
     * @author aqute
     * 
     */
    @Component(name = "acomp", enabled = true, factory = "abc", immediate = false, provide = LogService.class, servicefactory = true, configurationPolicy = ConfigurationPolicy.optional)
    static class MyComponent implements Serializable {
        private static final long serialVersionUID = 1L;
        LogService                log;

        @Activate
        protected void activatex() {
        }

        @Deactivate
        protected void deactivatex() {
        }

        @Modified
        protected void modifiedx() {
        }

        @Reference(type = '~', target = "(abc=3)")
        protected void setLog(LogService log) {
            this.log = log;
        }

        protected void unsetLog(LogService log) {
            this.log = null;
        }

    }

    public void testAnnotations() throws Exception {
        Builder b = new Builder();
        b.setClasspath(new File[] { new File("bin") });
        b.setProperty("Service-Component", "*.MyComponent");
        b.setProperty("Private-Package", "test");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Document doc = doc(b, "acomp");
        assertAttribute(doc, "test.ComponentTest.MyComponent",
                "implementation", "class");
        assertAttribute(doc, "acomp", "component", "name");
        assertAttribute(doc, "abc", "component", "factory");
        assertAttribute(doc, "true", "service", "servicefactory");
        assertAttribute(doc, "activatex", "component", "activate");
        assertAttribute(doc, "modifiedx", "component", "modified");
        assertAttribute(doc, "deactivatex", "component", "deactivate");
        assertAttribute(doc, "org.osgi.service.log.LogService", "provide",
                "interface");
        assertAttribute(doc, "(abc=3)", "reference", "target");
        assertAttribute(doc, "setLog", "reference", "bind");
        assertAttribute(doc, "unsetLog", "reference", "unbind");
        assertAttribute(doc, "0..1", "reference", "cardinality");
    }

    public void assertAttribute(Document doc, String value, String tag,
            String attribute) {
        assertAttribute(doc, value, tag, 0, attribute);
    }

    public void assertAttribute(Document doc, String value, String tag,
            int index, String attribute) {
        assertEquals(value, doc.getElementsByTagName(tag).item(index)
                .getAttributes().getNamedItem(attribute).getNodeValue());
    }

    Document doc(Builder b, String name) throws Exception {
        Jar jar = b.getJar();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(jar.getResource(
                "OSGI-INF/" + name + ".xml").openInputStream()));
        return doc;
    }

    public void testV1_1Directives() throws Exception {
        Element component = setup("test.activator.Activator;factory:=blabla;immediate:=true;enabled:=false;configuration-policy:=optional;activate:=start;deactivate:=stop;modified:=modified");
        assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component
                .getNamespaceURI());
        assertEquals("blabla", component.getAttribute("factory"));
        assertEquals("false", component.getAttribute("enabled"));
        assertEquals("optional", component.getAttribute("configuration-policy"));
        assertEquals("start", component.getAttribute("activate"));
        assertEquals("stop", component.getAttribute("deactivate"));
        assertEquals("modified", component.getAttribute("modified"));

    }

    public void testNoNamespace() throws Exception {
        Element component = setup("test.activator.Activator");
        assertEquals(null, component.getNamespaceURI());
    }

    public void testAutoNamespace() throws Exception {
        Element component = setup("test.activator.Activator;activate:='start';deactivate:='stop'");
        assertEquals("http://www.osgi.org/xmlns/scr/v1.1.0", component
                .getNamespaceURI());

    }

    public void testCustomNamespace() throws Exception {
        Element component = setup("test.activator.Activator;version:=2");
        assertEquals("http://www.osgi.org/xmlns/scr/v2.0.0", component
                .getNamespaceURI());

    }

    Element setup(String header) throws Exception {
        Builder b = new Builder();
        b.setProperty(Analyzer.SERVICE_COMPONENT, header);
        b
                .setClasspath(new File[] { new File("bin"),
                        new File("jar/osgi.jar") });
        b.setProperty("Private-Package",
                "test.activator, org.osgi.service.http.*");
        b.build();

        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        print(b.getJar().getResource("OSGI-INF/test.activator.Activator.xml"),
                System.out);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(b.getJar().getResource(
                "OSGI-INF/test.activator.Activator.xml").openInputStream()));

        return doc.getDocumentElement();
    }

    private void print(Resource resource, OutputStream out) throws IOException {
        InputStream in = resource.openInputStream();
        try {
            byte[] buffer = new byte[1024];
            int size = in.read(buffer);
            while (size > 0) {
                out.write(buffer, 0, size);
                size = in.read(buffer);
            }
            out.flush();
        } finally {
            in.close();
        }
    }

    /*
     * public void testWildcards() throws Exception { Builder b = new Builder();
     * b .setProperty(Analyzer.SERVICE_COMPONENT, "test/component/*.xml");
     * b.setProperty("-resourceonly", "true"); b.setProperty("Include-Resource",
     * "test/component=test/component"); Jar jar = b.build();
     * System.out.println(b.getErrors()); System.out.println(b.getWarnings());
     * assertEquals(0, b.getErrors().size()); assertEquals(0,
     * b.getWarnings().size()); }
     */
    public void testImplementation() throws Exception {
        Builder b = new Builder();
        b
                .setProperty(
                        Analyzer.SERVICE_COMPONENT,
                        "silly.name;implementation:=test.activator.Activator;provide:=java.io.Serialization;servicefactory:=true");
        b
                .setClasspath(new File[] { new File("bin"),
                        new File("jar/osgi.jar") });
        b.setProperty("Private-Package", "test.activator");
        b.build();
        System.out.println(b.getErrors());
        System.out.println(b.getWarnings());
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Jar jar = b.getJar();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(jar.getResource(
                "OSGI-INF/silly.name.xml").openInputStream()));

        assertEquals("test.activator.Activator", doc.getElementsByTagName(
                "implementation").item(0).getAttributes().getNamedItem("class")
                .getNodeValue());
        assertEquals("true", doc.getElementsByTagName("service").item(0)
                .getAttributes().getNamedItem("servicefactory").getNodeValue());
    }

    /**
     * Standard activator with reference to http.
     * 
     * @throws Exception
     */
    public void testProperties() throws Exception {
        java.util.Properties p = new Properties();
        p.put(Analyzer.EXPORT_PACKAGE, "test.activator,org.osgi.service.http");
        p.put(Analyzer.IMPORT_PACKAGE, "*");
        p.put(Analyzer.SERVICE_COMPONENT,
                "test.activator.Activator;properties:=\"a=3|4,b=1|2|3\"");
        Builder b = new Builder();
        b
                .setClasspath(new File[] { new File("bin"),
                        new File("jar/osgi.jar") });
        b.setProperties(p);
        b.build();
        assertEquals(0, b.getErrors().size());
        assertEquals(0, b.getWarnings().size());

        Jar jar = b.getJar();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(jar.getResource(
                "OSGI-INF/test.activator.Activator.xml").openInputStream()));

        NodeList l = doc.getElementsByTagName("property");
        assertEquals(2, l.getLength());
        Node n = l.item(0);
        System.out.println(n.getFirstChild().getNodeValue());
        assertEquals("3\n4", l.item(0).getFirstChild().getNodeValue().trim());
        assertEquals("1\n2\n3", l.item(1).getFirstChild().getNodeValue().trim());

        assertEquals("test.activator.Activator", doc.getElementsByTagName(
                "implementation").item(0).getAttributes().getNamedItem("class")
                .getNodeValue());
        // assertEquals("test.activator.Activator", xp.evaluate(
        // "/component/implementation/@class", doc));
        // assertEquals("org.osgi.service.http.HttpService", xp.evaluate(
        // "/component/reference[@name='http']/@interface", doc));
        // assertEquals("setHttp", xp.evaluate(
        // "/component/reference[@name='http']/@bind", doc));
        // assertEquals("unsetHttp", xp.evaluate(
        // "/component/reference[@name='http']/@unbind", doc));
        // assertEquals("", xp.evaluate(
        // "/component/reference[@name='http']/@target", doc));
    }

    /**
     * Check if all the directives work
     * 
     * @throws Exception
     */
    public void testUnknownDirective() throws Exception {
        java.util.Properties p = new Properties();
        p.put(Analyzer.EXPORT_PACKAGE, "test.activator,org.osgi.service.http");
        p.put(Analyzer.IMPORT_PACKAGE, "*");
        p.put(Analyzer.SERVICE_COMPONENT,
                "test.activator.Activator;provides:=true");
        Builder b = new Builder();
        b
                .setClasspath(new File[] { new File("bin"),
                        new File("jar/osgi.jar") });
        b.setProperties(p);
        b.build();
        assertEquals(1, b.getErrors().size());
        assertTrue(((String) b.getErrors().get(0))
                .indexOf("Unrecognized directive") >= 0);
        assertEquals(0, b.getWarnings().size());
    }

    /**
     * Check if all the directives work
     * 
     * @throws Exception
     */
    public void testDirectives() throws Exception {
//        Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService;dynamic:=http;optional:=http;provide:=test.activator.Activator; multiple:=http");

        // assertEquals("test.activator.Activator", xp.evaluate(
        // "/component/implementation/@class", doc));
        // assertEquals("org.osgi.service.http.HttpService", xp.evaluate(
        // "/component/reference[@name='http']/@interface", doc));
        // assertEquals("setHttp", xp.evaluate(
        // "/component/reference[@name='http']/@bind", doc));
        // assertEquals("unsetHttp", xp.evaluate(
        // "/component/reference[@name='http']/@unbind", doc));
        // assertEquals("0..n", xp.evaluate(
        // "/component/reference[@name='http']/@cardinality", doc));
        // assertEquals("dynamic", xp.evaluate(
        // "/component/reference[@name='http']/@policy", doc));
        // assertEquals("test.activator.Activator", xp.evaluate(
        // "/component/service/provide/@interface", doc));
    }

    /**
     * Check if a bad filter on a service component causes an error.
     * 
     * @throws Exception
     */
    public void testBadFilter() throws Exception {
        // java.util.Properties p = new Properties();
        // p.put(Analyzer.EXPORT_PACKAGE,
        // "test.activator,org.osgi.service.http");
        // p.put(Analyzer.IMPORT_PACKAGE, "*");
        // p
        // .put(
        // Analyzer.SERVICE_COMPONENT,
        // "test.activator.Activator;http=\"org.osgi.service.http.HttpService(|p=1)(p=2))\"");
        // Builder b = new Builder();
        // b
        // .setClasspath(new File[] { new File("bin"),
        // new File("jar/osgi.jar") });
        // b.setProperties(p);
        // b.build();
        // assertEquals(1, b.getErrors().size());
        // assertTrue(((String) b.getErrors().get(0))
        // .indexOf("is not a correct filter") >= 0);
        // assertEquals(0, b.getWarnings().size());
    }

    /**
     * Check if we can set a target filter
     * 
     * @throws Exception
     */
    public void testFilter() throws Exception {
        Element component = setup("test.activator.Activator;http=\"org.osgi.service.http.HttpService(|(p=1)(p=2))\"");
        Element implementation = (Element) component.getElementsByTagName(
                "implementation").item(0);
        assertEquals(null, implementation.getNamespaceURI());
        assertEquals("test.activator.Activator", implementation
                .getAttribute("class"));

        Element reference = (Element) component.getElementsByTagName(
                "reference").item(0);
        assertEquals("org.osgi.service.http.HttpService", reference
                .getAttribute("interface"));
        assertEquals("setHttp", reference.getAttribute("bind"));
        assertEquals("unsetHttp", reference.getAttribute("unbind"));
        assertEquals("(|(p=1)(p=2))", reference.getAttribute("target"));
    }

    /**
     * Standard activator with reference to http.
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception {
        Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService?");
        Element implementation = (Element) component.getElementsByTagName(
                "implementation").item(0);
        assertEquals(null, implementation.getNamespaceURI());
        assertEquals("test.activator.Activator", implementation
                .getAttribute("class"));

        Element reference = (Element) component.getElementsByTagName(
                "reference").item(0);
        assertEquals("org.osgi.service.http.HttpService", reference
                .getAttribute("interface"));
        assertEquals("setHttp", reference.getAttribute("bind"));
        assertEquals("unsetHttp", reference.getAttribute("unbind"));
        assertEquals("", reference.getAttribute("target"));
        assertEquals("0..1", reference.getAttribute("cardinality"));
        assertEquals("dynamic", reference.getAttribute("policy"));
    }

    /**
     * Standard activator with reference to http.
     * 
     * @throws Exception
     */
    public void testQuestion() throws Exception {
        Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService?");
        Element reference = (Element) component.getElementsByTagName(
                "reference").item(0);
        assertEquals("0..1", reference.getAttribute("cardinality"));
        assertEquals("dynamic", reference.getAttribute("policy"));
    }

    public void testStar() throws Exception {
        Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService*");
        Element reference = (Element) component.getElementsByTagName(
                "reference").item(0);
        assertEquals("0..n", reference.getAttribute("cardinality"));
        assertEquals("dynamic", reference.getAttribute("policy"));
    }

    public void testPlus() throws Exception {
        Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService+");
        Element reference = (Element) component.getElementsByTagName(
                "reference").item(0);
        assertEquals("1..n", reference.getAttribute("cardinality"));
        assertEquals("dynamic", reference.getAttribute("policy"));
    }

    public void testTilde() throws Exception {
        Element component = setup("test.activator.Activator;http=org.osgi.service.http.HttpService~");
        Element reference = (Element) component.getElementsByTagName(
                "reference").item(0);
        assertEquals("0..1", reference.getAttribute("cardinality"));
        assertEquals("", reference.getAttribute("policy"));
    }
}
