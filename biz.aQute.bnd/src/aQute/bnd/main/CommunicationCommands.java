package aQute.bnd.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Formatter;
import java.util.List;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.http.URLCache;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;

public class CommunicationCommands extends Processor {

	final bnd					bnd;
	final Workspace				workspace;
	final HttpClient			httpClient;
	final CommunicationOptions	options;
	final URLCache				cache;

	@Description("Commands to verify and check the communications settings for the http client")

	interface CommunicationOptions extends Options {

	}

	CommunicationCommands(bnd bnd, CommunicationOptions options) throws Exception {
		this.bnd = bnd;
		this.options = options;
		this.workspace = bnd.getWorkspace((String) null);
		this.httpClient = workspace.getPlugin(HttpClient.class);
		if (this.httpClient == null) {
			error("No http client");
			this.cache = null;
		} else
			this.cache = httpClient.cache();
	}

	@Arguments(arg = "url...")
	@Description("Show the information used by the Http Client to get aremote file")
	interface InfoOptions extends Options {
		@Description("Use the cache option. If this option is not used then thefile will be refreshed")

		boolean cached();

		@Description("Save the remote content in the given file. If you use '.'then	the file shown to the output")

		String output();
	}

	@Description("Show the information used by the Http Client to get aremote file")
	public void _info(InfoOptions options) throws Exception {
		List<String> arguments = options._arguments();
		try (Formatter f = new Formatter()) {
			while (!arguments.isEmpty()) {

				URI uri = new URI(arguments.remove(0));
				f.format("URI %s%n", uri);

				URLConnectionHandler handler = httpClient.findMatchingHandler(uri.toURL());
				if (handler != null) {
					f.format("Handled by %s%n", handler);
				}

				f.format("Cache file %s%n", httpClient.getCacheFileFor(uri));

				TaggedData tag;
				if (options.cached()) {
					tag = httpClient.build()
						.useCache()
						.get(TaggedData.class)
						.go(uri);
				} else {
					tag = httpClient.connectTagged(uri.toURL());
				}
				f.format("Last Modified %s%n", Instant.ofEpochMilli(tag.getModified()));
				f.format("Response code %s%n", tag.getResponseCode());
				f.format("State %s%n", tag.getState());
				f.format("Tag %s%n", tag.getTag());
				f.format("Has Payload %s%n", tag.hasPayload());

				if (options.output() != null && (tag.isOk() || tag.isNotModified())) {
					f.format("------------%n");

					OutputStream out;
					if (options.output()
						.equals(".")) {
						out = System.out;
					} else {
						File fout = bnd.getFile(options.output());
						fout.getParentFile()
							.mkdirs();
						out = new FileOutputStream(fout);
					}
					IO.copy(cache.getCacheFileFor(uri), out);
					f.format("------------%n");
				}
				tag.close();

				f.format("%n");
				f.flush();
			}
			bnd.out.println(f.toString());
		}
	}

	@Description("Clear the cached file that is associated with the givenURI")
	interface ClearOptions extends Options {

	}

	@Description("Clear the cached file that is associated with the givenURI")
	public void _clear(ClearOptions options) throws Exception {
		List<String> arguments = options._arguments();
		while (!arguments.isEmpty()) {
			URI uri = new URI(arguments.remove(0));
			if (cache.clear(uri)) {
				System.out.println("existed, removed");
			} else {
				System.out.println("not found");
			}
		}
	}

	@Description("Show the bnd -connection-settings")
	interface SettingsOptions extends Options {}

	@Description("Show the bnd -connection-settings")
	public void _settings(SettingsOptions options) {
		try (Formatter f = new Formatter()) {
			httpClient.reportSettings(f);
			bnd.out.println(f.toString());
		}
	}

}
