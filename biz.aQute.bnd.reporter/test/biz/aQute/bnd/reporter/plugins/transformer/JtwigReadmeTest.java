package biz.aQute.bnd.reporter.plugins.transformer;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import biz.aQute.bnd.reporter.plugins.transformer.TwigChecker.ListBuilder;
import biz.aQute.bnd.reporter.plugins.transformer.TwigChecker.MapBuilder;

public class JtwigReadmeTest {

	@Test
	public void testMinReadme() throws Exception {
		TwigChecker checker = checker();

		checker.expect("# ")
			.check();
	}

	@Test
	public void testMaxReadme() throws Exception {
		TwigChecker checker = checker();

		MapBuilder data = map();

		data.set("manifest", map().set("bundleName", "Name")
			.set("bundleDescription", "Description")
			.set("bundleVendor", "vendor")
			.set("bundleDocURL", "doc")

			.set("bundleSCM", map().set("url", "url"))
			.set("bundleDevelopers", list(map().set("identifier", "myId ")))
			.set("bundleLicenses", list(map().set("name", "License1")))
			.set("bundleUpdateLocation", "url")
			.set("bundleCopyright", "Copyright 2019")
			.set("bundleVersion", map().set("major", "1")
				.set("minor", "0")
				.set("micro", "0"))
			.set("bundleSymbolicName", map().set("symbolicName", "org.test")));

		data.set("projects", list(map().set("fileName", "folder")));

		data.set("codeSnippets", list().xadd(map().set("programmingLanguage", "java")
			.set("codeSnippet", "test")));

		data.set("components", list(map().set("name", "name")
			.set("configurationPolicy", "ignore")));

		checker.with(data)
			.expect("# Name")
			.expectBlankLine()
			.expect("Description")
			.expectBlankLine()
			.expect("## Links")
			.expectBlankLine()
			.expect("* [Documentation](doc)")
			.expect("* [Source Code](url)")
			.expect("* [Artifact(s)](url)")
			.expectBlankLine()
			.expect("## Coordinates")
			.expectBlankLine()
			.expect("### OSGi")
			.expectBlankLine()
			.expect("```")
			.expect("Bundle Symbolic Name: org.test")
			.expect("Version             : 1.0.0")
			.expect("```")
			.expectBlankLine()
			.expect("## Built Artifacts")
			.expectBlankLine()
			.expect("* [**folder**](folder)")
			.expectBlankLine()
			.expect("## Code Usage")
			.expectBlankLine()
			.expect("```java")
			.expect("test")
			.expect("```")
			.expectBlankLine()
			.expect("## Components")
			.expectBlankLine()
			.expect("### name - *state = not enabled, activation = delayed*")
			.expectBlankLine()
			.expect("#### Description")
			.expectBlankLine()
			.expect("#### Services")
			.expectBlankLine()
			.expect("No services.")
			.expectBlankLine()
			.expect("#### Properties")
			.expectBlankLine()
			.expect("No properties.")
			.expectBlankLine()
			.expect("#### Configuration")
			.expectBlankLine()
			.expect("No configuration.")
			.expectBlankLine()
			.expect("#### Reference bindings")
			.expectBlankLine()
			.expect("No bindings.")
			.expectBlankLine()
			.expect("#### OSGi-Configurator")
			.expectBlankLine()
			.expectBlankLine()
			.expect("```")
			.expect("/*")
			.expect(" * Component: name")
			.expect(" * policy:    ignore")
			.expect(" */")
			.expect("\"\":{")
			.expect("        //# Component properties")
			.expect("        // none")
			.expectBlankLine()
			.expect("        //# Reference bindings")
			.expect("        // none")
			.expectBlankLine()
			.expect("        //# ObjectClassDefinition - Attributes")
			.expect("        /*")
			.expect("         * Required = ")
			.expect("         * Type = ")
			.expect("         */")
			.expect("         // \"\": null")
			.expect("}")
			.expect("```")
			.expectBlankLine()
			.expect("## Developers")
			.expectBlankLine()
			.expect("* myId")
			.expectBlankLine()
			.expect("## Licenses")
			.expectBlankLine()
			.expect("**License1**")
			.expectBlankLine()
			.expect("## Copyright")
			.expectBlankLine()
			.expect("Copyright 2019")
			.expectBlankLine()
			.expect("---")
			.expect("vendor")
			.check();
	}

	static public TwigChecker checker() throws URISyntaxException {
		return new TwigChecker(new File(JtwigTransformerPlugin.class.getResource("templates/readme.twig")
			.toURI()));
	}

	static public ListBuilder list(Object... objects) {
		ListBuilder b = new TwigChecker.ListBuilder();
		if (objects != null) {
			for (Object o : objects) {
				b.add(o);
			}
		}
		return b;
	}

	static public MapBuilder map() {
		return new TwigChecker.MapBuilder();
	}
}
