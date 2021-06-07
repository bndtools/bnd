package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.libg.cryptography.SHA512;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.maven.dto.ChecksumDTO;

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
		checksumDTO.md5 = calcChecksum(jar, MD5.ALGORITHM);
		checksumDTO.sha1 = calcChecksum(jar, SHA1.ALGORITHM);
		checksumDTO.sha256 = calcChecksum(jar, SHA256.ALGORITHM);
		checksumDTO.sha512 = calcChecksum(jar, SHA512.ALGORITHM);

		if (checksumDTO.md5 == null && checksumDTO.sha1 == null && checksumDTO.sha256 == null
			&& checksumDTO.sha512 == null) {
			return null;
		}
		return checksumDTO;
	}

	private String calcChecksum(Jar jar, String algorithm) {

		File f = jar.getSource();
		if (f != null && f.isFile()) {
			try {

				switch (algorithm) {
					case MD5.ALGORITHM :
						return MD5.digest(f)
							.asHex();
					case SHA1.ALGORITHM :
						return SHA1.digest(f)
							.asHex();
					case SHA256.ALGORITHM :
						return SHA256.digest(f)
							.asHex();
					case SHA512.ALGORITHM :
						return SHA512.digest(f)
							.asHex();
					default :
						return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				_reporter.warning("Could not calculate checksum %s", algorithm);
			}
		} else {
			_reporter.warning("Could not get File to calculate the checksum");
		}
		return null;

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
