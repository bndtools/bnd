package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.artifact.dto.ChecksumDTO;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;

/**
 * This plugin calculates and add the checksums of the file to the report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.CHECKSUM)
public class ChecksumPlugin implements ReportEntryPlugin<Jar>, Plugin {

	private Reporter					_reporter;
	private final Map<String, String>	_properties	= new HashMap<>();

	public ChecksumPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.CHECKSUM);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Jar.class.getCanonicalName());
	}

	@Override
	public ChecksumDTO extract(final Jar jar, final Locale locale) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		ChecksumDTO checksumDTO = new ChecksumDTO();
		checksumDTO.md5 = calcChecksum(jar, "MD5");
		checksumDTO.sha1 = calcChecksum(jar, "SHA-1");
		checksumDTO.sha256 = calcChecksum(jar, "SHA-256");

		if (checksumDTO.md5 == null && checksumDTO.sha1 == null && checksumDTO.sha256 == null) {
			return null;
		}
		return checksumDTO;
	}

	private String calcChecksum(Jar jar, String algorithm) {

		File f = jar.getSource();
		if (f != null && f.isFile()) {
			try {

				final MessageDigest digest = MessageDigest.getInstance(algorithm);
				final byte[] hashbytes = digest.digest(Files.readAllBytes(f.toPath()));
				String hex = bytesToHex(hashbytes);
				return hex;
			} catch (Exception e) {
				_reporter.warning("Could not calculate checksum s%", algorithm);
			}
		} else {
			_reporter.warning("Could not get File to calculate the checksum");
		}

		return null;
	}

	private static String bytesToHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public void setReporter(final Reporter processor) {
		_reporter = processor;
	}
}
