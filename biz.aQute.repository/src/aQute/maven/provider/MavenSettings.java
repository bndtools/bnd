package aQute.maven.provider;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import aQute.lib.io.IO;

public class MavenSettings {
	private static final String					MAVEN_USER_SETTINGS_XML	= "~/.m2/settings.xml";
	private static final Pattern				VAR_PATTERN				= Pattern
			.compile("\\$\\{(?<category>env|user)\\.(?<var>.*?)\\}");
	private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private static final XPathFactory			xpf						= XPathFactory.newInstance();

	private static volatile String				LOCAL_MVN_REPO_DIR;

	public static String localRepository() {
		if (LOCAL_MVN_REPO_DIR == null)
			LOCAL_MVN_REPO_DIR = lookupLocalRepository();
		return LOCAL_MVN_REPO_DIR;
	}

	private static String lookupLocalRepository() {
		String localRepository = "${user.home}/.m2/repository";
		localRepository = parseLocalDirectory(globalSettingsFile(), localRepository);
		localRepository = parseLocalDirectory(userSettingsFile(), localRepository);
		return interpolate(localRepository);
	}

	/*
	 * According to https://maven.apache.org/settings.html we only need to
	 * interpolate ${user.home} and ${env.*} here
	 */
	private static String interpolate(String input) {
		Matcher matcher = VAR_PATTERN.matcher(input);

		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String category = matcher.group("category");
			String var = matcher.group("var");
			String replacement = matcher.group();

			switch (category) {
				case "user" :
					// The only thing we support here is "home"
					if (var.equals("home")) {
						// We let bnd take care of figuring out the home
						// directory for us
						replacement = "~";
					}
					break;
				case "env" :
					replacement = System.getenv(var) == null ? "" : System.getenv(var);
					break;
			}

			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);

		return result.toString();
	}

	private static String parseLocalDirectory(File settings, String dflt) {
		if (settings == null || !settings.isFile())
			return dflt;

		XPath xpath = xpf.newXPath();
		String localRepository = null;

		try {
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document document = builder.parse(settings);
			document.normalize();

			localRepository = xpath.evaluate("/settings/localRepository", document).trim();
			if (localRepository.isEmpty())
				return dflt;
			return localRepository;
		} catch (Exception ignored) {
			return dflt;
		}
	}

	private static File globalSettingsFile() {
		String m2home = System.getenv("M2_HOME");
		if (m2home == null)
			return null;
		return IO.getFile(String.format("%s/conf/settings.xml", m2home));
	}

	private static File userSettingsFile() {
		return IO.getFile(MAVEN_USER_SETTINGS_XML);
	}
}