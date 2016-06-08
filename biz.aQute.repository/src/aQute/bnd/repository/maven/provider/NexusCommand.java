package aQute.bnd.repository.maven.provider;

import java.io.IOException;
import java.net.URI;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Options;

public class NexusCommand extends Processor {

	public NexusCommand(Processor parent) {
		super(parent);
	}

	public NexusCommand() {
		super();
	}

	@Arguments(arg = "path")
	interface SignOptions extends Options {

	}

	public void _sign(SignOptions options) throws Exception {
		URI uri = new URI(options._arguments().get(0));
		HttpClient client = getHttpClient();

	}

	private HttpClient getHttpClient() throws IOException, Exception {
		HttpClient client = getPlugin(HttpClient.class);
		if (client == null) {
			client = new HttpClient();
			client.readSettings(this);
		}
		return client;
	}
}
