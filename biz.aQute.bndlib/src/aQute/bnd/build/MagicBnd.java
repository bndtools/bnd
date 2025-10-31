package aQute.bnd.build;

import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.re;
import static aQute.libg.re.Catalog.term;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import aQute.bnd.result.Result;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.re.Catalog;
import aQute.libg.re.RE;
import aQute.libg.re.RE.Match;

/**
 * This is used in included files in a bnd file. It allows another file to be
 * mapped to the properties. This can be used to setup a plugin that is
 * parameterized by that file.
 * <p>
 * See test.WorkspaceTest.testMagicBnd() for a test.
 * </p>
 */
public class MagicBnd {
	final static RE	KEY_P		= re("[\\w\\d_\\.]+");
	final static RE	VALUE_P		= Catalog.setAll;
	final static RE	REPO_ATTR_P	= term(lit("#"), g("key", KEY_P), lit("="), g("value", VALUE_P), Catalog.setWs);

	public static Result<Properties> map(Workspace workspace, File file) {
		String parts[] = Strings.extension(file.getName());
		if (parts == null) {
			return Result.ok(null);
		}

		String ext = parts[1];
		return switch (ext) {
			case "pmvn" -> convertMaven(workspace, file);
			case "pobr" -> convertOBR(workspace, file);
			default -> Result.ok(null);
		};
	}

	/*
	 * Convert an OBR XML file to bnd plugin
	 */
	private static Result<Properties> convertOBR(Workspace workspace, File file) {
		StringBuilder sb = new StringBuilder();
		String parts[] = Strings.extension(file.getName());

		String name = getName(file, file.getName());

		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("name", name);
		attrs.put("locations", file.toURI()
			.toString());

		sb.append("aQute.bnd.repository.osgi.OSGiRepository");

		for (Map.Entry<String, String> e : attrs.entrySet()) {
			sb.append(";")
				.append(e.getKey())
				.append("='")
				.append(e.getValue())
				.append("'");
		}

		UTF8Properties p = new UTF8Properties();
		p.setProperty("-plugin.ext." + file.getName(), sb.toString(), file.getAbsolutePath());
		return Result.ok(p);
	}

	private static String getName(File file, String defaultName) throws FactoryConfigurationError {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		try (InputStream inputStream = new FileInputStream(file);) {
			XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.START_ELEMENT) {
					return reader.getAttributeValue(null, "name");
				}
			}
		} catch (Exception e1) {
			// ignore
		}
		return defaultName;
	}

	/*
	 * Convert an Maven GAV file into a MavenBndRepository plugin setup
	 */

	private static Result<Properties> convertMaven(Workspace ws, File file) {
		StringBuilder sb = new StringBuilder();
		String parts[] = Strings.extension(file.getName());

		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("name", file.getName());
		boolean repo = false;
		List<String> release = new ArrayList<>();
		List<String> snapshot = new ArrayList<>();
		try (BufferedReader br = IO.reader(file)) {
			String line;
			while ((line = br.readLine()) != null) {
				Optional<Match> matches = REPO_ATTR_P.matches(line);
				if (matches.isPresent()) {
					Match m = matches.get();
					String key = m.presentGroup("key");
					String value = m.presentGroup("value");
					switch (key) {
						case "releaseUrl", "releaseUrls" -> release.add(value);
						case "snapshotUrl", "snapshotUrls" -> snapshot.add(value);
						case "repo" -> {
							release.add(value);
							snapshot.add("value");
						}
						case "index" -> {
						}
						default -> {
							attrs.put(key, value);
						}
					}
				} else
					break;
			}
		} catch (Exception e) {
			return Result.err("reading file %s to convert to bnd properties failed: %s", file, e.getMessage());
		}
		sb.append("aQute.bnd.repository.maven.provider.MavenBndRepository")
			.append(";index=")
			.append(file.getAbsolutePath()
				.replace('\\', '/'));

		for (Map.Entry<String, String> e : attrs.entrySet()) {
			sb.append(";")
				.append(e.getKey())
				.append("='")
				.append(e.getValue())
				.append("'");
		}

		if (release.isEmpty())
			release.add("https://repo.maven.apache.org/maven2/");
		sb.append(";releaseUrl='")
			.append(release.stream()
				.collect(Collectors.joining()))
			.append("'");
		sb.append(";snapshotUrl='")
			.append(snapshot.stream()
				.collect(Collectors.joining()))
			.append("'");

		UTF8Properties p = new UTF8Properties();
		p.setProperty("-plugin.ext." + file.getName(), sb.toString(), file.getAbsolutePath());
		return Result.ok(p);
	}
}
