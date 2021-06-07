package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Packages;

public class ReferencedAnnotationTest {

	@Test
	public void testReferenced() throws Exception {
		try (Builder builder = new Builder()) {
			builder.addClasspath(new File("bin_test"));

			builder.setIncludePackage("test.referenced.annotation.testReferenced");
			builder.build();
			builder.check();
			Packages imports = builder.getImports();
			assertThat(imports).containsOnlyKeys(builder.getPackageRef("test.export.annotation.testCalculated"),
				builder.getPackageRef("test.export.annotation.testConsumer"),
				builder.getPackageRef("test.export.annotation.testProvider"));
		}
	}

}
