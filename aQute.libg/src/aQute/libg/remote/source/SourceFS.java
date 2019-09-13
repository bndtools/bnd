package aQute.libg.remote.source;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import aQute.libg.remote.Delta;
import aQute.libg.remote.Sink;

class SourceFS {
	private final static Pattern				WINDOWS_PREFIX	= Pattern.compile("(\\p{Alpha}):\\\\(.*)");
	private final static String					WINDOWS_FILE	= "(?:\\p{Alpha}:|\\\\)(\\\\[\\p{Alnum}-_+.~@$%&=]+)*";
	private final static String					UNIX_FILE		= "(/[\\p{Alnum}-_+.~@$%&=]+)+";
	private final static Pattern				LOCAL_P			= Pattern
		.compile(IO.isWindows() ? WINDOWS_FILE : UNIX_FILE);

	private MultiMap<String, File>				shas			= new MultiMap<>();
	private final Map<File, FileDescription>	files			= new HashMap<>();
	private final boolean						pathConversion;
	private final String						cwd;
	private final char							separatorChar;
	private Sink								sink;
	private String								areaId;

	/*
	 * The information we maintain per file that we sync remotely.
	 */
	static class FileDescription {
		File			file;
		String			path;
		String			sha;
		long			modified;
		boolean			touched;
		public boolean	transform;
		public boolean	dir;
	}

	SourceFS(char separatorChar, File cwd, Sink sink, String areaId) {
		this.separatorChar = separatorChar;
		this.cwd = cwd.getAbsolutePath();
		pathConversion = File.separatorChar != separatorChar;
		this.sink = sink;
		this.areaId = areaId;
	}

	public String transform(String s) throws Exception {
		Matcher m = LOCAL_P.matcher(s);
		StringBuilder sb = new StringBuilder();
		int start = 0;
		for (; m.find(); start = m.end()) {
			FileDescription fd = toRemote(m.group(0));
			fd.touched = true;
			sb.append(s, start, m.start())
				.append(fd.path);
		}
		return (start == 0) ? s
			: sb.append(s, start, s.length())
				.toString();
	}

	private FileDescription toRemote(String localPath) throws Exception {
		File f = new File(localPath);
		return toRemote(f);
	}

	private FileDescription toRemote(File f) throws NoSuchAlgorithmException, Exception {
		FileDescription fd = files.get(f);

		if (fd != null)
			return fd;

		String remotePath = toRemotePath(f);

		fd = new FileDescription();
		fd.file = f;
		fd.path = remotePath;
		fd.modified = f.lastModified();

		if (f.isFile()) {
			fd.sha = updateSha(null, f);
			fd.touched = true;
		} else if (f.isDirectory()) {
			fd.dir = true;
			for (File sub : f.listFiles()) {
				toRemote(sub);
			}
		}
		files.put(f, fd);

		return fd;
	}

	private String toRemotePath(File f) {
		String remotePath;
		String abs = f.getAbsolutePath();
		if (abs.startsWith(cwd)) {
			remotePath = abs.substring(cwd.length());
			while (remotePath.startsWith(File.separator))
				remotePath = remotePath.substring(1);
		} else {
			if (IO.isWindows()) {

				//
				// Why is windows always 10x more complicated??
				// Buffoons ...
				//
				// WE have
				// Remote names: \\foo\zoo
				// device names c:\foo
				// absolute \zoo\zoo
				//

				if (abs.startsWith("\\\\")) { // remote file path, should be
												// followed by remote name
					remotePath = "_ABS\\REMOTE" + abs.substring(1);
				} else {
					Matcher m = WINDOWS_PREFIX.matcher(abs);
					if (m.matches()) {
						remotePath = "_ABS\\" + m.group(1) + "\\" + m.group(2);
					} else
						remotePath = "_ABS\\" + abs;
				}
			} else
				remotePath = "_ABS" + abs;
		}

		if (pathConversion)
			remotePath = remotePath.replace(File.separatorChar, separatorChar);

		return remotePath;
	}

	public void sync() throws Exception {

		Set<FileDescription> toBeDeleted = new HashSet<>();
		List<Delta> deltas = new ArrayList<>();

		for (FileDescription fd : new HashSet<>(files.values())) {
			if (fd.transform) {
				Delta delta = new Delta();
				delta.path = fd.path;
				delta.content = transform(IO.collect(fd.file));
				deltas.add(delta);
			}
		}

		for (FileDescription fd : files.values()) {
			if (fd.file.isDirectory())
				continue;

			if (fd.modified != fd.file.lastModified() || fd.touched) {
				fd.touched = false;
				Delta delta = new Delta();
				delta.path = fd.path;

				fd.modified = fd.file.lastModified();
				fd.touched = true;

				if (!fd.file.isFile()) {
					delta.delete = true;
					toBeDeleted.add(fd);
					deltas.add(delta);
					continue;
				}

				if (!fd.transform) {
					String updateSha = updateSha(fd.sha, fd.file);

					delta.sha = fd.sha = updateSha;
					deltas.add(delta);
				}
			}
		}

		files.values()
			.removeAll(toBeDeleted);

		sync(deltas);
	}

	protected void sync(List<Delta> deltas) throws Exception {
		sink.sync(areaId, deltas);
	}

	public String updateSha(String oldSha, File file) throws NoSuchAlgorithmException, Exception {
		if (oldSha != null)
			shas.remove(oldSha);

		if (file != null && file.isFile()) {
			String sha = SHA1.digest(file)
				.asHex();
			shas.add(sha, file);
			return sha;
		}
		return null;
	}

	public byte[] getData(String sha) throws Exception {
		List<File> files = shas.get(sha);
		if (files == null)
			return null;

		for (File f : files) {
			if (f.isFile()) {

				assert sha.equals(SHA1.digest(f)
					.asHex());

				return IO.read(f);
			}
		}
		return null;
	}

	public void markTransform(File f) throws Exception {
		FileDescription fd = toRemote(f);
		fd.transform = true;
	}

	public String add(File file) throws Exception {
		return toRemote(file).path;
	}

}
