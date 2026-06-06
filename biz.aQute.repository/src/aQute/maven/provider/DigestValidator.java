package aQute.maven.provider;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.io.IO;

class DigestValidator {

	// MD5(xercesImpl-2.9.0.jar)= 33ec8d237cbaceeffb2c2a7f52afd79a
	// SHA1(xercesImpl-2.9.0.jar)= 868c0792233fc78d8c9bac29ac79ade988301318

	final static Pattern DIGEST_POLLUTED = Pattern.compile("(.+=\\s*)?(?<digest>([0-9A-F][0-9A-F])+)\\s*",
		Pattern.CASE_INSENSITIVE);

	/**
	 * Validates that a file digest matches a remote digest.
	 *
	 * @param file the file being validated (deleted if validation fails)
	 * @param fileDigest the computed file digest
	 * @param remoteDigest the remote digest to compare against
	 * @throws IllegalArgumentException if digests don't match and deletes file
	 */
	static void checkDigest(File file, String fileDigest, String remoteDigest) {
		if (remoteDigest == null)
			return;

		Matcher m = DIGEST_POLLUTED.matcher(remoteDigest);
		if (m.matches())
			remoteDigest = m.group("digest");

		try {
			int start = 0;
			while (start < remoteDigest.length() && Character.isWhitespace(remoteDigest.charAt(start)))
				start++;

			for (int i = 0; i < fileDigest.length(); i++) {
				if (start + i < remoteDigest.length()) {
					char us = fileDigest.charAt(i);
					char them = remoteDigest.charAt(start + i);
					if (us == them || Character.toLowerCase(us) == Character.toLowerCase(them))
						continue;
				}
				throw new IllegalArgumentException(
					"Invalid content checksum " + fileDigest + " for " + file + "; expected " + remoteDigest);
			}

		} catch (Exception e) {
			IO.delete(file);
			throw e;
		}
	}
}
