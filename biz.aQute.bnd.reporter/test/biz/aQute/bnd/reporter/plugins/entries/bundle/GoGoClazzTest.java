package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.InputStream;
import java.util.Optional;

import org.junit.Assert;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import junit.framework.TestCase;

public class GoGoClazzTest extends TestCase {

	public void testJar() throws Exception {

		final Jar jar = new Jar("jar", "testresources/gogoEntry/source.jar");
		Resource resource = jar.getResource("gogo/ClassA.class");
		Clazz clazz = new Clazz(new Analyzer(), null, null);
		clazz.parseClassFile(resource.openInputStream());

		clazz.methods()
			.filter(m -> "methodB".equals(m.getName()))
			.forEach(md -> {

				// all methos has more than one param
				Assert.assertNotEquals(0, md.getParameters().length);
			});

	}

	public void testThis() throws Exception {

		InputStream in = GoGoClazzTest.class.getResourceAsStream(GoGoClazzTest.class.getSimpleName() + ".class");
		Clazz clazz = new Clazz(new Analyzer(), null, null);
		clazz.parseClassFile(in);

		Optional<MethodDef> omdWorks = clazz.methods()
			.filter(m -> "methodWorks".equals(m.getName()))
			.findFirst();

		MethodDef mdWorks = omdWorks.get();
		Assert.assertEquals(1, mdWorks.getParameters().length);

	}

	public void methodWorks(String parameterField) {

	}

}
