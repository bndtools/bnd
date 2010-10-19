package aQute.bnd.libsync;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.lib.osgi.*;

public class LibSync extends Processor {
	URL url; 
	
	
	public LibSync() {
		
	}
	
	public LibSync(Processor parent) {
		super(parent);
	}
	
	
	public void submit(Jar jar) throws Exception {

		String host = getProperty("libsync.repo", "http://libsync.com/repo");

		try {
			URL url = new URL(host);
			Verifier v = new Verifier(jar);
			v.setPedantic(true);
			v.verify();
			getInfo(v);
			if (isOk() && v.getWarnings().isEmpty()) {
				send0(jar, url);
			}
		} catch (MalformedURLException e) {
			error("The libsync.repo property does not contain a proper URL %s, exception: %s",
					host, e);
		} catch (Exception e) {
			error("Submission of %s to %s failed even after retrying", host, jar.getName());
		}
	}

	Jar send0(Jar jar, URL url) throws Exception {
		int retries = Integer.parseInt(getProperty("libsync.retries", "3"));

		for (int i = 0; i < retries - 1; i++) {
			try {
				return send1(jar, url);
			} catch (Exception e) {
				warning("Submission failed %s, will retry (%d times defined in libsync.retries)",
						e, retries);
				Thread.sleep(i * 1000);
			}
		}
		return send1(jar, url);
	}

	Jar send1(Jar jar, URL url) throws Exception {
		HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
		urlc.setDoOutput(true);
		urlc.setDoInput(true);
		trace("Connecting to: %s", url);
		urlc.setRequestMethod("POST");
		urlc.setRequestProperty("Content-Type", "application/binary");

		urlc.connect();

		OutputStream out = urlc.getOutputStream();
		try {
			jar.write(out);
		} finally {
			out.close();
		}
		trace("Submitted: %s to %s", jar.getName(), url);

		InputStream in = urlc.getInputStream();
		try {
			Jar result = new Jar(url.toExternalForm(), in);
			addClose(result);
			trace("Received: %s", result.getName());
			Resource errors = result.getResource("META-INF/errors");
			Resource warnings = result.getResource("META-INF/warnings");
			if (errors != null)
				parse(errors.openInputStream(), url.getFile(), getErrors());
			if (warnings != null)
				parse(warnings.openInputStream(), url.getFile(), getWarnings());
			return result;
		} finally {
			in.close();
		}
	}

	private void parse(InputStream in, String prefix, List<String> to) throws Exception {
		try {
			InputStreamReader r = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(r);
			String line = br.readLine();
			while (line != null) {
				to.add(prefix + line);
				line = br.readLine();
			}
		} finally {
			in.close();
		}
	}
}
