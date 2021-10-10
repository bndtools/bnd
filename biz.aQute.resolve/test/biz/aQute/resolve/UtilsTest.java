package biz.aQute.resolve;

import java.util.List;

import org.junit.jupiter.api.Test;

import biz.aQute.resolve.Utils.ResolveTrace;

public class UtilsTest {
	public static String msg1 = "org.osgi.service.resolver.ResolutionException: Unable to resolve"
		+ " <<INITIAL>> version=null: missing requirement Require[osgi.identity]{}"
		+ "{filter=(&(osgi.identity=org.apache.felix.gogo.shell)(&(version>=0.0.0)"
		+ "(!(version>=1.0.0))))} [caused by: Unable to resolve org.apache.felix.gogo.shell "
		+ "version=0.10.0: missing requirement Require[osgi.wiring.package]{}"
		+ "{filter=(osgi.wiring.package=org.osgi.framework)}]";

	@Test
	public void testMessage() {
		List<ResolveTrace> trace = Utils.parseException(msg1);
		// assertEquals(2, trace.size());
	}

}
