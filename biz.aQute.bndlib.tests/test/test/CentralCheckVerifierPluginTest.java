package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.function.BiConsumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.plugin.maven.CentralCheck;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.strings.Strings;
import aQute.lib.xml.XML;

@ExtendWith(SoftAssertionsExtension.class)
public class CentralCheckVerifierPluginTest {
	final static DocumentBuilderFactory	dbf;
	final static XPath					xpath;

	static {
		dbf = XML.newDocumentBuilderFactory();
		dbf.setNamespaceAware(false);
		XPathFactory xpathFactory = XPathFactory.newInstance();
		xpath = xpathFactory.newXPath();
	}

	enum PomAssertions implements BiConsumer<SoftAssertions, Document> {
		groupId("project/groupId/text()"),
		artifactId("project/artifactId/text()"),
		version("project/version/text()"),
		name("project/name/text()"),
		description("project/description/text()"),
		url("project/url/text()"),
		licenses("count(project/licenses/license/*)") {
			@Override
			public void accept(SoftAssertions softly, Document doc) {
				try {
					softly.assertThat(Integer.parseInt(expr.evaluate(doc)))
						.as("project %s", name())
						.isPositive();
				} catch (XPathExpressionException e) {
					softly.fail("failed to evaluate xpath expression", e);
				}
			}
		},
		developers("count(project/developers/developer/*)") {
			@Override
			public void accept(SoftAssertions softly, Document doc) {
				try {
					softly.assertThat(Integer.parseInt(expr.evaluate(doc)))
						.as("project %s", name())
						.isPositive();
				} catch (XPathExpressionException e) {
					softly.fail("failed to evaluate xpath expression", e);
				}
			}
		},
		scm("count(project/scm/*)") {
			@Override
			public void accept(SoftAssertions softly, Document doc) {
				try {
					softly.assertThat(Integer.parseInt(expr.evaluate(doc)))
						.as("project %s", name())
						.isPositive();
				} catch (XPathExpressionException e) {
					softly.fail("failed to evaluate xpath expression", e);
				}
			}
		};

		final XPathExpression expr;

		PomAssertions(String expr) {
			try {
				this.expr = xpath.compile(expr);
			} catch (XPathExpressionException e) {
				throw Exceptions.duck(e);
			}
		}

		@Override
		public void accept(SoftAssertions softly, Document doc) {
			try {
				softly.assertThat(Strings.trim(expr.evaluate(doc)))
					.as("project %s", name())
					.isNotEmpty();
			} catch (XPathExpressionException e) {
				softly.fail("failed to evaluate xpath expression", e);
			}
		}
	}

	@Test
	void no_POM(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "false");
			b.setProperty("Bundle-SymbolicName", "p1.p2");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly.assertThat(b.check())
				.isTrue();
		}
	}

	@Test
	void missing_all(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "true");
			b.setProperty("Bundle-SymbolicName", "p1.p2");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly
				.assertThat(b.check("-groupid not set", "Bundle-Version not set", "Bundle-DocURL not set",
					"Bundle-License not set", "Bundle-Developers not set", "Bundle-SCM not set"))
				.isTrue();
		}
	}

	@Test
	void pom_instruction_groupid(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "groupid=g1,artifactid=p1.p2,version=4.5.6");
			b.setProperty("Bundle-SymbolicName", "bsn1");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setProperty("Bundle-DocURL", "https://bnd.bndtools.org/");
			b.setProperty("Bundle-License", "\"Apache-2.0\";" //
				+ "description=\"This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0\";" //
				+ "link=\"https://opensource.org/licenses/Apache-2.0\"");
			b.setProperty("Bundle-Developers", "pkriens;" //
				+ "email=Peter.Kriens@aQute.biz;" //
				+ "name=\"Peter Kriens\";" //
				+ "organization=Bndtools;"//
				+ "organizationUrl=https://github.com/bndtools;" //
				+ "roles=\"architect,developer\";" //
				+ "timezone=1");
			b.setProperty("Bundle-SCM", "url=https://github.com/bndtools/bnd," //
				+ "connection=scm:git:https://github.com/bndtools/bnd.git,"//
				+ "developerConnection=scm:git:git@github.com:bndtools/bnd.git,"//
				+ "tag=HEAD");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly.assertThat(b.check())
				.isTrue();

			Resource pomResource = jar.getResource("META-INF/maven/g1/p1.p2/pom.xml");
			assertThat(pomResource).isNotNull();
			validate_POM(softly, pomResource.openInputStream(), EnumSet.allOf(PomAssertions.class));
		}
	}

	@Test
	void groupid_instruction(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "true");
			b.setProperty("-groupid", "g1");
			b.setProperty("Bundle-SymbolicName", "p1.p2");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setProperty("Bundle-DocURL", "https://bnd.bndtools.org/");
			b.setProperty("Bundle-License", "\"Apache-2.0\";" //
				+ "description=\"This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0\";" //
				+ "link=\"https://opensource.org/licenses/Apache-2.0\"");
			b.setProperty("Bundle-Developers", "pkriens;" //
				+ "email=Peter.Kriens@aQute.biz;" //
				+ "name=\"Peter Kriens\";" //
				+ "organization=Bndtools;"//
				+ "organizationUrl=https://github.com/bndtools;" //
				+ "roles=\"architect,developer\";" //
				+ "timezone=1");
			b.setProperty("Bundle-SCM", "url=https://github.com/bndtools/bnd," //
				+ "connection=scm:git:https://github.com/bndtools/bnd.git,"//
				+ "developerConnection=scm:git:git@github.com:bndtools/bnd.git,"//
				+ "tag=HEAD");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly.assertThat(b.check())
				.isTrue();

			Resource pomResource = jar.getResource("META-INF/maven/g1/p1.p2/pom.xml");
			assertThat(pomResource).isNotNull();
			validate_POM(softly, pomResource.openInputStream(), EnumSet.allOf(PomAssertions.class));
		}
	}

	void validate_POM(SoftAssertions softly, InputStream in,
		Collection<? extends BiConsumer<? super SoftAssertions, ? super Document>> assertions) throws Exception {
		assertThat(in).as("pom inputstream")
			.isNotNull();

		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(in);
		assertThat(doc).as("pom document")
			.isNotNull();

		assertions.forEach(assertion -> assertion.accept(softly, doc));
	}

	@Test
	void missing_DocURL(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "groupid=g1");
			b.setProperty("Bundle-SymbolicName", "p1.p2");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setProperty("Bundle-License", "\"Apache-2.0\";" //
				+ "description=\"This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0\";" //
				+ "link=\"https://opensource.org/licenses/Apache-2.0\"");
			b.setProperty("Bundle-Developers", "pkriens;" //
				+ "email=Peter.Kriens@aQute.biz;" //
				+ "name=\"Peter Kriens\";" //
				+ "organization=Bndtools;"//
				+ "organizationUrl=https://github.com/bndtools;" //
				+ "roles=\"architect,developer\";" //
				+ "timezone=1");
			b.setProperty("Bundle-SCM", "url=https://github.com/bndtools/bnd," //
				+ "connection=scm:git:https://github.com/bndtools/bnd.git,"//
				+ "developerConnection=scm:git:git@github.com:bndtools/bnd.git,"//
				+ "tag=HEAD");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly.assertThat(b.check("Bundle-DocURL not set"))
				.isTrue();

			Resource pomResource = jar.getResource("META-INF/maven/g1/p1.p2/pom.xml");
			assertThat(pomResource).isNotNull();
			validate_POM(softly, pomResource.openInputStream(), EnumSet.complementOf(EnumSet.of(PomAssertions.url)));
		}
	}

	@Test
	void missing_License(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "groupid=g1");
			b.setProperty("Bundle-SymbolicName", "p1.p2");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setProperty("Bundle-DocURL", "https://bnd.bndtools.org/");
			b.setProperty("Bundle-Developers", "pkriens;" //
				+ "email=Peter.Kriens@aQute.biz;" //
				+ "name=\"Peter Kriens\";" //
				+ "organization=Bndtools;"//
				+ "organizationUrl=https://github.com/bndtools;" //
				+ "roles=\"architect,developer\";" //
				+ "timezone=1");
			b.setProperty("Bundle-SCM", "url=https://github.com/bndtools/bnd," //
				+ "connection=scm:git:https://github.com/bndtools/bnd.git,"//
				+ "developerConnection=scm:git:git@github.com:bndtools/bnd.git,"//
				+ "tag=HEAD");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly.assertThat(b.check("Bundle-License not set"))
				.isTrue();

			Resource pomResource = jar.getResource("META-INF/maven/g1/p1.p2/pom.xml");
			assertThat(pomResource).isNotNull();
			validate_POM(softly, pomResource.openInputStream(),
				EnumSet.complementOf(EnumSet.of(PomAssertions.licenses)));
		}
	}

	@Test
	void missing_Developers(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "groupid=g1");
			b.setProperty("Bundle-SymbolicName", "p1.p2");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setProperty("Bundle-DocURL", "https://bnd.bndtools.org/");
			b.setProperty("Bundle-License", "\"Apache-2.0\";" //
				+ "description=\"This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0\";" //
				+ "link=\"https://opensource.org/licenses/Apache-2.0\"");
			b.setProperty("Bundle-SCM", "url=https://github.com/bndtools/bnd," //
				+ "connection=scm:git:https://github.com/bndtools/bnd.git,"//
				+ "developerConnection=scm:git:git@github.com:bndtools/bnd.git,"//
				+ "tag=HEAD");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly.assertThat(b.check("Bundle-Developers not set"))
				.isTrue();

			Resource pomResource = jar.getResource("META-INF/maven/g1/p1.p2/pom.xml");
			assertThat(pomResource).isNotNull();
			validate_POM(softly, pomResource.openInputStream(),
				EnumSet.complementOf(EnumSet.of(PomAssertions.developers)));
		}
	}

	@Test
	void missing_SCM(SoftAssertions softly) throws Exception {
		try (Builder b = new Builder()) {
			b.setProperty("-pom", "groupid=g1");
			b.setProperty("Bundle-SymbolicName", "p1.p2");
			b.setProperty("Bundle-Version", "1.2.3");
			b.setProperty("Bundle-DocURL", "https://bnd.bndtools.org/");
			b.setProperty("Bundle-License", "\"Apache-2.0\";" //
				+ "description=\"This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0\";" //
				+ "link=\"https://opensource.org/licenses/Apache-2.0\"");
			b.setProperty("Bundle-Developers", "pkriens;" //
				+ "email=Peter.Kriens@aQute.biz;" //
				+ "name=\"Peter Kriens\";" //
				+ "organization=Bndtools;"//
				+ "organizationUrl=https://github.com/bndtools;" //
				+ "roles=\"architect,developer\";" //
				+ "timezone=1");
			b.setExportPackage("test.activator");
			b.addClasspath(new File("bin_test"));
			b.getPlugins()
				.add(new CentralCheck());

			Jar jar = b.build();
			softly.assertThat(b.check("Bundle-SCM not set"))
				.isTrue();

			Resource pomResource = jar.getResource("META-INF/maven/g1/p1.p2/pom.xml");
			assertThat(pomResource).isNotNull();
			validate_POM(softly, pomResource.openInputStream(), EnumSet.complementOf(EnumSet.of(PomAssertions.scm)));
		}
	}

}
