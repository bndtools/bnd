package aQute.bnd.repository.maven.provider;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.maven.nexus.provider.Nexus;
import aQute.maven.nexus.provider.Signer;

public class NexusCommand extends Processor {
	private final static Logger	logger	= LoggerFactory.getLogger(NexusCommand.class);

	private NexusOptions		options;
	private Nexus				nexus;

	public interface NexusOptions extends Options {
		URI url();
	}

	public NexusCommand(Processor parent, NexusOptions options) throws Exception {
		super(parent);
		this.options = options;
		if (this.options == null) {
			error("No -u/--url set");
			return;
		}
		HttpClient client = new HttpClient();
		client.readSettings(parent);
		this.nexus = new Nexus(options.url(), client);
	}

	@Arguments(arg = {
		"path..."
	})
	interface SignOptions extends Options {
		String command(String s);

		boolean show();

		String password();

		String key();
	}

	public void _sign(SignOptions options) throws Exception {
		String password = null;
		if (options.password() == null) {
			Console console = System.console();
			if (console == null) {
				error("No -p/--password set for PGP key and no console to ask");
			} else {
				char[] pw = console.readPassword("Passsword: ");
				if (pw == null || pw.length == 0) {
					error("Password not entered");
				}
				password = new String(pw);
			}
		} else
			password = options.password();

		Signer signer = new Signer(new String(password), options.command(getProperty("gpg", System.getenv("GPG"))));

		if (signer == null || password == null || !isOk())
			return;

		List<String> args = options._arguments();

		if (args.isEmpty()) {
			List<URI> files = nexus.files();
			for (URI uri : files) {
				try {
					logger.debug("signing {}", relative(uri));
					File f = nexus.download(uri);
					byte[] signature = signer.sign(f);
					if (options.show())
						show(signature);
					else
						nexus.upload(new URI(uri + ".asc"), signature);
				} catch (Exception e) {
					exception(e, "could not download/sign/upload %s", relative(uri));
				}
			}
		} else {
			for (String arg : args) {
				File f = getFile(arg);
				if (!f.isFile()) {
					error("Can't find file %s", f);
				} else {
					byte[] signature = signer.sign(f);
					if (options.show())
						show(signature);
					else {
						File out = new File(f.getParentFile(), f.getName() + ".asc");
						IO.store(signature, out);
					}
				}
			}
		}
	}

	private void show(byte[] signature) throws IOException {
		System.out.write(signature);
	}

	public URI relative(URI uri) {
		return this.options.url()
			.relativize(uri);
	}

	@Arguments(arg = {})
	interface FilesOption extends Options {

	}

	public void _files(FilesOption options) throws Exception {
		URI base = this.options.url();
		for (URI uri : nexus.files()) {
			System.out.printf("%s%n", base.relativize(uri));
		}
	}

}
