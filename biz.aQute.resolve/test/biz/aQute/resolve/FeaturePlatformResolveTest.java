package biz.aQute.resolve;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.p2.provider.EclipsePlatform;
import aQute.p2.provider.Feature;
import test.lib.MockRegistry;
import test.lib.NullLogService;

/**
 * An Eclipse feature lists the launcher/SWT fragments of every platform. Only
 * the ones of the platform of the run (-runproperties) may be resolved.
 */
@ExtendWith(SoftAssertionsExtension.class)
public class FeaturePlatformResolveTest {

	@InjectSoftAssertions
	SoftAssertions					softly;

	private final LogService		log			= new NullLogService();

	private static final String		WIN32		= "osgi.os=win32,osgi.ws=win32,osgi.arch=x86_64";
	private static final String		MACOSX		= "osgi.os=macosx,osgi.ws=cocoa,osgi.arch=x86_64";
	private static final String		ANYWHERE	= "osgi.os=*,osgi.ws=*,osgi.arch=*";

	private static final String		FEATURE		= "osgi.identity;filter:='(&(osgi.identity=test.feature)(type=org.eclipse.update.feature))'";
	private static final String		BLACKLIST	= "osgi.identity;filter:='(osgi.identity=*macosx*)',osgi.identity;filter:='(osgi.identity=*linux*)'";

	/**
	 * @param withPlatformFilters the fragments declare their platform like the
	 *            index of a p2 repository does since Eclipse-PlatformFilter is
	 *            indexed
	 */
	private static ResourcesRepository repository(boolean withPlatformFilters) throws Exception {
		ResourcesRepository repository = new ResourcesRepository();
		repository.add(bundle("test.host", null, null));
		repository.add(fragment("test.frag.win32", "win32", "win32", withPlatformFilters));
		repository.add(fragment("test.frag.macosx", "macosx", "cocoa", withPlatformFilters));
		repository.add(fragment("test.frag.linux", "linux", "gtk", withPlatformFilters));
		repository.add(bundle("test.consumer", "test.swt", null));
		repository.add(feature());
		return repository;
	}

	private static Resource bundle(String bsn, String importPackage, String platformFilter) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Bundle-SymbolicName", bsn);
		headers.put("Bundle-Version", "1.0.0");
		if (importPackage != null)
			headers.put("Import-Package", importPackage);
		return build(headers, platformFilter);
	}

	private static Resource fragment(String bsn, String os, String ws, boolean withPlatformFilter) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Bundle-SymbolicName", bsn);
		headers.put("Bundle-Version", "1.0.0");
		headers.put("Fragment-Host", "test.host;bundle-version=\"[1.0,2)\"");
		headers.put("Export-Package", "test.swt;version=1.0.0");
		String filter = "(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=x86_64))";
		return build(headers, withPlatformFilter ? filter : null);
	}

	private static Resource build(Map<String, String> headers, String platformFilter) {
		ResourceBuilder builder = new ResourceBuilder();
		builder.addManifest(Domain.domain(headers));
		if (platformFilter != null)
			builder.addRequirement(EclipsePlatform.requirement(platformFilter));
		return builder.build();
	}

	private static Resource feature() throws Exception {
		String xml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<feature id="test.feature" label="Test" version="1.0.0">
			  <plugin id="test.host" version="1.0.0" unpack="false"/>
			  <plugin id="test.frag.win32" version="1.0.0" os="win32" ws="win32" arch="x86_64" fragment="true"/>
			  <plugin id="test.frag.macosx" version="1.0.0" os="macosx" ws="cocoa" arch="x86_64" fragment="true"/>
			  <plugin id="test.frag.linux" version="1.0.0" os="linux" ws="gtk" arch="x86_64" fragment="true"/>
			</feature>
			""";
		try (Jar jar = new Jar("test.feature")) {
			jar.putResource("feature.xml", new EmbeddedResource(xml.getBytes(UTF_8), 0L));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			jar.write(out);

			Feature feature = new Feature(new ByteArrayInputStream(out.toByteArray()));
			feature.parse();
			return feature.toResource();
		}
	}

	private Set<String> resolve(ResourcesRepository repository, String requirement, String runProperties,
		String blacklist) throws Exception {
		Processor model = new Processor();
		model.setProperty("-runrequires", requirement);
		model.setProperty("-runproperties", runProperties);
		if (blacklist != null)
			model.setProperty("-runblacklist", blacklist);

		BndrunResolveContext context = new BndrunResolveContext(model, null, new MockRegistry(), log);
		context.addRepository(repository);
		context.setLevel(0);
		context.init();
		try (ResolverLogger logger = new ResolverLogger(4)) {
			Resolver resolver = new BndResolver(logger);
			Map<Resource, List<Wire>> resolved = resolver.resolve(context);
			return resolved.keySet()
				.stream()
				.map(ResourceUtils::getIdentityCapability)
				.filter(Objects::nonNull)
				.map(IdentityCapability::osgi_identity)
				.collect(toSet());
		}
	}

	@Test
	public void testFeatureResolvesWithTheMembersOfThePlatform() throws Exception {
		for (boolean withPlatformFilters : new boolean[] {
			true, false
		}) {
			Set<String> resolved = resolve(repository(withPlatformFilters), FEATURE, WIN32, BLACKLIST);

			softly.assertThat(resolved)
				.as("platform filters on the fragments: " + withPlatformFilters)
				.contains("test.feature", "test.host", "test.frag.win32")
				.doesNotContain("test.frag.macosx", "test.frag.linux");
		}
	}

	@Test
	public void testPlatformOfTheRunSelectsTheMembers() throws Exception {
		for (boolean withPlatformFilters : new boolean[] {
			true, false
		}) {
			Set<String> resolved = resolve(repository(withPlatformFilters), FEATURE, MACOSX, null);

			softly.assertThat(resolved)
				.as("platform filters on the fragments: " + withPlatformFilters)
				.contains("test.feature", "test.host", "test.frag.macosx")
				.doesNotContain("test.frag.win32", "test.frag.linux");
		}
	}

	@Test
	public void testMemberOfThePlatformCanStillBeBlacklisted() throws Exception {
		String blacklist = "osgi.identity;filter:='(osgi.identity=test.frag.win32)'";

		assertThatThrownBy(() -> resolve(repository(true), FEATURE, WIN32, blacklist))
			.isInstanceOf(ResolutionException.class);
	}

	@Test
	public void testWildcardKeepsAllPlatforms() throws Exception {
		for (boolean withPlatformFilters : new boolean[] {
			true, false
		}) {
			Set<String> resolved = resolve(repository(withPlatformFilters), FEATURE, ANYWHERE, null);

			softly.assertThat(resolved)
				.as("platform filters on the fragments: " + withPlatformFilters)
				.contains("test.feature", "test.host", "test.frag.win32", "test.frag.macosx", "test.frag.linux");
		}
	}

	@Test
	public void testFragmentOfOtherPlatformIsNotResolvedOnItsOwn() throws Exception {
		String macosx = "osgi.identity;filter:='(osgi.identity=test.frag.macosx)'";

		assertThatThrownBy(() -> resolve(repository(true), macosx, WIN32, null))
			.isInstanceOf(ResolutionException.class);

		softly.assertThat(resolve(repository(true), macosx, MACOSX, null))
			.contains("test.host", "test.frag.macosx");
		softly.assertThat(resolve(repository(false), macosx, WIN32, null))
			.as("without a platform filter a requirement made directly is not touched")
			.contains("test.host", "test.frag.macosx");
	}

	@Test
	public void testPackageImportDoesNotAttachFragmentsOfOtherPlatforms() throws Exception {
		String consumer = "osgi.identity;filter:='(osgi.identity=test.consumer)'";

		softly.assertThat(resolve(repository(true), consumer, WIN32, null))
			.contains("test.consumer", "test.host", "test.frag.win32")
			.doesNotContain("test.frag.macosx", "test.frag.linux");
	}

	@Test
	public void testDebugOutputShowsThePlatform() throws Exception {
		Processor model = new Processor();
		model.setProperty("-runrequires", FEATURE);
		model.setProperty("-runproperties", WIN32);
		BndrunResolveContext context = new BndrunResolveContext(model, null, new MockRegistry(), log);
		context.addRepository(repository(true));
		context.init();

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		new DebugReporter(new PrintStream(bytes, true, UTF_8), context, 1).report();

		softly.assertThat(bytes.toString(UTF_8))
			.contains("TARGET PLATFORM")
			.contains("osgi.os=win32, osgi.ws=win32, osgi.arch=x86_64");
	}
}
