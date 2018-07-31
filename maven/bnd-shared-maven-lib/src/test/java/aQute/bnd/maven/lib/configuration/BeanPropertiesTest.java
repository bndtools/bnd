package aQute.bnd.maven.lib.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Properties;

import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import aQute.bnd.osgi.Processor;

public class BeanPropertiesTest {
	private Properties		beanProperties;
	private Processor		processor;
	private MavenProject	project;
	@Rule
	public TestName			testName	= new TestName();

	@Before
	public void setUp() throws Exception {
		project = new MavenProject(new Model());
		project.setGroupId(BeanPropertiesTest.class.getName());
		project.setArtifactId(testName.getMethodName());
		project.setVersion("0.0.1");
		project.setFile(new File(project.getGroupId(), project.getArtifactId()));

		beanProperties = new BeanProperties();
		beanProperties.put("project", project);
		Properties mavenProperties = new Properties(beanProperties);
		mavenProperties.putAll(project.getProperties());

		processor = new Processor(mavenProperties, false);

		assertThat(beanProperties).hasSize(1);
	}

	@After
	public void tearDown() throws Exception {
		processor.close();
	}

	@Test
	public void testProjectName() {
		project.setName(testName.getMethodName());
		assertThat(beanProperties.getProperty("project")).contains(project.getGroupId(), project.getArtifactId());
		assertThat(beanProperties.getProperty("project.name")).isEqualTo(testName.getMethodName());

		processor.setProperty("Project-Name", "${project.name}");
		assertThat(processor.getProperty("Project-Name")).isEqualTo(testName.getMethodName());
	}

	@Test
	public void testProjectLicense() {
		License license = new License();
		license.setName("Test License");
		project.addLicense(license);
		assertThat(project.getLicenses()).hasSize(1)
			.contains(license);
		assertThat(project.getLicenses()
			.get(0)).hasFieldOrPropertyWithValue("name", "Test License");

		assertThat(beanProperties.getProperty("project.licenses")).isNotNull();
		assertThat(beanProperties.getProperty("project.licenses[0]")).isNotNull();
		assertThat(beanProperties.getProperty("project.licenses[0].name")).isEqualTo("Test License");

		processor.setProperty("Project-License", "${project.licenses\\[0\\].name}");
		assertThat(processor.getProperty("Project-License")).isEqualTo("Test License");
	}

}
