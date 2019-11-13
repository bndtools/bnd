package test.apiguardian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import aQute.bnd.apiguardian.api.API.Status;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class APIGuardianAnnotationsTest {

	@Test
	public void exportsPackages() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-includepackage", "test.apiguardian.api_a.*");
			b.setProperty("-export-apiguardian", "*");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Attributes a = getAttr(jar);

			assertThat(a.getValue("Export-Package")).isNotNull();

			SoftAssertions softly = new SoftAssertions();

			Parameters parameters = new Parameters(a.getValue("Export-Package"));

			softly.assertThat(parameters) //
				.containsKey("test.apiguardian.api_a")
				.containsKey("test.apiguardian.api_a.sub")
				.containsKey("test.apiguardian.api_a.sub.sub")
				.extractingFromEntries(e -> e.getValue())
				.allMatch(it -> ((Attrs) it).get("version")
					.equals("0.0.0"));

			softly.assertThat(parameters.get("test.apiguardian.api_a"))
				.containsEntry("status", Status.STABLE.name());
			softly.assertThat(parameters.get("test.apiguardian.api_a.sub"))
				.containsEntry("status", Status.INTERNAL.name());
			softly.assertThat(parameters.get("test.apiguardian.api_a.sub.sub"))
				.containsEntry("status",
				Status.EXPERIMENTAL.name());

			softly.assertAll();
		}
	}

	@Test
	public void exportsPackagesWithAttributes() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-includepackage", "test.apiguardian.api_a.*");
			b.setProperty("-export-apiguardian", "*;version=1.1.2");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Attributes a = getAttr(jar);

			assertThat(a.getValue("Export-Package")).isNotNull();

			SoftAssertions softly = new SoftAssertions();

			Parameters parameters = new Parameters(a.getValue("Export-Package"));

			softly.assertThat(parameters) //
				.containsKey("test.apiguardian.api_a")
				.containsKey("test.apiguardian.api_a.sub")
				.containsKey("test.apiguardian.api_a.sub.sub")
				.extractingFromEntries(e -> e.getValue())
				.allMatch(it -> ((Attrs) it).get("version")
					.equals("1.1.2"));

			softly.assertThat(parameters.get("test.apiguardian.api_a"))
				.containsEntry("status", Status.STABLE.name());
			softly.assertThat(parameters.get("test.apiguardian.api_a.sub"))
				.containsEntry("status", Status.INTERNAL.name());
			softly.assertThat(parameters.get("test.apiguardian.api_a.sub.sub"))
				.containsEntry("status", Status.EXPERIMENTAL.name());

			softly.assertAll();
		}
	}

	@Test
	public void exportsTargetedPackagesWithAttributes() throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-includepackage", "test.apiguardian.api_a.*");
			b.setProperty("-export-apiguardian", "test.apiguardian.api_a.sub.*;version=1.3.2");
			b.addClasspath(new File("bin_test"));
			Jar jar = b.build();

			if (!b.check())
				fail();

			Attributes a = getAttr(jar);

			assertThat(a.getValue("Export-Package")).isNotNull();

			SoftAssertions softly = new SoftAssertions();

			Parameters parameters = new Parameters(a.getValue("Export-Package"));

			softly.assertThat(new Parameters(a.getValue("Export-Package"))) //
				.doesNotContainKey("test.apiguardian.api_a")
				.containsKey("test.apiguardian.api_a.sub")
				.containsKey("test.apiguardian.api_a.sub.sub")
				.extractingFromEntries(e -> e.getValue())
				.allMatch(it -> ((Attrs) it).get("version")
					.equals("1.3.2"));

			softly.assertThat(parameters.get("test.apiguardian.api_a.sub"))
				.containsEntry("status", Status.INTERNAL.name());
			softly.assertThat(parameters.get("test.apiguardian.api_a.sub.sub"))
				.containsEntry("status", Status.EXPERIMENTAL.name());

			softly.assertAll();
		}
	}

	private Attributes getAttr(Jar jar) throws Exception {
		Manifest m = jar.getManifest();
		return m.getMainAttributes();
	}

}
