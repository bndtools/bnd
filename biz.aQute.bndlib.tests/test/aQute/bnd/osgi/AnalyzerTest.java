package aQute.bnd.osgi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.lib.io.IO;

public class AnalyzerTest {

	@Test
	public void testCopy() throws Exception {
		try (Builder b = new Builder()) {
			b.addClasspath(IO.getFile("jar/osgi.jar"));
			b.setProperty("-includepackage", "org.osgi.service.condpermadmin");
			b.build();
			assertThat(b.check()).isTrue();
			Analyzer a = Analyzer.copy(b);
			compare("classspace", a.getClassspace(), b.getClassspace());
			compare("imports", a.getImports(), b.getImports());
			compare("exports", a.getExports(), b.getExports());
			compare("referred", a.getReferred(), b.getReferred());
			compare("contained", a.getContained(), b.getContained());
			compare("classes", a.getClasses(), b.getClasses());

			Jar ja = a.getJar();
			Jar jb = b.getJar();
			compare("resources", ja.getResources(), jb.getResources());
			compare("compression", ja.hasCompression(), jb.hasCompression());
			compare("bsn", ja.getBsn(), jb.getBsn());
			compare("manifestName", ja.getManifestName(), jb.getManifestName());
			compare("directories", ja.getDirectories(), jb.getDirectories());
			compare("module name", ja.getModuleName(), jb.getModuleName());
			compare("module version", ja.getModuleVersion(), jb.getModuleVersion());
			compare("manifest", ja.getManifest(), jb.getManifest());
			compare("version", ja.getVersion(), jb.getVersion());
			compare("lastModifiedReason", ja.lastModifiedReason(), jb.lastModifiedReason());
			compare("packages", ja.getPackages(), jb.getPackages());
		}
	}

	private void compare(String descr, Enum<?> a, Enum<?> b) {
		assertThat(a).describedAs(descr)
			.isEqualTo(b);
	}

	private void compare(String descr, String a, String b) {
		assertThat(a).describedAs(descr)
			.isEqualTo(b);
	}

	private void compare(String descr, Object a, Object b) {
		assertTrue("must be different " + descr, a != b);
		assertThat(a).describedAs(descr)
			.isEqualTo(b);
	}

	private void compare(String descr, Map<?, ?> a, Map<?, ?> b) {
		assertTrue("must be different " + descr, a != b);
		assertThat(a).describedAs(descr)
			.isEqualTo(b);
	}

	private void compare(String descr, Collection<?> a, Collection<?> b) {
		assertTrue("must be different", a != b);
		assertThat(a).describedAs(descr)
			.isEqualTo(b);
	}

	@Test
	public void testdoNameSection() throws Exception {
		try (Analyzer a = new Analyzer()) {
			a.doNameSection(null, "@");
			a.doNameSection(null, "@@");
			a.doNameSection(null, "@foo@bar@");
		}
	}

}
