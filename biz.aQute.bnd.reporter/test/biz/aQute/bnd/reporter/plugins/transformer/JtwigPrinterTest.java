package biz.aQute.bnd.reporter.plugins.transformer;

import biz.aQute.bnd.reporter.plugins.transformer.TwigChecker.ListBuilder;
import biz.aQute.bnd.reporter.plugins.transformer.TwigChecker.MapBuilder;
import junit.framework.TestCase;

public class JtwigPrinterTest extends TestCase {

	public void testTitle() throws Exception {
		TwigChecker checker = checker("printTitle");

		checker.with("")
			.with(1)
			.expect("# ")
			.check();

		checker.with("")
			.with(2)
			.expect("## ")
			.check();

		checker.with("")
			.with(3)
			.expect("### ")
			.check();

		checker.with("")
			.with(4)
			.expect("#### ")
			.check();

		checker.with("")
			.with(5)
			.expect("##### ")
			.check();

		checker.with("")
			.with(6)
			.expect("###### ")
			.check();

		checker.with("")
			.with(7)
			.expect("####### ")
			.check();

		checker.with("test")
			.with(1)
			.expect("# test")
			.check();

		checker.with(" test ")
			.with(1)
			.expect("# test")
			.check();

		checker.with("test")
			.with(1)
			.with("https://url.com/test.gif")
			.expect("# ![](https://url.com/test.gif) test")
			.check();
	}

	public void testSlingFeatureCoordinates() throws Exception {
		TwigChecker checker = checker("printFeatureCoordinate");

		checker.with(map().set("groupId", "groupIdTest")
			.set("artifactId", "artifactIdTest")
			.set("version", "versionTest"))
			.expect("```")
			.expect("\"bundles\": [")
			.expect("   {")
			.expect("    \"id\": \"groupIdTest:artifactIdTest:versionTest\"")
			.expect("   }")
			.expect("]")
			.expect("```")
			.check();

		checker.with(map().set("groupId", "groupIdTest")
			.set("artifactId", "artifactIdTest")
			.set("version", "versionTest")
			.set("classifier", "extra"))
			.with(map().set("sha1", "theChecksum"))
			.expect("```")
			.expect("\"bundles\": [")
			.expect("   {")
			.expect("    \"id\": \"groupIdTest:artifactIdTest:versionTest:jar:extra\"")
			.expect("    \"hash\": \"theChecksum\"")
			.expect("   }")
			.expect("]")
			.expect("```")
			.check();

	}

	public void testMavenCoordinate() throws Exception {
		TwigChecker checker = checker("printMavenCoordinate");

		checker.with(map().set("groupId", "groupIdTest")
			.set("artifactId", "artifactIdTest")
			.set("version", "versionTest"))
			.expect("```xml")
			.expect("<dependency>")
			.expect("    <groupId>groupIdTest</groupId>")
			.expect("    <artifactId>artifactIdTest</artifactId>")
			.expect("    <version>versionTest</version>")
			.expect("</dependency>")
			.expect("```")
			.check();

		checker.with(map().set("groupId", "groupIdTest")
			.set("artifactId", "artifactIdTest")
			.set("version", "versionTest")
			.set("classifier", "extra"))
			.expect("```xml")
			.expect("<dependency>")
			.expect("    <groupId>groupIdTest</groupId>")
			.expect("    <artifactId>artifactIdTest</artifactId>")
			.expect("    <version>versionTest</version>")
			.expect("    <classifier>extra</classifier>")
			.expect("</dependency>")
			.expect("```")
			.check();

		checker.with(map().set("groupId", " groupIdTest ")
			.set("artifactId", " artifactIdTest ")
			.set("version", " versionTest "))
			.with("**")
			.expect("```xml")
			.expect("**<dependency>")
			.expect("**    <groupId>groupIdTest</groupId>")
			.expect("**    <artifactId>artifactIdTest</artifactId>")
			.expect("**    <version>versionTest</version>")
			.expect("**</dependency>")
			.expect("**```")
			.check();
	}

	public void testOSGiCoordinate() throws Exception {
		TwigChecker checker = checker("printOsgiCoordinate");

		checker.with("test")
			.with(map().set("major", 1)
				.set("minor", 0)
				.set("micro", 0))
			.expect("```")
			.expect("Bundle Symbolic Name: test")
			.expect("Version             : 1.0.0")
			.expect("```")
			.check();

		checker.with(" test ")
			.with(map().set("major", 1)
				.set("minor", 0)
				.set("micro", 0))
			.with("**")
			.expect("```")
			.expect("**Bundle Symbolic Name: test")
			.expect("**Version             : 1.0.0")
			.expect("**```")
			.check();
	}

	public void testChecksum() throws Exception {
		TwigChecker checker = checker("printChecksum");

		checker.with(map().set("md5", "md5Value")
			.set("sha1", "sha1Value")
			.set("sha256", "sha256Value"))
			.expect("```")
			.expect("md5:    md5Value")
			.expect("sha1:   sha1Value")
			.expect("sha256: sha256Value")
			.expect("```")
			.check();

		checker.with(map().set("sha1", "sha1Value"))
			.expect("```")
			.expect("sha1:   sha1Value")
			.expect("```")
			.check();
	}

	public void testOSGiVersion() throws Exception {
		TwigChecker checker = checker("printOsgiVersion");

		checker.with(map().set("major", "1")
			.set("minor", "0")
			.set("micro", "0"))
			.expect("1.0.0")
			.check();

		checker.with(map().set("major", "1")
			.set("minor", "0")
			.set("micro", "0")
			.set("qualifier", "RELEASE"))
			.expect("1.0.0.RELEASE")
			.check();
	}

	public void testTableHeaders() throws Exception {
		TwigChecker checker = checker("printTableHeaders");

		checker.with(list())
			.expectBlankLine()
			.check();

		checker.with(list("h1"))
			.expect("|h1 |")
			.expect("|--- |")
			.check();

		checker.with(list(" h1 ", " h2 "))
			.expect("|h1 |h2 |")
			.expect("|--- |--- |")
			.check();

		checker.with(list(" h1 ", " h2 "))
			.with("ll")
			.expect("|h1 |h2 |")
			.expect("|--- |--- |")
			.check();

		checker.with(list(" h1 ", " h2 ", " h3 "))
			.with("rcl")
			.expect("|h1 |h2 |h3 |")
			.expect("|---: |:---: |--- |")
			.check();

		checker.with(list(" h1 ", " h2 ", " h3 "))
			.with("lrc")
			.with("**")
			.expect("|h1 |h2 |h3 |")
			.expect("**|--- |---: |:---: |")
			.check();
	}

	public void testPropertyType() throws Exception {
		TwigChecker checker = checker("printPropertyType");

		checker.with("Type")
			.expect("Type")
			.check();

		checker.with("Type")
			.with("true")
			.expect("Type[]")
			.check();

		checker.with("Type")
			.with("false")
			.expect("Type")
			.check();
	}

	public void testPropertyValues() throws Exception {
		TwigChecker checker = checker("printPropertyValues");

		checker.with("Type")
			.with(list())
			.expect("")
			.check();

		checker.with("Integer")
			.with(list(1))
			.expect("1")
			.check();

		checker.with("Integer")
			.with(list(1, 2))
			.expect("1")
			.check();

		checker.with("Integer")
			.with(list(1, 2))
			.with(false)
			.expect("1")
			.check();

		checker.with("Integer")
			.with(list(1, 2))
			.with(true)
			.expect("[1, 2]")
			.check();

		checker.with("Type")
			.with(list("Value1", "Value2"))
			.with(true)
			.expect("[Value1, Value2]")
			.check();

		checker.with("String")
			.with(list("Value1", "Value2"))
			.with(true)
			.expect("[\"Value1\", \"Value2\"]")
			.check();

		checker.with("String")
			.with(list("Value1", "Value2"))
			.expect("\"Value1\"")
			.check();
	}

	public void testOptions() throws Exception {
		TwigChecker checker = checker("printOptions");

		checker.with(list())
			.expect("")
			.check();

		checker.with(list(map().set("value", "OPT1")))
			.expect("\"OPT1\"")
			.check();

		checker.with(list(map().set("value", "OPT1"), map().set("value", "OPT2")))
			.expect("\"OPT1\", \"OPT2\"")
			.check();
	}

	public void testComponents() throws Exception {
		TwigChecker checker = checker("printComponents");

		checker.with(list(), list())
			.check();

		checker.with(list(map().set("name", "name")
			.set("configurationPolicy", "ignore")), list())
			.expect("### name - *state = not enabled, activation = delayed*")
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
			.check();

		checker.with(list(map().set("name", "name")
			.set("configurationPolicy", "ignore"),
			map().set("name", "name2")
				.set("configurationPolicy", "ignore")),
			list())
			.expect("### name - *state = not enabled, activation = delayed*")
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
			.expect("---")
			.expectBlankLine()
			.expect("### name2 - *state = not enabled, activation = delayed*")
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
			.check();
	}

	public void testComponentTitle() throws Exception {
		TwigChecker checker = checker("_printComponentTitle");

		checker.with(map().set("name", "name"))
			.expect("### name - *state = not enabled, activation = delayed*")
			.check();

		checker.with(map().set("name", "name")
			.set("defaultEnabled", true)
			.set("immediate", true))
			.expect("### name - *state = enabled, activation = immediate*")
			.check();

		checker.with(map().set("name", "name")
			.set("defaultEnabled", false)
			.set("immediate", true))
			.expect("### name - *state = not enabled, activation = immediate*")
			.check();

		checker.with(map().set("name", "name")
			.set("defaultEnabled", true)
			.set("immediate", false))
			.expect("### name - *state = enabled, activation = delayed*")
			.check();
	}

	public void testComponentServices() throws Exception {
		TwigChecker checker = checker("_printComponentServices");

		checker.with(map())
			.expect("#### Services")
			.expectBlankLine()
			.expect("No services.")
			.check();

		checker.with(map().set("scope", "test")
			.set("serviceInterfaces", list("org.test")))
			.expect("#### Services - *scope = test*")
			.expectBlankLine()
			.expect("|Interface name |")
			.expect("|--- |")
			.expect("|org.test |")
			.check();

		checker.with(map().set("scope", "test")
			.set("serviceInterfaces", list("org.test", "org.test2")))
			.expect("#### Services - *scope = test*")
			.expectBlankLine()
			.expect("|Interface name |")
			.expect("|--- |")
			.expect("|org.test |")
			.expect("|org.test2 |")
			.check();
	}

	public void testComponentProperties() throws Exception {
		TwigChecker checker = checker("_printComponentProperties");

		checker.with(map())
			.expect("#### Properties")
			.expectBlankLine()
			.expect("No properties.")
			.check();

		checker.with(map().set("properties", map().set("prop1", map().set("type", "Integer")
			.set("values", list(1)))))
			.expect("#### Properties")
			.expectBlankLine()
			.expect("|Name |Type |Value |")
			.expect("|--- |--- |--- |")
			.expect("|prop1 |Integer |1 |")
			.check();

		checker.with(map().set("properties", map().set("prop1", map().set("type", "Integer")
			.set("values", list(1)))
			.set("prop2", map().set("type", "Integer")
				.set("values", list(1)))))
			.expect("#### Properties")
			.expectBlankLine()
			.expect("|Name |Type |Value |")
			.expect("|--- |--- |--- |")
			.expect("|prop1 |Integer |1 |")
			.expect("|prop2 |Integer |1 |")
			.check();
	}

	public void testComponentConfiguration() throws Exception {
		TwigChecker checker = checker("_printComponentConfiguration");

		checker.with(map().set("factory", "factory.key"))
			.expect("#### Configuration - *factory*")
			.expectBlankLine()
			.expect("Factory name: `factory.key`")
			.check();

		checker.with(map().set("configurationPolicy", "ignore"))
			.expect("#### Configuration")
			.expectBlankLine()
			.expect("No configuration.")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("No information available.")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid", "my.pid2")))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("No information available.")
			.expectBlankLine()
			.expect("##### Pid: `my.pid2`")
			.expectBlankLine()
			.expect("No information available.")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.with(list(map().set("pids", list("my.pid2"))))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("No information available.")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.with(list(map().set("pids", list("my.pid"))
				.set("attributes", list(map().set("id", "myId")
					.set("type", "Integer")
					.set("required", "false")))))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|Id |`myId` |")
			.expect("|Required |**false** |")
			.expect("|Type |**Integer** |")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.with(list(map().set("factoryPids", list("my.pid"))
				.set("attributes", list(map().set("id", "myId")
					.set("type", "Integer")
					.set("required", "false")))))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Factory Pid: `my.pid`")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|Id |`myId` |")
			.expect("|Required |**false** |")
			.expect("|Type |**Integer** |")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.with(list(map().set("pids", list("my.pid"))
				.set("attributes", list(map().set("id", "myId")
					.set("type", "Integer")
					.set("required", "false"),
					map().set("id", "myId2")
						.set("type", "Integer")
						.set("required", "false")))))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|Id |`myId` |")
			.expect("|Required |**false** |")
			.expect("|Type |**Integer** |")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|Id |`myId2` |")
			.expect("|Required |**false** |")
			.expect("|Type |**Integer** |")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.with(list(map().set("pids", list("my.pid"))
				.set("attributes", list(map().set("id", "myId")
					.set("type", "Integer")
					.set("description", "Description")
					.set("values", list(1))
					.set("max", "10")
					.set("min", "1")
					.set("required", "false")))))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|Id |`myId` |")
			.expect("|Required |**false** |")
			.expect("|Type |**Integer** |")
			.expect("|Description |Description |")
			.expect("|Default |1 |")
			.expect("|Value range |`min = 1` / `max = 10` |")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.with(list(map().set("pids", list("my.pid"))
				.set("attributes", list(map().set("id", "myId")
					.set("type", "Integer")
					.set("max", "10")
					.set("required", "false")))))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|Id |`myId` |")
			.expect("|Required |**false** |")
			.expect("|Type |**Integer** |")
			.expect("|Value range |`max = 10` |")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid")))
			.with(list(map().set("pids", list("my.pid"))
				.set("attributes", list(map().set("id", "myId")
					.set("type", "Integer")
					.set("options", list(map().set("value", "VALUE")))
					.set("required", "false")))))
			.expect("#### Configuration - *policy = required*")
			.expectBlankLine()
			.expect("##### Pid: `my.pid`")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|Id |`myId` |")
			.expect("|Required |**false** |")
			.expect("|Type |**Integer** |")
			.expect("|Value range |\"VALUE\" |")
			.check();
	}

	public void testComponentOSGiConfiguratorSnippet() throws Exception {
		TwigChecker checker = checker("_printComponentJsonConfiguratorSnippet");

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid"))
			.set("name", "MyComponentName"))
			.expect("#### OSGi-Configurator")
			.expectBlankLine()
			.expectBlankLine()
			.expect("```")
			.expect("/*")
			.expect(" * Component: MyComponentName")
			.expect(" * policy:    required")
			.expect(" */")
			.expect("\"my.pid\":{")
			.expect("        //# Component properties")
			.expect("        // none")
			.expectBlankLine()
			.expect("        //# Reference bindings")
			.expect("        // none")
			.expectBlankLine()
			.expect("        //# ObjectClassDefinition - Attributes")
			.expect("        // (No PidOcd available.)")
			.expect("}")
			.expect("```")
			.check();

		checker.with(map().set("configurationPolicy", "required")
			.set("configurationPid", list("my.pid"))
			.set("name", "MyComponentName")
			.set("properties", map().set("prop1", map().set("values", list("value1"))
				.set("type", "String")
				.set("multiValue", false))
				.set("prop2", map().set("values", list("value2", "value3"))
					.set("type", "String")
					.set("multiValue", true)))
			.set("references", list(map().set("cardinality", "cardinality")
				.set("interfaceName", "interfaceName")
				.set("name", "name")
				.set("policy", "policy")
				.set("policyOption", "policyOption")
				.set("scope", "scope")
				.set("target", "target"),
				map().set("cardinality", "cardinality")
					.set("interfaceName", "interfaceName")
					.set("name", "name")
					.set("policy", "policy")
					.set("policyOption", "policyOption")
					.set("scope", "scope")
					.set("target", null))))
			.with(list(map().set("pids", list("my.pid"))
				.set("attributes", list(map().set("id", "myId1")
					.set("type", "Integer")
					.set("required", true),
					map().set("id", "myId2")
						.set("type", "Integer")
						.set("description", "descriptionFooBar")
						.set("min", "1")
						.set("max", "10")
						.set("cardinality", "1")
						.set("values", list("2", "3", "4"))
						.set("required", false)))))
			.expect("#### OSGi-Configurator")
			.expectBlankLine()
			.expectBlankLine()
			.expect("```")
			.expect("/*")
			.expect(" * Component: MyComponentName")
			.expect(" * policy:    required")
			.expect(" */")
			.expect("\"my.pid\":{")
			.expect("        //# Component properties")
			.expect("        /*")
			.expect("         * Type = String")
			.expect("         * Default = \"value1\"")
			.expect("         */")
			.expect("         // \"prop1\": null,")
			.expectBlankLine()
			.expect("        /*")
			.expect("         * Type = String[]")
			.expect("         * Default = [\"value2\", \"value3\"]")
			.expect("         */")
			.expect("         // \"prop2\": null,")
			.expectBlankLine()
			.expectBlankLine()
			.expect("        //# Reference bindings")
			.expect("        // \"name.target\": \"target\",")
			.expect("        // \"name.target\": \"(component.pid=*)\",")
			.expectBlankLine()
			.expectBlankLine()
			.expect("        //# ObjectClassDefinition - Attributes")
			.expect("        /*")
			.expect("         * Required = true")
			.expect("         * Type = Integer")
			.expect("         */")
			.expect("         \"myId1\": null,")
			.expectBlankLine()
			.expect("        /*")
			.expect("         * Required = false")
			.expect("         * Type = Integer[]")
			.expect("         * Description = descriptionFooBar")
			.expect("         * Default = [2, 3, 4]")
			.expect("         * Value restriction = `min = 1` / `max = 10`")
			.expect("         */")
			.expect("         // \"myId2\": null")
			.expect("}")
			.expect("```")
			.check();

	}

	public void testComponentReferences() throws Exception {
		TwigChecker checker = checker("_printComponentReferences");

		checker.with(map())
			.expect("#### Reference bindings")
			.expectBlankLine()
			.expect("No bindings.")
			.check();

		checker.with(map().set("references", list(map().set("cardinality", "cardinality")
			.set("interfaceName", "interfaceName")
			.set("name", "name")
			.set("policy", "policy")
			.set("policyOption", "policyOption")
			.set("scope", "scope")
			.set("target", "target"))))
			.expect("#### Reference bindings")
			.expectBlankLine()
			.expect("|Attribute |Value |")
			.expect("|--- |--- |")
			.expect("|name |name |")
			.expect("|interfaceName |interfaceName |")
			.expect("|target |target |")
			.expect("|cardinality |cardinality |")
			.expect("|policy |policy |")
			.expect("|policyOption |policyOption |")
			.expect("|scope |scope |")
			.check();
	}

	public void testGogoCommands() throws Exception {
		TwigChecker checker = checker("_printGogoCommands");

		checker.with(list(map().set("title", "S1_title")
			.set("functions", list(map().set("title", "S1F1_title")
				.set("methods", list(map().set("title", "S1F1M1_title")
					.set("description", "S1F1M1_description - NoParams")
					.set("parameters", null),
					map().set("title", "S1F1M2_title")
						.set("description",
							"S1F1M2_description - OneParam  NoNames TitleIsSet AbsendIsNotSet PresentIsNotSet")
						.set("parameters", list(map().set("absentValue", null)
							.set("presentValue", null)
							.set("description", "S1F1M2P1_Description Array")
							.set("names", null)
							.set("type", "String[]")
							.set("title", "S1F1M2P1_Title"))),
					map().set("title", "S1F1M3_title")
						.set("description",
							"S1F1M3_description - OneParam  NoNames TitleIsNotSet AbsendIsNotSet PresentIsNotSet")
						.set("parameters", list(map().set("absentValue", null)
							.set("presentValue", null)
							.set("description", "S1F1M3P1_Description")
							.set("names", null)
							.set("title", null))),
					map().set("title", "S1F1M4_title")
						.set("description",
							"S1F1M4_description - TwoParam  NoNames TitleIsNotSet AbsendIsNotSet PresentIsNotSet")
						.set("parameters", list(map().set("absentValue", null)
							.set("presentValue", null)
							.set("description", "S1F1M4P1_Description")
							.set("names", null)
							.set("title", null),
							map().set("absentValue", null)
								.set("presentValue", null)
								.set("description", "S1F1M4P2_Description")
								.set("names", null)
								.set("title", null))),
					map().set("title", "S1F1M6_title")
						.set("description",
							"S1F1M6_description - TwoParam (1. WithNames AbsendisSet PresentIsSet), (2. WithNames AbsendisSet PresentIsNotSet)")
						.set("parameters", list(map().set("absentValue", "false")
							.set("presentValue", "true")
							.set("description", "S1F1M6P1_Description")
							.set("names", list("--f", "-force"))
							.set("title", "S1F1M6P1_Title"),
							map().set("absentValue", "debug")
								.set("presentValue", null)
								.set("description", "S1F1M6P2_Description")
								.set("names", list("--l", "-log"))
								.set("title", "S1F1M6P2_Title"))),
					map().set("title", "S1F1M7_title")
						.set("description", "S1F1M7_description - ForeParam")
						.set("parameters", list(map().set("absentValue", "false")
							.set("presentValue", "true")
							.set("description", "S1F1M7P1_Description  WithNames AbsendIsSet PresentIsSet)")
							.set("names", list("--f", "-force"))
							.set("title", "S1F1M7P1_Title"),
							map().set("absentValue", "debug")
								.set("presentValue", null)
								.set("description", "S1F1M7P2_Description  WithNames AbsendIsSet PresentIsNull)")
								.set("names", list("--l", "-log"))
								.set("title", "S1F1M7P2_Title"),
							map().set("absentValue", null)
								.set("presentValue", null)
								.set("description", "S1F1M7P3_Description WithNames AbsendIsNull PresentIsNull)")
								.set("names", null)
								.set("title", "S1F1M7P3_Title"),
							map().set("absentValue", null)
								.set("presentValue", null)
								.set("description", "S1F1M7P4_Description NoNames NoTitle AbsendIsNull PresentIsNull")
								.set("names", null)
								.set("title", null))))))),
			map().set("title", "S2_title")
				.set("functions", list(map().set("title", "S2F1_title")
					.set("methods", list(map().set("title", "S2F1M1_title")
						.set("description", "S2F1M1_description - NoParams")
						.set("parameters", null)))))))
			.expect("### S1_title:S1F1_title")
			.expectBlankLine()
			.expect("**Synopsis**")
			.expect("`S1_title:S1F1_title")
			.expectBlankLine()
			.expect("**Description**")
			.expect("S1F1M1_description - NoParams")
			.expectBlankLine()
			.expectBlankLine()
			.expect("---")
			.expectBlankLine()
			.expect("**Synopsis**")
			.expect("`S1_title:S1F1_title S1F1M2P1_TITLE...`")
			.expectBlankLine()
			.expect("**Description**")
			.expect("S1F1M2_description - OneParam  NoNames TitleIsSet AbsendIsNotSet PresentIsNotSet")
			.expectBlankLine()
			.expect("**Arguments**")
			.expect("* `S1F1M2P1_TITLE...` S1F1M2P1_Description Array")
			.expectBlankLine()
			.expectBlankLine()
			.expect("---")
			.expectBlankLine()
			.expect("**Synopsis**")
			.expect("`S1_title:S1F1_title ARG0`")
			.expectBlankLine()
			.expect("**Description**")
			.expect("S1F1M3_description - OneParam  NoNames TitleIsNotSet AbsendIsNotSet PresentIsNotSet")
			.expectBlankLine()
			.expect("**Arguments**")
			.expect("* `ARG0` S1F1M3P1_Description")
			.expectBlankLine()
			.expectBlankLine()
			.expect("---")
			.expectBlankLine()
			.expect("**Synopsis**")
			.expect("`S1_title:S1F1_title ARG0 ARG1`")
			.expectBlankLine()
			.expect("**Description**")
			.expect("S1F1M4_description - TwoParam  NoNames TitleIsNotSet AbsendIsNotSet PresentIsNotSet")
			.expectBlankLine()
			.expect("**Arguments**")
			.expect("* `ARG0` S1F1M4P1_Description")
			.expect("* `ARG1` S1F1M4P2_Description")
			.expectBlankLine()
			.expectBlankLine()
			.expect("---")
			.expectBlankLine()
			.expect("**Synopsis**")
			.expect("`S1_title:S1F1_title [OPTIONS]")
			.expectBlankLine()
			.expect("**Description**")
			.expect(
				"S1F1M6_description - TwoParam (1. WithNames AbsendisSet PresentIsSet), (2. WithNames AbsendisSet PresentIsNotSet)")
			.expectBlankLine()
			.expect("**Options**")
			.expect("* `--f , -force ` S1F1M6P1_Description")
			.expect("* `--l S1F1M6P2_TITLE, -log S1F1M6P2_TITLE` S1F1M6P2_Description")
			.expectBlankLine()
			.expect("---")
			.expectBlankLine()
			.expect("**Synopsis**")
			.expect("`S1_title:S1F1_title [OPTIONS] S1F1M7P3_TITLE ARG3`")
			.expectBlankLine()
			.expect("**Description**")
			.expect("S1F1M7_description - ForeParam")
			.expectBlankLine()
			.expect("**Arguments**")
			.expect("* `S1F1M7P3_TITLE` S1F1M7P3_Description WithNames AbsendIsNull PresentIsNull)")
			.expect("* `ARG3` S1F1M7P4_Description NoNames NoTitle AbsendIsNull PresentIsNull")
			.expectBlankLine()
			.expect("**Options**")
			.expect("* `--f , -force ` S1F1M7P1_Description  WithNames AbsendIsSet PresentIsSet)")
			.expect(
				"* `--l S1F1M7P2_TITLE, -log S1F1M7P2_TITLE` S1F1M7P2_Description  WithNames AbsendIsSet PresentIsNull)")
			.expectBlankLine()
			.expect("---")
			.expect("### S2_title:S2F1_title")
			.expectBlankLine()
			.expect("**Synopsis**")
			.expect("`S2_title:S2F1_title")
			.expectBlankLine()
			.expect("**Description**")
			.expect("S2F1M1_description - NoParams")
			.expectBlankLine()
			.expectBlankLine()
			.expect("---")
			.expectBlankLine()
			.check();

	}

	public void testDevelopers() throws Exception {
		TwigChecker checker = checker("printDevelopers");

		checker.with(list())
			.check();

		checker.with(list(map().set("identifier", "myId ")))
			.expect("* myId")
			.check();

		checker.with(list(map().set("email", "jo@pm.me ")))
			.expect("* [jo@pm.me](mailto:jo@pm.me)")
			.check();

		checker.with(list(map().set("identifier", "myId ")
			.set("name", "Jo Do")))
			.expect("* **Jo Do** (myId)")
			.check();

		checker.with(list(map().set("identifier", "jo@pm.me")
			.set("name", "Jo Do")))
			.expect("* **Jo Do** / [jo@pm.me](mailto:jo@pm.me)")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("email", "jo@pm.me")))
			.expect("* myId / [jo@pm.me](mailto:jo@pm.me)")
			.check();

		checker.with(list(map().set("name", "Jo Do")
			.set("email", "jo@pm.me")))
			.expect("* **Jo Do** / [jo@pm.me](mailto:jo@pm.me)")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("name", "Jo Do")
			.set("email", "jo@pm.me")))
			.expect("* **Jo Do** (myId) / [jo@pm.me](mailto:jo@pm.me)")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("organization", "test")))
			.expect("* myId @ test")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("organizationUrl", "testurl")))
			.expect("* myId @ [testurl](testurl)")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("organization", "test ")
			.set("organizationUrl", " testurl")))
			.expect("* myId @ [test](testurl)")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("roles", list())))
			.expect("* myId")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("roles", list("a"))))
			.expect("* myId - *a*")
			.check();

		checker.with(list(map().set("identifier", "myId")
			.set("roles", list("a", "b"))))
			.expect("* myId - *a*, *b*")
			.check();

		map().set("identifier", "")
			.set("name", "")
			.set("email", "")
			.set("identifier", "")
			.set("organization", "")
			.set("organizationUrl", "")
			.set("roles", list());

		checker.with(list(map().set("identifier", "myId")
			.set("name", "Jo Do")
			.set("email", "jo@pm.me")
			.set("organization", "test")
			.set("organizationUrl", "testurl")
			.set("roles", list("a", "b")),
			map().set("identifier", "myId2")
				.set("name", "Jo Do2")
				.set("email", "jo@pm.me2")
				.set("organization", "test2")
				.set("organizationUrl", "testurl2")
				.set("roles", list("c"))))
			.expect("* **Jo Do** (myId) / [jo@pm.me](mailto:jo@pm.me) @ [test](testurl) - *a*, *b*")
			.expect("* **Jo Do2** (myId2) / [jo@pm.me2](mailto:jo@pm.me2) @ [test2](testurl2) - *c*")
			.check();
	}

	public void testLicenses() throws Exception {
		TwigChecker checker = checker("printLicenses");

		checker.with(list())
			.check();

		checker.with(list(map().set("name", "License1")))
			.expect("**License1**")
			.check();

		checker.with(list(map().set("name", "License1")
			.set("description", "My description. ")))
			.expect("**License1**")
			.expect("  > My description.")
			.check();

		checker.with(list(map().set("name", "License1")
			.set("description", "My description. ")
			.set("link", "mylink")))
			.expect("**License1**")
			.expect("  > My description.")
			.expect("  >")
			.expect("  > For more information see [mylink](mylink).")
			.check();

		checker.with(list(map().set("name", "License1")
			.set("description", "My description. ")
			.set("link", "mylink"), map().set("name", "License2"), map().set("name", "License2bis"),
			map().set("name", "License3")
				.set("link", "mylink2")))
			.expect("**License1**")
			.expect("  > My description.")
			.expect("  >")
			.expect("  > For more information see [mylink](mylink).")
			.expectBlankLine()
			.expect("**License2**")
			.expectBlankLine()
			.expect("**License2bis**")
			.expectBlankLine()
			.expect("**License3**")
			.expect("  >")
			.expect("  > For more information see [mylink2](mylink2).")
			.check();
	}

	public void testArtifacts() throws Exception {
		TwigChecker checker = checker("printArtifacts");

		checker.with(map())
			.check();

		MapBuilder project = map();
		MapBuilder bundle = map();
		MapBuilder manifest = map();

		// One project
		manifest.set("bundleName", "Name");
		manifest.set("bundleDescription", "Description");
		manifest.set("bundleSymbolicName", map().set("symbolicName", "org.name"));
		project.set("commonInfo", map().set("name", "CName")
			.set("description", "CDescription"));
		project.set("fileName", "folder1");
		project.set("bundles", list(bundle.set("manifest", manifest)));

		checker.with(map().set("projects", list(project)))
			.expect("* [**Name**](folder1): Description")
			.check();

		manifest.set("bundleName", null);
		manifest.set("bundleDescription", null);
		manifest.set("bundleSymbolicName", map().set("symbolicName", "org.name"));
		project.set("commonInfo", map().set("name", "CName")
			.set("description", "CDescription"));
		project.set("fileName", "folder1");
		project.set("bundles", list(bundle.set("manifest", manifest)));

		checker.with(map().set("projects", list(project)))
			.expect("* [**CName**](folder1): CDescription")
			.check();

		manifest.set("bundleName", null);
		manifest.set("bundleDescription", null);
		manifest.set("bundleSymbolicName", map().set("symbolicName", "org.name"));
		project.set("commonInfo", null);
		project.set("fileName", "folder1");
		project.set("bundles", list(bundle.set("manifest", manifest)));

		checker.with(map().set("projects", list(project)))
			.expect("* [**org.name**](folder1)")
			.check();

		manifest.set("bundleName", null);
		manifest.set("bundleDescription", null);
		manifest.set("bundleSymbolicName", null);
		project.set("commonInfo", null);
		project.set("fileName", "folder1");
		project.set("bundles", list(bundle.set("manifest", manifest)));

		checker.with(map().set("projects", list(project)))
			.expect("* [**folder1**](folder1)")
			.check();

		manifest.set("bundleName", "Name");
		manifest.set("bundleDescription", null);
		manifest.set("bundleSymbolicName", null);
		project.set("commonInfo", null);
		project.set("fileName", null);
		project.set("bundles", list(bundle.set("manifest", manifest)));

		checker.with(map().set("projects", list(project)))
			.expect("* [**Name**]()")
			.check();

		// Nested bundles

		MapBuilder bundle2 = map();
		MapBuilder manifest2 = map();

		manifest.set("bundleName", "Name");
		manifest.set("bundleDescription", "Description");
		manifest.set("bundleSymbolicName", map().set("symbolicName", "org.dom.one"));
		manifest2.set("bundleName", null);
		manifest2.set("bundleDescription", null);
		manifest2.set("bundleSymbolicName", map().set("symbolicName", "org.otherdom.two"));
		project.set("commonInfo", map().set("name", "CName")
			.set("description", "CDescription"));
		project.set("fileName", "org.dom");
		project.set("bundles", list(bundle.set("manifest", manifest), bundle2.set("manifest", manifest2)));

		checker.with(map().set("projects", list(project)))
			.expect("* [**CName**](org.dom): CDescription")
			.expect("  * [**Name**](org.dom/readme.one.md): Description")
			.expect("  * [**org.otherdom.two**](org.dom/readme.org.otherdom.two.md)")
			.check();

		// Nested projects

		MapBuilder project2 = map();
		MapBuilder project3 = map();

		manifest.set("bundleName", "Name");
		manifest.set("bundleDescription", "Description");
		manifest2.set("bundleName", "Name2");
		manifest2.set("bundleDescription", "Description2");
		project.set("commonInfo", map().set("name", "CName")
			.set("description", "CDescription"));
		project.set("fileName", "folder1");
		project2.set("fileName", "folder2");
		project2.set("bundles", list(map().set("manifest", manifest)));
		project3.set("fileName", "folder3");
		project3.set("bundles", list(map().set("manifest", manifest2)));

		project.set("projects", list(project2, project3));

		checker.with(map().set("projects", list(project)))
			.expect("* [**CName**](folder1): CDescription")
			.expect("  * [**Name**](folder1/folder2): Description")
			.expect("  * [**Name2**](folder1/folder3): Description2")
			.check();

		// Multiple Nested Project

		MapBuilder manifest3 = map();
		MapBuilder project4 = map();
		MapBuilder project5 = map();

		manifest.set("bundleName", "Name");
		manifest.set("bundleDescription", "Description");
		manifest2.set("bundleName", "Name2");
		manifest2.set("bundleDescription", "Description2");
		manifest3.set("bundleName", "Name3");
		manifest3.set("bundleDescription", "Description3");

		project.clear();
		project2.clear();
		project3.clear();

		project.set("fileName", "folder1");
		project.set("commonInfo", map().set("name", "CName")
			.set("description", "CDescription"));
		project.set("projects", list(project2));

		project2.set("fileName", "folder2");
		project2.set("commonInfo", map().set("name", "CName2")
			.set("description", "CDescription2"));
		project2.set("projects", list(project3, project4));

		project3.set("fileName", "folder3");
		project3.set("bundles", list(map().set("manifest", manifest)));

		project4.set("fileName", "folder4");
		project4.set("bundles", list(map().set("manifest", manifest2)));

		project5.set("fileName", "folder1");
		project5.set("bundles", list(map().set("manifest", manifest3)));

		checker.with(map().set("projects", list(project, project5)))
			.expect("* [**CName**](folder1): CDescription")
			.expect("  * [**CName2**](folder1/folder2): CDescription2")
			.expect("    * [**Name**](folder1/folder2/folder3): Description")
			.expect("    * [**Name2**](folder1/folder2/folder4): Description2")
			.expect("* [**Name3**](folder1): Description3")
			.check();
	}

	public void testCodeSnippets() throws Exception {
		TwigChecker checker = checker("printCodeSnippets");

		checker.with(list())
			.check();

		checker.with(list().xadd(map().set("programmingLanguage", "java")
			.set("codeSnippet", "test")))
			.expect("```java")
			.expect("test")
			.expect("```")
			.check();

		checker.with(list().xadd(map().set("programmingLanguage", "java")
			.set("codeSnippet", "test"))
			.xadd(map().set("programmingLanguage", "java")
				.set("codeSnippet", "test2")))
			.expect("```java")
			.expect("test")
			.expect("```")
			.expectBlankLine()
			.expect("```java")
			.expect("test2")
			.expect("```")
			.check();

		checker.with(list().xadd(map().set("steps", list().xadd(map().set("programmingLanguage", "java")
			.set("codeSnippet", "test1"))
			.xadd(map().set("programmingLanguage", "java")
				.set("codeSnippet", "test2"))))
			.xadd(map().set("programmingLanguage", "java")
				.set("codeSnippet", "test3")))
			.expect("```java")
			.expect("test1")
			.expect("```")
			.expectBlankLine()
			.expect("```java")
			.expect("test2")
			.expect("```")
			.expectBlankLine()
			.expect("```java")
			.expect("test3")
			.expect("```")
			.check();

		checker.with(list().xadd(map().set("title", "Title1")
			.set("programmingLanguage", "java")
			.set("codeSnippet", "test1"))
			.xadd(map().set("title", "Title2")
				.set("description", "Description 2")
				.set("steps", list().xadd(map().set("title", "SubTitle1")
					.set("description", "SubDescription 1")
					.set("programmingLanguage", "java")
					.set("codeSnippet", "test1"))
					.xadd(map().set("title", "SubTitle2")
						.set("description", "SubDescription 2")
						.set("programmingLanguage", "java")
						.set("codeSnippet", "test2"))))
			.xadd(map().set("title", "Title3")
				.set("description", "Description 3")
				.set("programmingLanguage", "java")
				.set("codeSnippet", "test1")))
			.expect("### Title1")
			.expectBlankLine()
			.expect("```java")
			.expect("test1")
			.expect("```")
			.expectBlankLine()
			.expect("### Title2")
			.expectBlankLine()
			.expect("Description 2")
			.expectBlankLine()
			.expect("#### SubTitle1")
			.expectBlankLine()
			.expect("SubDescription 1")
			.expectBlankLine()
			.expect("```java")
			.expect("test1")
			.expect("```")
			.expectBlankLine()
			.expect("#### SubTitle2")
			.expectBlankLine()
			.expect("SubDescription 2")
			.expectBlankLine()
			.expect("```java")
			.expect("test2")
			.expect("```")
			.expectBlankLine()
			.expect("### Title3")
			.expectBlankLine()
			.expect("Description 3")
			.expectBlankLine()
			.expect("```java")
			.expect("test1")
			.expect("```")
			.check();

		checker.with(list().xadd(map().set("title", "Title1")
			.set("programmingLanguage", "java")
			.set("codeSnippet", "test1"))
			.xadd(map().set("title", "Title2")
				.set("description", "Description 2")
				.set("steps", list().xadd(map().set("title", "SubTitle1")
					.set("description", "SubDescription 1")
					.set("programmingLanguage", "java")
					.set("codeSnippet", "test1"))
					.xadd(map().set("title", "SubTitle2")
						.set("description", "SubDescription 2")
						.set("programmingLanguage", "java")
						.set("codeSnippet", "test2"))))
			.xadd(map().set("title", "Title3")
				.set("description", "Description 3")
				.set("programmingLanguage", "java")
				.set("codeSnippet", "test1")))
			.with(1)
			.expect("# Title1")
			.expectBlankLine()
			.expect("```java")
			.expect("test1")
			.expect("```")
			.expectBlankLine()
			.expect("# Title2")
			.expectBlankLine()
			.expect("Description 2")
			.expectBlankLine()
			.expect("## SubTitle1")
			.expectBlankLine()
			.expect("SubDescription 1")
			.expectBlankLine()
			.expect("```java")
			.expect("test1")
			.expect("```")
			.expectBlankLine()
			.expect("## SubTitle2")
			.expectBlankLine()
			.expect("SubDescription 2")
			.expectBlankLine()
			.expect("```java")
			.expect("test2")
			.expect("```")
			.expectBlankLine()
			.expect("# Title3")
			.expectBlankLine()
			.expect("Description 3")
			.expectBlankLine()
			.expect("```java")
			.expect("test1")
			.expect("```")
			.check();
	}

	public void testVendor() throws Exception {
		TwigChecker checker = checker("printVendor");

		checker.with("vendor")
			.expect("vendor")
			.check();

		checker.with("vendor")
			.with(map().set("address", "mypostal"))
			.expect("vendor - mypostal")
			.check();

		checker.with("vendor")
			.with(map().set("type", "postal")
				.set("address", "mypostal"))
			.expect("vendor - mypostal")
			.check();

		checker.with("vendor")
			.with(map().set("type", "email")
				.set("address", "myemail"))
			.expect("vendor - [myemail](mailto:myemail)")
			.check();

		checker.with("vendor")
			.with(map().set("type", "url")
				.set("address", "myurl"))
			.expect("vendor - [myurl](myurl)")
			.check();

		checker.with("")
			.with(map().set("type", "url")
				.set("address", "myurl"))
			.expect("[myurl](myurl)")
			.check();
	}

	static public TwigChecker checker(String functionName) {
		return new TwigChecker("default:printer.twig", functionName);
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
