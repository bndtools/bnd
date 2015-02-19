package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import aQute.bnd.service.remotelaunch.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;

@SuppressWarnings("unused")
public class RemoteLauncher extends Thread {
	final int									REMOTE_PORT		= 17023;
	static Pattern								WINDOWS_FILE_P	= Pattern.compile(
																		"([a-z]:|\\\\)(\\\\[\\w\\d-_+.~@$%&=]+)*",
																		Pattern.CASE_INSENSITIVE);
	static Pattern								UNIX_FILE_P		= Pattern.compile("(/[\\w\\d-_+.~@$%&=])+",
																		Pattern.CASE_INSENSITIVE);
	static Pattern								LOCAL_P			= File.separatorChar == '\\' ? WINDOWS_FILE_P
																		: UNIX_FILE_P;

	final Map<File,FileDescription>				fileToId		= new HashMap<File,FileDescription>();
	final LinkedBlockingQueue<FileDescription>	queue			= new LinkedBlockingQueue<FileDescription>();
	private ProjectLauncher						launcher;
	private Slave								slave;
	private List<String>						launch;

	static class FileDescription {
		String	name;
		String	id;
		byte[]	sha;
		File	file;
	}

	public RemoteLauncher(ProjectLauncher projectLauncher) {
		this.launcher = projectLauncher;
	}

	public String transform(String s) throws Exception {
		Matcher m = LOCAL_P.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, cached(m.group(0)));
		}
		m.appendTail(sb);
		return null;
	}

	private String cached(String path) throws Exception {
		File f = new File(path).getAbsoluteFile();
		if (f.isFile())
			return cached(f);
		return path;
	}

	private String cached(File f) throws Exception {
		FileDescription fd = fileToId.get(f);
		if (fd == null) {
			fileToId.put(f, fd = newId(f, f.getName()));
		}
		return fd.id;
	}

	private FileDescription newId(File f, String name) throws Exception {
		int n = 0;
		String proposedId = "local/" + name;
		while (fileToId.values().contains(proposedId))
			proposedId = "local/" + ++n + name;

		FileDescription fd = new FileDescription();
		fd.id = proposedId;
		fd.file = f;
		fd.name = name;
		fd.sha = SHA1.digest(f).digest();
		queue.add(fd);
		return fd;
	}

	public List<String> transform(List<String> entries) throws Exception {
		List<String> result = new ArrayList<String>(entries);
		for (int i = 0; i < result.size(); i++)
			result.set(i, transform(result.get(i)));
		return result;
	}

	public void close() {
		// TODO Auto-generated method stub

	}


	public void remote(List<String> java, String host) {
	}

	public void update(File f) throws Exception {
		String s = IO.collect(f);
		s = transform(s);
		IO.store(s, f);
		slave.update(cached(f), s.getBytes("UTF-8"));
	}

	public int launch() {
		// TODO Auto-generated method stub
		return 0;

	}

}
