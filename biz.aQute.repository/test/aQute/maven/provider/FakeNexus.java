package aQute.maven.provider;

import java.io.File;
import java.net.HttpURLConnection;

import aQute.http.testservers.Httpbin;
import aQute.lib.date.Dates;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;

public class FakeNexus extends Httpbin {

	private File base;

	public FakeNexus(Config config, File base) throws Exception {
		super(config);
		this.base = base;
	}

	public File _repo(Request rq, Response rsp) throws Exception {
		try {
			String path = rq.uri.getPath();
			if (!path.startsWith("/repo/")) {
				rsp.code = HttpURLConnection.HTTP_BAD_REQUEST;
				return null;
			}
			path = path.substring(6);

			System.out.println(rq.method + " " + path);

			File f = IO.getFile(base, path);

			switch (rq.method) {
				case "GET" :

					if (!f.isFile()) {
						rsp.code = HttpURLConnection.HTTP_NOT_FOUND;
						System.out.println(rq.method + " " + path + " not found");
						return null;
					}

					if (rq.headers.containsKey("if-modified-since")) {
						long l = fromHttpDate(rq.headers.get("if-modified-since"));
						if (f.lastModified() <= l) {
							rsp.code = HttpURLConnection.HTTP_NOT_MODIFIED;
							return null;
						}
					}

					if (rq.headers.containsKey("if-none-match")) {
						String etag = rq.headers.get("if-none-match");
						if (etag.equals(SHA1.digest(f)
							.asHex())) {
							rsp.code = HttpURLConnection.HTTP_NOT_MODIFIED;
							return null;
						}
					}

					rsp.headers.put("Last-Modified", toHttpDate(f.lastModified()));
					rsp.headers.put("ETag", SHA1.digest(f)
						.asHex());
					return f;

				case "PUT" :
					if (rq.headers.containsKey("if-match") && f.isFile()) {
						System.out.println("if-match" + rq.headers.get("if-match"));
						String etag = rq.headers.get("if-match");
						if (!etag.equals(SHA1.digest(f)
							.asHex())) {
							rsp.code = HttpURLConnection.HTTP_PRECON_FAILED;
							return null;
						}
					}

					if (rq.headers.containsKey("if-unmodified-since")) {
						long l = fromHttpDate(rq.headers.get("if-unmodified-since"));
						System.out.println("if-unmodified-since h=" + l + " f=" + f.lastModified());
						if (f.isFile() && f.lastModified() > l) {
							rsp.code = HttpURLConnection.HTTP_PRECON_FAILED;
							return null;
						}
					}
					f.getParentFile()
						.mkdirs();
					IO.copy(rq.content, f);
					rsp.headers.put("Last-Modified", toHttpDate(f.lastModified()));
					rsp.headers.put("ETag", SHA1.digest(f)
						.asHex());
					rsp.code = 201;
					return f;

				case "DELETE" :
					if (!f.isFile()) {
						rsp.code = HttpURLConnection.HTTP_NOT_FOUND;
						return null;
					}
					IO.delete(f);
					rsp.code = 200;
					return null;

				default :
					System.out.println("OOPS");
					rsp.code = HttpURLConnection.HTTP_BAD_REQUEST;
					return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("exit");
		}
		return null;
	}

	private String toHttpDate(long lastModified) {
		return Dates.formatMillis(Dates.RFC_7231_DATE_TIME, lastModified);
	}


	private long fromHttpDate(String string) {
		return Dates.parseMillis(Dates.RFC_7231_DATE_TIME, string);
	}

}
