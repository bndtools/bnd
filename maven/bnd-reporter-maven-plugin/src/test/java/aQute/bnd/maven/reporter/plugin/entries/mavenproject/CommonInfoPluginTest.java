package aQute.bnd.maven.reporter.plugin.entries.mavenproject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;

import aQute.bnd.maven.reporter.plugin.MavenProjectWrapper;
import biz.aQute.bnd.reporter.manifest.dto.CommonInfoDTO;
import junit.framework.TestCase;

public class CommonInfoPluginTest extends TestCase {

	public void testMinimal() throws Exception {
		final CommonInfoPlugin plugin = new CommonInfoPlugin();

		MavenProjectWrapper p = getProject();
		p.getProject()
			.setName("default");
		p.getProject()
			.setVersion(null);

		final CommonInfoDTO infoDto = plugin.extract(p, Locale.forLanguageTag("und"));

		assertNull(infoDto.copyright);
		assertNull(infoDto.description);
		assertNull(infoDto.docURL);
		assertEquals("default", infoDto.name);
		assertNull(infoDto.updateLocation);
		assertNull(infoDto.vendor);
		assertNull(infoDto.contactAddress);
		assertNull(infoDto.developers);
		assertNull(infoDto.icons);
		assertNull(infoDto.licenses);
		assertNull(infoDto.scm);
		assertNull(infoDto.version);
	}

	public void testFull() throws Exception {
		final CommonInfoPlugin plugin = new CommonInfoPlugin();

		MavenProjectWrapper p = getProject();

		p.getProject()
			.setDescription("test2");
		p.getProject()
			.setUrl("test3");
		p.getProject()
			.setName("test4");

		DistributionManagement di = new DistributionManagement();
		di.setDownloadUrl("test5");
		p.getProject()
			.setDistributionManagement(di);

		Organization o = new Organization();
		o.setName("test6");
		o.setUrl("test7");
		p.getProject()
			.setOrganization(o);

		Developer d = new Developer();
		d.setId("test8");
		p.getProject()
			.addDeveloper(d);

		License l = new License();
		l.setName("test10");
		p.getProject()
			.addLicense(l);

		Scm s = new Scm();
		s.setUrl("test11");
		p.getProject()
			.setScm(s);
		p.getProject()
			.setVersion("1.0.0");

		final CommonInfoDTO infoDto = plugin.extract(p, Locale.forLanguageTag("und"));

		assertNull(infoDto.copyright);
		assertEquals("test2", infoDto.description);
		assertEquals("test3", infoDto.docURL);
		assertEquals("test4", infoDto.name);
		assertEquals("test5", infoDto.updateLocation);
		assertEquals("test6", infoDto.vendor);
		assertEquals("test7", infoDto.contactAddress.address);
		assertEquals("url", infoDto.contactAddress.type);
		assertEquals("test8", infoDto.developers.iterator()
			.next().identifier);
		assertNull(infoDto.icons);
		assertEquals("test10", infoDto.licenses.iterator()
			.next().name);
		assertEquals("test11", infoDto.scm.url);
		assertEquals(1, infoDto.version.major);
	}

	private MavenProjectWrapper getProject() throws Exception {
		List<MavenProject> ps = new ArrayList<>();
		MavenProject p = new MavenProject();

		ps.add(p);

		return new MavenProjectWrapper(ps, p);
	}
}
