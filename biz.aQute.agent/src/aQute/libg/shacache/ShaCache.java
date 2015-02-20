package aQute.libg.shacache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.regex.Pattern;

import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;

public class ShaCache {
	static Pattern SHA_P = Pattern.compile("[A-F0-9]{40,40}",
			Pattern.CASE_INSENSITIVE);

	private final File root;

	public ShaCache(File root) {
		this.root = root;
		this.root.mkdirs();
	}

	public InputStream getStream(String sha, ShaSource... sources)
			throws FileNotFoundException {
		if (!SHA_P.matcher(sha).matches())
			throw new IllegalArgumentException("Not a SHA");

		File f = new File(root, sha);
		if (!f.isFile()) {
			for (ShaSource s : sources) {
				try {
					InputStream in = s.get(sha);
					if (s.isFast())
						return in;

					if (in != null) {
						File tmp = IO.createTempFile(root, sha.toLowerCase(),
								".shacache");
						IO.copy(in, tmp);
						String digest = SHA1.digest(tmp).asHex();
						if (digest.equalsIgnoreCase(sha)) {
							tmp.renameTo(f);
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (!f.isFile())
			return null;
		return new FileInputStream(f);
	}

	public File getFile(String sha, ShaSource... sources)
			throws FileNotFoundException {
		if (!SHA_P.matcher(sha).matches())
			throw new IllegalArgumentException("Not a SHA");

		File f = new File(root, sha);
		if (f.isFile())
			return f;

		for (ShaSource s : sources) {
			try {
				InputStream in = s.get(sha);
				if (in != null) {
					File tmp = IO.createTempFile(root, sha.toLowerCase(),
							".shacache");
					IO.copy(in, tmp);
					String digest = SHA1.digest(tmp).asHex();
					if (digest.equalsIgnoreCase(sha)) {
						tmp.renameTo(f);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (!f.isFile())
			return null;
		return f;
	}

	public void purge() {
		IO.delete(root);
		root.mkdirs();
	}
}
