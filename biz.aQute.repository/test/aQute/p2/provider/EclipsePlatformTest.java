package aQute.p2.provider;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.CapReqBuilder;

@ExtendWith(SoftAssertionsExtension.class)
public class EclipsePlatformTest {

	@InjectSoftAssertions
	SoftAssertions							softly;

	private static final Map<String, String>	WIN32		= Map.of("osgi.os", "win32", "osgi.ws", "win32",
		"osgi.arch", "x86_64");
	private static final Map<String, String>	ANYWHERE	= Map.of("osgi.os", "*", "osgi.ws", "*", "osgi.arch", "*");

	private static final String				SWT_WIN32	= "(& (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86_64))";
	private static final String				SWT_MACOSX	= "(& (osgi.ws=cocoa) (osgi.os=macosx) (osgi.arch=x86_64))";

	private static Requirement member(String relation, String os, String ws, String arch) {
		CapReqBuilder builder = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=member)");
		builder.addAttribute(Feature.RELATION_ATTRIBUTE, relation);
		builder.addAttribute("os", os);
		builder.addAttribute("ws", ws);
		builder.addAttribute("arch", arch);
		return builder.buildSyntheticRequirement();
	}

	private static Map<String, Object> attributes(EclipsePlatform platform) {
		return platform.capability()
			.getAttributes();
	}

	@SuppressWarnings("unchecked")
	private static List<String> list(Object value) {
		return (List<String>) value;
	}

	@Test
	public void testRunPropertiesDefineThePlatform() {
		softly.assertThat(attributes(EclipsePlatform.of(WIN32)))
			.containsEntry("osgi.os", "win32")
			.containsEntry("osgi.ws", "win32")
			.containsEntry("osgi.arch", "x86_64");
	}

	@Test
	public void testWsIsDerivedFromOs() {
		softly.assertThat(attributes(EclipsePlatform.of(Map.of("osgi.os", "macosx", "osgi.arch", "aarch64"))))
			.containsEntry("osgi.os", "macosx")
			.containsEntry("osgi.ws", "cocoa")
			.containsEntry("osgi.arch", "aarch64");
		softly.assertThat(attributes(EclipsePlatform.of(Map.of("osgi.os", "linux"))))
			.containsEntry("osgi.ws", "gtk");
		softly.assertThat(attributes(EclipsePlatform.of(Map.of("osgi.os", "macosx", "osgi.ws", "gtk"))))
			.as("an explicit ws wins")
			.containsEntry("osgi.ws", "gtk");
	}

	@Test
	public void testHostIsTheDefault() {
		String os = EclipsePlatform.toOs(System.getProperty("os.name"));
		Map<String, Object> expected = Map.of("osgi.os", os, "osgi.ws", EclipsePlatform.toWs(os), "osgi.arch",
			EclipsePlatform.toArch(System.getProperty("os.arch")));

		softly.assertThat(attributes(EclipsePlatform.of(Map.of())))
			.containsAllEntriesOf(expected);
		softly.assertThat(attributes(EclipsePlatform.of(null)))
			.containsAllEntriesOf(expected);
		softly.assertThat(attributes(EclipsePlatform.of(Map.of("osgi.os", " "))))
			.as("blank is not set")
			.containsAllEntriesOf(expected);
	}

	@Test
	public void testOsNames() {
		softly.assertThat(EclipsePlatform.toOs("Windows 11"))
			.isEqualTo("win32");
		softly.assertThat(EclipsePlatform.toOs("Mac OS X"))
			.isEqualTo("macosx");
		softly.assertThat(EclipsePlatform.toOs("Darwin"))
			.as("darwin contains win")
			.isEqualTo("macosx");
		softly.assertThat(EclipsePlatform.toOs("Linux"))
			.isEqualTo("linux");
	}

	@Test
	public void testMembersOfOtherPlatformsDoNotMatch() {
		EclipsePlatform platform = EclipsePlatform.of(WIN32);

		for (String relation : new String[] {
			Feature.RELATION_PLUGIN, Feature.RELATION_INCLUDE
		}) {
			softly.assertThat(platform.matches(member(relation, "win32", "win32", "x86_64")))
				.as(relation + " same platform")
				.isTrue();
			softly.assertThat(platform.matches(member(relation, "macosx", "cocoa", "x86_64")))
				.as(relation + " other os")
				.isFalse();
			softly.assertThat(platform.matches(member(relation, "win32", "win32", "aarch64")))
				.as(relation + " other arch")
				.isFalse();
			softly.assertThat(platform.matches(member(relation, "win32", "gtk", "x86_64")))
				.as(relation + " other ws")
				.isFalse();
			softly.assertThat(platform.matches(member(relation, "win32,linux", null, null)))
				.as(relation + " os list with the platform")
				.isTrue();
			softly.assertThat(platform.matches(member(relation, "linux, macosx", null, null)))
				.as(relation + " os list without the platform")
				.isFalse();
			softly.assertThat(platform.matches(member(relation, null, null, null)))
				.as(relation + " no coordinates")
				.isTrue();
		}
	}

	@Test
	public void testOtherRequirementsAlwaysMatch() {
		EclipsePlatform platform = EclipsePlatform.of(WIN32);

		softly.assertThat(platform.matches(member(Feature.RELATION_REQUIRE, "macosx", "cocoa", "x86_64")))
			.as("a requires import is not a member")
			.isTrue();

		CapReqBuilder unmarked = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=member)");
		unmarked.addAttribute("os", "macosx");
		softly.assertThat(platform.matches(unmarked.buildSyntheticRequirement()))
			.as("not a feature requirement")
			.isTrue();

		CapReqBuilder otherNamespace = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.wiring.package=a)");
		otherNamespace.addAttribute(Feature.RELATION_ATTRIBUTE, Feature.RELATION_PLUGIN);
		otherNamespace.addAttribute("os", "macosx");
		softly.assertThat(platform.matches(otherNamespace.buildSyntheticRequirement()))
			.as("not an identity requirement")
			.isTrue();
	}

	@Test
	public void testWildcardMatchesEverything() {
		EclipsePlatform platform = EclipsePlatform.of(ANYWHERE);

		softly.assertThat(platform.matches(member(Feature.RELATION_PLUGIN, "macosx", "cocoa", "aarch64")))
			.isTrue();
		softly.assertThat(platform.matches(member(Feature.RELATION_PLUGIN, "win32", "win32", "x86_64")))
			.isTrue();

		Map<String, Object> attributes = attributes(platform);
		softly.assertThat(list(attributes.get("osgi.os")))
			.contains("win32", "linux", "macosx");
		softly.assertThat(list(attributes.get("osgi.ws")))
			.contains("win32", "gtk", "cocoa");
		softly.assertThat(list(attributes.get("osgi.arch")))
			.contains("x86_64", "aarch64");

		Map<String, String> onlyOs = Map.of("osgi.os", "*");
		softly.assertThat(attributes(EclipsePlatform.of(onlyOs)).get("osgi.ws"))
			.as("ws follows a wildcard os")
			.isInstanceOf(List.class);
	}

	@Test
	public void testCapabilitySatisfiesPlatformFilters() throws Exception {
		Capability win32 = EclipsePlatform.of(WIN32)
			.capability();
		Capability anywhere = EclipsePlatform.of(ANYWHERE)
			.capability();

		softly.assertThat(FrameworkUtil.createFilter(SWT_WIN32)
			.matches(win32.getAttributes()))
			.isTrue();
		softly.assertThat(FrameworkUtil.createFilter(SWT_MACOSX)
			.matches(win32.getAttributes()))
			.isFalse();
		softly.assertThat(FrameworkUtil.createFilter(SWT_WIN32)
			.matches(anywhere.getAttributes()))
			.isTrue();
		softly.assertThat(FrameworkUtil.createFilter(SWT_MACOSX)
			.matches(anywhere.getAttributes()))
			.isTrue();
	}

	@Test
	public void testRequirementFromPlatformFilter() {
		CapReqBuilder requirement = EclipsePlatform.requirement(SWT_WIN32);

		softly.assertThat(requirement)
			.isNotNull();
		softly.assertThat(requirement.getNamespace())
			.isEqualTo(EclipsePlatform.NAMESPACE);
		softly.assertThat(requirement.buildSyntheticRequirement()
			.getDirectives())
			.containsEntry(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(&(osgi.ws=win32)(osgi.os=win32)(osgi.arch=x86_64))");

		softly.assertThat(EclipsePlatform.requirement("(!(osgi.os=win32))"))
			.isNotNull();
	}

	@Test
	public void testPlatformFilterThatCannotBeEvaluatedIsNotIndexed() {
		softly.assertThat(EclipsePlatform.requirement(null))
			.isNull();
		softly.assertThat(EclipsePlatform.requirement(" "))
			.isNull();
		softly.assertThat(EclipsePlatform.requirement("(& (osgi.os=win32"))
			.as("invalid")
			.isNull();
		softly.assertThat(EclipsePlatform.requirement("(osgi.nl=de)"))
			.as("locale")
			.isNull();
		softly.assertThat(EclipsePlatform.requirement("(&(osgi.os=win32)(osgi.nl=de))"))
			.as("os and locale")
			.isNull();
		softly.assertThat(EclipsePlatform.requirement("(osgi.OS=win32)"))
			.as("not the name of a platform coordinate")
			.isNull();
	}
}
