package aQute.bnd.osgi;

import java.io.InputStream;
import java.util.Optional;

import org.junit.Assert;

import aQute.bnd.osgi.Clazz.MethodDef;
import junit.framework.TestCase;

public class ClazzTest extends TestCase {

	public void testMethodParams() throws Exception {

		InputStream in = ClazzTest.class.getResourceAsStream(ClazzTest.class.getSimpleName() + ".class");
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
