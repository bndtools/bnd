package test.component_extra;

import static aQute.bnd.test.BndTestCase.assertOk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

import org.junit.jupiter.api.Test;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

/*
 * Placed in a new package to avoid breaking lots of existing tests with the additional packages
 * we need to import.
 */
public class DSAnnotationExtrasTest {

	@Component(configurationPid = "x" // force DS 1.3
	)
	public static class DS13reference_cardinalities {

		@Reference
		void setLogService(LogService log) {}

		@Reference(cardinality = ReferenceCardinality.OPTIONAL)
		void setEventAdmin(EventAdmin ea) {}

		@Reference(cardinality = ReferenceCardinality.MULTIPLE)
		void setConfigAdmin(ConfigurationAdmin cm) {}

		@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE)
		void setMetatype(MetaTypeService metatype) {}
	}

	@Test
	public void testRequireCapabilityCardinality() throws Exception {
		Builder b = new Builder();
		b.setProperty(Constants.DSANNOTATIONS, DS13reference_cardinalities.class.getName());
		b.setProperty("Private-Package", DS13reference_cardinalities.class.getPackage()
			.getName());
		b.addClasspath(new File("bin_test"));

		Jar jar = b.build();
		assertOk(b);

		Attributes attrs = jar.getManifest()
			.getMainAttributes();

		Parameters requires = new Parameters(attrs.getValue(Constants.REQUIRE_CAPABILITY));
		List<Attrs> serviceRequires = getAll(requires, "osgi.service");
		assertEquals(4, serviceRequires.size());
		checkServiceRequirements(serviceRequires, LogService.class, false, false);
		checkServiceRequirements(serviceRequires, EventAdmin.class, true, false);
		checkServiceRequirements(serviceRequires, ConfigurationAdmin.class, true, true);
		checkServiceRequirements(serviceRequires, MetaTypeService.class, false, true);
	}

	private void checkServiceRequirements(List<Attrs> serviceReqs, Class<?> clazz, boolean optional, boolean multiple) {
		String filter = String.format("(objectClass=%s)", clazz.getName());
		boolean found = false;
		for (Attrs serviceReq : serviceReqs) {
			if (filter.equals(serviceReq.get("filter:"))) {
				assertEquals("active", serviceReq.get("effective:"), "missing effective:=active on " + clazz.getName());
				if (optional)
					assertEquals("optional", serviceReq.get("resolution:"),
						"missing resolution:=optional on " + clazz.getName());
				if (multiple)
					assertEquals("multiple", serviceReq.get("cardinality:"),
						"missing cardinality:=multiple on " + clazz.getName());
				found = true;
			}
		}
		assertTrue(found, "objectClass not found: " + clazz.getName());
	}

	private static List<Attrs> getAll(Parameters p, String key) {
		List<Attrs> l = new ArrayList<>();
		for (; p.containsKey(key); key += aQute.bnd.osgi.Constants.DUPLICATE_MARKER) {
			l.add(p.get(key));
		}
		return l;
	}

}
