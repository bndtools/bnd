package aQute.bnd.repository.maven.provider;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.collections.Iterables;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA256;
import aQute.maven.nexus.provider.Nexus;
import aQute.maven.nexus.provider.Nexus.Asset;
import aQute.maven.nexus.provider.Signer;

public class NexusCommand extends Processor {
	private final static Logger	logger	= LoggerFactory.getLogger(NexusCommand.class);

	private NexusOptions		options;
	private Nexus				nexus;
	final HttpClient			client;

	public enum Compatible {
		CRAWL,
		NEXUS2,
		NEXUS3
	}

	@ProviderType
	public interface NexusOptions extends Options {
		URI url();

		Compatible compatible(Compatible deflt);
	}

	@SuppressWarnings("resource")
	public NexusCommand(Processor parent, NexusOptions options) throws Exception {
		super(parent);
		this.options = options;
		client = new HttpClient();
		client.readSettings(parent);
		if (this.options.url() == null) {
			error("No -u/--url set");
			System.out.println(options._help());
			nexus = null;
			return;
		} else {
			this.nexus = new Nexus(options.url(), client, Processor.getExecutor());
		}
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
			if (files == null) {
				error("URI is not reachable %s", nexus.getUri());
			} else {
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

	@Arguments(arg = {
		"files..."
	})
	interface FilesOption extends Options {}

	public void _files(FilesOption options) throws Exception {
		List<URI> uris = getFiles();
		if (uris == null) {
			error("URI is not reachable %s", nexus.getUri());
		} else {
			System.out.println(Strings.join("\n", uris));
		}
	}

	private List<URI> getFiles() throws Exception {
		List<URI> list = new ArrayList<>();

		switch (this.options.compatible(Compatible.NEXUS3)) {
			case CRAWL :
				logger.debug("files in crawl mode");
				list.addAll(nexus.crawl("zip,jar,par"));
				break;
			case NEXUS2 :
				logger.debug("files in compatible mode");
				URI base = this.options.url();
				list.addAll(nexus.files());
				break;
			default :
			case NEXUS3 :
				for (String repo : options._arguments()) {
					nexus.getAssets(repo)
						.stream()
						.map((Asset asset) -> asset.downloadUrl)
						.forEach(list::add);
				}
				break;
		}
		return list;
	}

	interface IndexOptions extends Options {
		String name();

		URI referal();

		int depth();

		String output();
	}

	public void _index(IndexOptions options) throws Exception {
		ResourcesRepository repo = new ResourcesRepository();

		List<URI> files = getFiles();
		logger.debug("index files : {}", files);
		files.forEach(jar -> parse(repo, jar));

		XMLResourceGenerator xrg = new XMLResourceGenerator().indent(2);
		if (options.name() != null)
			xrg.name(options.name());
		if (options.referal() != null)
			xrg.referral(options.referal(), options.depth() <= 0 ? 3 : options.depth());
		xrg.repository(repo);

		if (options.output() != null) {
			File f = IO.getFile(options.output());
			f.getParentFile()
				.mkdirs();
			xrg.compress()
				.save(f);
		} else {
			xrg.indent(2);
			xrg.save(System.out);
		}
	}

	private void parse(ResourcesRepository repo, URI jar) {
		if (!isInteresting(jar.getPath()))
			return;

		try {
			File go = client.build()
				.get()
				.useCache()
				.go(jar);
			if (jar.getPath()
				.endsWith(".jar")) {
				parseJar(repo, jar, go);
			} else {
				parseZip(repo, jar, go);
			}
		} catch (javax.net.ssl.SSLHandshakeException e) {
			error("Failed to open URL due to ssl verification shit: %s %s", jar, e.getMessage());
		} catch (Exception e) {
			exception(e, "failed to parse file %s", jar);
		}
	}

	private void parseZip(ResourcesRepository repo, URI jar, File go)
		throws IOException, MalformedURLException, URISyntaxException, ZipException {

		String base = jar.toString() + "!/";
		IO.copy(jar.toURL(), go);
		try (ZipFile zip = new ZipFile(go)) {
			for (ZipEntry entry : Iterables.iterable(zip.entries())) {
				String entryName = entry.getName();
				if (entryName.endsWith(".jar")) {
					parse(repo, new URI(base + entryName));
				}
			}
		} catch (javax.net.ssl.SSLHandshakeException e) {
			error("Failed to open URL due to ssl verification shit: %s %s", jar, e.getMessage());
		}
	}

	private void parseJar(ResourcesRepository repo, URI jar, File go)
		throws IOException, NoSuchAlgorithmException, Exception {
		SHA256.digest(go);
		ResourceBuilder rb = new ResourceBuilder();
		rb.addFile(go, jar);
		Resource resource = rb.build();
		repo.add(resource);
	}

	private boolean isInteresting(String path) {
		if (path.endsWith("-javadoc.jar"))
			return false;
		if (path.endsWith("-sources.jar"))
			return false;

		return path.endsWith(".jar") || path.endsWith(".zip") || path.endsWith(".par");
	}
}
