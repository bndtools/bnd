package org.osgi.service.indexer.impl;

import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.log.LogService;

import junit.framework.TestCase;

public class TestIndexer extends TestCase {

	/**
	 * Test if the resolver is not affected if we use a dummy resolver
	 */
	@SuppressWarnings("deprecation")
	public void testResolverUnaffected() throws Exception {

		RepoIndex indexer = new RepoIndex();
		String without = index(indexer);

		indexer = new RepoIndex();
		indexer.setURLResolver(new URLResolver() {

			@Override
			public URI resolver(File artifact) throws Exception {
				return null;
			}
		});

		String with = index(indexer);

		assertEquals("Should be the same without a resolver or a resolver that returns null", without, with);

		indexer = new RepoIndex();
		indexer.setURLResolver(new URLResolver() {

			@Override
			public URI resolver(File artifact) throws Exception {
				throw new Exception();
			}
		});

		with = index(indexer);
		assertEquals("Should be the same without a resolver or a resolver that returns null", without, with);
	}

	/**
	 * Test if the resolver can change the URLs
	 */
	@SuppressWarnings("deprecation")
	public void testResolverEffect() throws Exception {

		RepoIndex indexer = new RepoIndex();
		indexer.setURLResolver(new URLResolver() {

			@Override
			public URI resolver(File artifact) throws Exception {
				return new URI("xyz://FOOBAR/" + artifact.getName());
			}
		});

		String with = index(indexer);

		Pattern p = Pattern.compile("xyz://FOOBAR/(03-export.jar|06-requirebundle.jar)(?:\"|')");
		Matcher m = p.matcher(with);
		int n = 0;
		while (m.find()) {
			n++;
		}
		assertEquals("There are two files indexed", 2, n);
	}

	public void testFragmentBsnVersion() throws Exception {
		assertFragmentMatch("testdata/fragment-01.txt", "testdata/01-bsn+version.jar");
	}

	public void testFragmentBsnVersionWithScrewyPath() throws Exception {
		assertFragmentMatch("testdata/fragment-01.txt", "testdata/../testdata/01-bsn+version.jar");
	}

	public void testFragmentBsnVersionWithBundleOutsideTheParentPath() throws Exception {
		RepoIndex indexer = new RepoIndex();

		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("generated/../testdata/01-bsn+version.jar")), writer,
				singletonMap(RepoIndex.ROOT_URL, new File("testdata").getAbsoluteFile().toURI().toURL().toString()));

		String expected = Utils.readStream(new FileInputStream("testdata/fragment-01-relative.txt"));
		assertEquals(expected, writer.toString().trim());
	}

	public void testFragmentLocalization() throws Exception {
		assertFragmentMatch("testdata/fragment-02.txt", "testdata/02-localization.jar");
	}

	public void testFragmentExport() throws Exception {
		assertFragmentMatch("testdata/fragment-03.txt", "testdata/03-export.jar");
	}

	public void testFragmentExportWithUses() throws Exception {
		assertFragmentMatch("testdata/fragment-04.txt", "testdata/04-export+uses.jar");
	}

	public void testFragmentImport() throws Exception {
		assertFragmentMatch("testdata/fragment-05.txt", "testdata/05-import.jar");
	}

	public void testFragmentRequireBundle() throws Exception {
		assertFragmentMatch("testdata/fragment-06.txt", "testdata/06-requirebundle.jar");
	}

	public void testFragmentOptionalImport() throws Exception {
		assertFragmentMatch("testdata/fragment-07.txt", "testdata/07-optionalimport.jar");
	}

	public void testFragmentFragmentHost() throws Exception {
		assertFragmentMatch("testdata/fragment-08.txt", "testdata/08-fragmenthost.jar");
	}

	public void testFragmentSingletonBundle() throws Exception {
		assertFragmentMatch("testdata/fragment-09.txt", "testdata/09-singleton.jar");
	}

	public void testFragmentExportService() throws Exception {
		assertFragmentMatch("testdata/fragment-10.txt", "testdata/10-exportservice.jar");
	}

	public void testFragmentImportService() throws Exception {
		assertFragmentMatch("testdata/fragment-11.txt", "testdata/11-importservice.jar");
	}

	public void testFragmentForbidFragments() throws Exception {
		assertFragmentMatch("testdata/fragment-12.txt", "testdata/12-nofragments.jar");
	}

	public void testFragmentBREE() throws Exception {
		assertFragmentMatch("testdata/fragment-13.txt", "testdata/13-bree.jar");
	}

	public void testFragmentProvideRequireCap() throws Exception {
		assertFragmentMatch("testdata/fragment-14.txt", "testdata/14-provide-require-cap.jar");
	}

	public void testFragmentRequireSCR() throws Exception {
		assertFragmentMatch("testdata/fragment-15.txt", "testdata/15-scr.jar");
	}

	public void testFragmentOptionalRequireBundle() throws Exception {
		assertFragmentMatch("testdata/fragment-16.txt", "testdata/16-optionalrequirebundle.jar");
	}

	public void testFragmentRequireSCR1_0() throws Exception {
		assertFragmentMatch("testdata/fragment-scr1_0.txt", "testdata/scr1_0.jar");
	}

	public void testFragmentRequireSCR1_1() throws Exception {
		assertFragmentMatch("testdata/fragment-scr1_1.txt", "testdata/scr1_1.jar");
	}

	public void testFragmentRequireSCR1_2() throws Exception {
		assertFragmentMatch("testdata/fragment-scr1_2.txt", "testdata/scr1_2.jar");
	}

	public void testFragmentSCRServices() throws Exception {
		assertFragmentMatch("testdata/fragment-scr_services.txt", "testdata/scr_services.jar");
	}

	private static void assertFragmentMatch(String expectedPath, String jarPath) throws Exception {
		RepoIndex indexer = new RepoIndex();
		assertFragmentMatch(indexer, expectedPath, jarPath);
	}

	private static void assertFragmentMatch(RepoIndex indexer, String expectedPath, String jarPath) throws Exception {
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File(jarPath)), writer, null);

		String expected = Utils.readStream(new FileInputStream(expectedPath));
		assertEquals(expected, writer.toString().trim());
	}

	public void testEmptyIndex() throws Exception {
		RepoIndex indexer = new RepoIndex();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Set<File> files = Collections.emptySet();

		Map<String,String> config = new HashMap<>();
		config.put(RepoIndex.REPOSITORY_INCREMENT_OVERRIDE, "0");
		config.put(ResourceIndexer.REPOSITORY_NAME, "empty");
		config.put(ResourceIndexer.PRETTY, "true");
		indexer.index(files, out, config);

		String expected = Utils.readStream(new FileInputStream("testdata/empty.txt"));
		assertEquals(expected, out.toString());
	}

	public void testFullIndex() throws Exception {
		RepoIndex indexer = new RepoIndex();

		String decompressed = index(indexer);

		String unpackedXML = Utils.readStream(new FileInputStream("testdata/unpacked.xml"));
		String expected = unpackedXML.replaceAll("\\r?\\n|\\t", "");
		assertEquals(expected, decompressed);
	}

	private String index(RepoIndex indexer) throws Exception, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Set<File> files = new LinkedHashSet<>();
		files.add(new File("testdata/03-export.jar"));
		files.add(new File("testdata/06-requirebundle.jar"));

		Map<String,String> config = new HashMap<>();
		config.put(RepoIndex.REPOSITORY_INCREMENT_OVERRIDE, "0");
		config.put(ResourceIndexer.REPOSITORY_NAME, "full-c+f");
		indexer.index(files, out, config);

		String decompressed = Utils.decompress(out.toByteArray());
		return decompressed;
	}

	public void testFullIndexPrettyCompressedPermutations() throws Exception {
		Boolean pretties[] = {
				null, Boolean.FALSE, Boolean.TRUE
		};
		Boolean compressions[] = {
				null, Boolean.FALSE, Boolean.TRUE
		};

		boolean outPretties[] = {
				false, false, false, true, false, false, true, true, true
		};
		boolean outCompressions[] = {
				true, false, true, false, false, true, false, false, true
		};

		String expectedPretty = Utils.readStream(new FileInputStream("testdata/full-03+06.txt"));
		String expectedNotPretty = Utils.readStream(new FileInputStream("testdata/full-03+06-not-pretty.txt"));

		RepoIndex indexer = new RepoIndex();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Set<File> files = new LinkedHashSet<>();
		files.add(new File("testdata/03-export.jar"));
		files.add(new File("testdata/06-requirebundle.jar"));

		Map<String,String> config = new HashMap<>();

		int outIndex = 0;
		for (Boolean pretty : pretties) {
			for (Boolean compression : compressions) {
				config.put(RepoIndex.REPOSITORY_INCREMENT_OVERRIDE, "0");
				config.put(ResourceIndexer.REPOSITORY_NAME, "full-c+f");
				if (pretty != null) {
					config.put(ResourceIndexer.PRETTY, pretty.toString().toLowerCase());
				}
				if (compression != null) {
					config.put(ResourceIndexer.COMPRESSED, compression.toString().toLowerCase());
				}
				indexer.index(files, out, config);

				String expected = outPretties[outIndex] ? expectedPretty : expectedNotPretty;
				if (!outCompressions[outIndex]) {
					assertEquals("pretty/compression = " + pretty + "/" + compression, expected, out.toString());
				} else {
					assertEquals("pretty/compression = " + pretty + "/" + compression, expected,
							Utils.decompress(out.toByteArray()));
				}

				config.clear();
				out.reset();
				outIndex++;
			}
		}
	}

	public void testAddAnalyzer() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new WibbleAnalyzer(), null);

		StringWriter writer = new StringWriter();
		LinkedHashSet<File> files = new LinkedHashSet<>();
		files.add(new File("testdata/01-bsn+version.jar"));
		files.add(new File("testdata/02-localization.jar"));

		indexer.indexFragment(files, writer, null);
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-wibble.txt"));

		assertEquals(expected, writer.toString().trim());
	}

	public void testAddAnalyzerWithFilter() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new WibbleAnalyzer(), FrameworkUtil.createFilter("(location=*sion.jar)"));

		StringWriter writer = new StringWriter();
		LinkedHashSet<File> files = new LinkedHashSet<>();
		files.add(new File("testdata/01-bsn+version.jar"));
		files.add(new File("testdata/02-localization.jar"));

		indexer.indexFragment(files, writer, null);
		String expected = Utils.readStream(new FileInputStream("testdata/fragment-wibble-filtered.txt"));

		assertEquals(expected, writer.toString().trim());
	}

	public void testAnalyzerException() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new BadAnalyzer(), null);

		StringWriter writer = new StringWriter();
		LinkedHashSet<File> files = new LinkedHashSet<>();
		files.add(new File("testdata/01-bsn+version.jar"));
		files.add(new File("testdata/02-localization.jar"));

		indexer.indexFragment(files, writer, null);

		String xml = writer.toString().trim();
		String comment = "  <!-- Error calling analyzer \"org.osgi.service.indexer.impl.BadAnalyzer\" on resource testdata/01-bsn+version.jar with message java.lang.IllegalStateException: Bwa Ha Ha Ha! and stack: java.lang.IllegalStateException: Bwa Ha Ha Ha!";
		assertTrue(String.format("Did not contain the correct comment %s in %s", comment, xml), xml.contains(comment));

		comment = "  <!-- Error calling analyzer \"org.osgi.service.indexer.impl.BadAnalyzer\" on resource testdata/02-localization.jar with message java.lang.IllegalStateException: Bwa Ha Ha Ha! and stack: java.lang.IllegalStateException: Bwa Ha Ha Ha!";
		assertTrue(String.format("Did not contain the correct comment %s in %s", comment, xml), xml.contains(comment));
	}

	public void testRootInSubdirectory() throws Exception {
		RepoIndex indexer = new RepoIndex();

		Map<String,String> props = new HashMap<>();
		props.put(ResourceIndexer.ROOT_URL, new File("testdata").getAbsoluteFile().toURI().toURL().toString());

		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/01-bsn+version.jar")), writer, props);

		String expected = Utils.readStream(new FileInputStream("testdata/fragment-subdir1.txt"));
		assertEquals(expected, writer.toString().trim());
	}

	public void testRootInSubSubdirectory() throws Exception {
		RepoIndex indexer = new RepoIndex();

		Map<String,String> props = new HashMap<>();
		props.put(ResourceIndexer.ROOT_URL, new File("testdata").getAbsoluteFile().toURI().toURL().toString());

		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/subdir/01-bsn+version.jar")), writer, props);

		String expected = Utils.readStream(new FileInputStream("testdata/fragment-subdir2.txt"));
		assertEquals(expected, writer.toString().trim());
	}

	public void testLogErrorsFromAnalyzer() throws Exception {
		ResourceAnalyzer badAnalyzer = new ResourceAnalyzer() {
			public void analyzeResource(Resource resource, List<Capability> capabilities,
					List<Requirement> requirements) throws Exception {
				throw new Exception("Bang!");
			}
		};
		ResourceAnalyzer goodAnalyzer = mock(ResourceAnalyzer.class);

		LogService log = mock(LogService.class);
		RepoIndex indexer = new RepoIndex(log);
		indexer.addAnalyzer(badAnalyzer, null);
		indexer.addAnalyzer(goodAnalyzer, null);

		// Run the indexer
		Map<String,String> props = new HashMap<>();
		props.put(ResourceIndexer.ROOT_URL, new File("testdata").getAbsoluteFile().toURI().toURL().toString());
		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/subdir/01-bsn+version.jar")), writer, props);

		// The "good" analyzer should have been called
		verify(goodAnalyzer).analyzeResource(any(Resource.class), anyListOf(Capability.class),
				anyListOf(Requirement.class));

		// The log service should have been notified about the exception
		ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
		verify(log).log(eq(LogService.LOG_ERROR), any(String.class), exceptionCaptor.capture());
		assertEquals("Bang!", exceptionCaptor.getValue().getMessage());
	}

	public void testBundleOutsideRootDirectory() throws Exception {
		LogService log = mock(LogService.class);
		RepoIndex indexer = new RepoIndex(log);

		Map<String,String> props = new HashMap<>();
		props.put(ResourceIndexer.ROOT_URL, new File("testdata/subdir").getAbsoluteFile().toURI().toURL().toString());

		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/01-bsn+version.jar")), writer, props);

		verify(log).log(eq(LogService.LOG_ERROR), anyString(), isA(IllegalArgumentException.class));
	}

	public void testRemoveDisallowed() throws Exception {
		LogService log = mock(LogService.class);
		RepoIndex indexer = new RepoIndex(log);
		indexer.addAnalyzer(new NaughtyAnalyzer(), null);

		Map<String,String> props = new HashMap<>();
		props.put(ResourceIndexer.ROOT_URL, new File("testdata").getAbsoluteFile().toURI().toURL().toString());

		StringWriter writer = new StringWriter();
		indexer.indexFragment(Collections.singleton(new File("testdata/subdir/01-bsn+version.jar")), writer, props);

		verify(log).log(eq(LogService.LOG_ERROR), anyString(), isA(UnsupportedOperationException.class));
	}

	public void testRecogniseFelixSCR() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));
		assertFragmentMatch(indexer, "testdata/org.apache.felix.scr-1.6.0.xml",
				"testdata/org.apache.felix.scr-1.6.0.jar");
	}

	public void testRecogniseAriesBlueprint() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));
		assertFragmentMatch(indexer, "testdata/org.apache.aries.blueprint-1.0.0.xml",
				"testdata/org.apache.aries.blueprint-1.0.0.jar");
	}

	public void testRecogniseGeminiBlueprint() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));
		assertFragmentMatch(indexer, "testdata/gemini-blueprint-extender-1.0.0.RELEASE.xml",
				"testdata/gemini-blueprint-extender-1.0.0.RELEASE.jar");
	}

	public void testRecogniseFelixJetty() throws Exception {
		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));
		assertFragmentMatch(indexer, "testdata/org.apache.felix.http.jetty-2.2.0.xml",
				"testdata/org.apache.felix.http.jetty-2.2.0.jar");
	}

	public void testMacroExpansion() throws Exception {
		Properties props = new Properties();
		try (InputStream in = new FileInputStream("testdata/known-bundles.properties")) {
			props.load(in);
		}

		RepoIndex indexer = new RepoIndex();
		indexer.addAnalyzer(new KnownBundleAnalyzer(props), FrameworkUtil.createFilter("(name=*)"));
		assertFragmentMatch(indexer, "testdata/org.apache.felix.eventadmin-1.2.14.xml",
				"testdata/org.apache.felix.eventadmin-1.2.14.jar");
	}

	public void testFragmentRequireBlueprint() throws Exception {
		assertFragmentMatch("testdata/fragment-17.txt", "testdata/17-blueprint1.jar");
	}

	public void testFragmentRequireBlueprintUsingHeader() throws Exception {
		assertFragmentMatch("testdata/fragment-18.txt", "testdata/18-blueprint2.jar");
	}

	public void testFragmentBundleNativeCode() throws Exception {
		assertFragmentMatch("testdata/fragment-19.txt", "testdata/19-bundlenativecode.jar");
	}

	public void testFragmentBundleNativeCodeOptional() throws Exception {
		assertFragmentMatch("testdata/fragment-20.txt", "testdata/20-bundlenativecode-optional.jar");
	}

	public void testFragmentPlainJar() throws Exception {
		LogService mockLog = Mockito.mock(LogService.class);
		RepoIndex indexer = new RepoIndex(mockLog);
		indexer.addAnalyzer(new KnownBundleAnalyzer(), FrameworkUtil.createFilter("(name=*)"));

		assertFragmentMatch(indexer, "testdata/fragment-plainjar.txt", "testdata/jcip-annotations.jar");
		Mockito.verifyZeroInteractions(mockLog);
	}

	public void testFragmentPlainJarWithVersion() throws Exception {
		assertFragmentMatch("testdata/fragment-plainjar-versioned.txt", "testdata/jcip-annotations-2.5.6.wibble.jar");
	}

	public void testImportServiceOptional() throws Exception {
		assertFragmentMatch("testdata/org.apache.felix.eventadmin-1.3.2.xml",
				"testdata/org.apache.felix.eventadmin-1.3.2.jar");
	}
}
