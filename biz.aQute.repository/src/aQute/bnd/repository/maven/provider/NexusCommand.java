package aQute.bnd.repository.maven.provider;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.collections.Iterables;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.Digest;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.maven.nexus.provider.Nexus;
import aQute.maven.nexus.provider.Nexus.Asset;
import aQute.maven.nexus.provider.Signer;

public class NexusCommand extends Processor {
	private final static Logger	logger	= LoggerFactory.getLogger(NexusCommand.class);

	private NexusOptions		options;
	private Nexus				nexus;
	final HttpClient			client;
	final Crawler				crawler;

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
		use(parent);
		this.options = options;
		client = new HttpClient();
		client.readSettings(parent);
		this.crawler = new Crawler(client, getPromiseFactory());
		if (this.options.url() == null) {
			nexus = null;
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

		URI from();

		String include();

		String xclude();

		int threads();

	}

	public void _sign(SignOptions options) throws Exception {
		if (!checkNexus())
			return;

		String password = null;
		if (options.password() == null) {
			Console console = System.console();
			if (console == null) {
				error("No -p/--password set for PGP key and no console to ask");
			} else {
				char[] pw = console.readPassword("Passsword for pgp: ");
				if (pw == null || pw.length == 0) {
					error("Password not entered");
				}
				password = new String(pw);
			}
		} else
			password = options.password();

		Signer signer = new Signer(new String(password), options.command(getProperty("gpg", System.getenv("GPG"))));

		if (password == null || !isOk())
			return;

		List<String> args = options._arguments();

		if (args.isEmpty()) {

			List<URI> files;
			final URI from;
			final URI to = nexus.getUri();

			if (options.from() != null) {
				URI fromx = options.from()
					.toString()
					.endsWith("/") ? options.from() : new URI(options.from() + "/");
				from = fromx.normalize();
				files = crawler.getURIs(from, Crawler.predicate(options.include(), options.xclude()));
				trace("retrieving files from ", from);
			} else {
				files = nexus.files();
				from = nexus.getUri();
			}

			if (files == null) {
				error("URI is not reachable %s", nexus.getUri());
			} else {
				trace("from %s to %s %s files", from, to, files.size());

				int n = 2;
				Semaphore threads = new Semaphore(Math.max(options.threads(), 1));

				List<Promise<Void>> promises = new ArrayList<>();
				PromiseFactory pf = getPromiseFactory();
				CopyOnWriteArrayList<URI> inFlight = new CopyOnWriteArrayList<>();

				for (URI source : files) {

					while (true) {
						if (threads.tryAcquire(5, TimeUnit.SECONDS))
							break;
						trace("in flight %s", inFlight);
					}

					inFlight.add(source);

					Promise<Void> promise = pf.submit(() -> {
						try {
							sign(options, signer, from, to, source);
							return null;
						} finally {
							inFlight.remove(source);
							threads.release();
						}
					});
					promises.add(promise);

					//
					// The staging area is time sensitive, if we push
					// multiple files it creates multiple areas
					// so the first n files we're careful
					//

					if (n-- > 0) {
						promise.getValue();
					}
				}
				Promises.all(promises)
					.getValue();
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

	private void sign(SignOptions options, Signer signer, URI from, URI to, URI source) {
		String path = source.getPath();
		if (path.endsWith(".sha1") || path.endsWith(".asc") || path.endsWith(".md5"))
			return;

		try {

			URI relativized = from.relativize(source);
			URI dest = to.resolve(relativized);

			trace("signing %s", relativized);

			File f = nexus.download(source);

			trace("received %s size %s", f, f.length());

			byte[] signature = signer.sign(f);
			if (options.show())
				show(signature);
			else {
				URI asc = new URI(dest + ".asc");
				trace("signed upload %s", asc);
				nexus.upload(asc, signature);

				if (!dest.equals(source)) {
					trace("source upload %s", dest);
					nexus.upload(dest, IO.read(f));
					SHA1 sha1 = SHA1.digest(f);
					MD5 md5 = MD5.digest(f);
					trace("digests %s SHA1+MD5 %s %s", dest, sha1.asHex(), md5.asHex());
					nexus.upload(new URI(dest + ".sha1"), digest(sha1));
					nexus.upload(new URI(dest + ".md5"), digest(md5));
				}
			}
		} catch (Exception e) {
			exception(e, "could not download/sign/upload %s", relative(source));
		}
	}

	private byte[] digest(Digest sha1) {
		return (sha1.asHex() + "\n").getBytes(StandardCharsets.UTF_8);
	}

	private boolean checkNexus() {
		if (nexus == null) {
			error("The -u option to define the maven repo to use was not given");
			return false;
		}
		return true;
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
	interface FilesOption extends Options {
		@Description("A resource URI is only include if the include pattern appears in the path and the exclude does not appear")
		String include();

		@Description("A resource URI is only include if the include pattern appears in the path and the exclude does not appear")
		String exclude();

		boolean relative();
	}

	public void _files(FilesOption options) throws Exception {
		List<String> _arguments = options._arguments();
		if (_arguments.isEmpty()) {
			oldstyle();
		} else {
			List<URI> result = new ArrayList<>();
			for (String arg : _arguments) {
				URI uri = new URI(arg);
				if (!uri.isAbsolute()) {
					if (nexus == null) {
						error("Not an absolute URI %s", uri);
						continue;
					} else {
						uri = nexus.getUri()
							.resolve(arg);
						if (!uri.isAbsolute()) {
							error("Not an absolute URI %s", uri);
							continue;
						}
					}
				}
				assert uri.isAbsolute();

				List<URI> urIs = crawler.getURIs(uri, Crawler.predicate(options.include(), options.exclude()));
				result.addAll(urIs);
			}

			if (options.relative() && nexus != null) {
				URI root = nexus.getUri();
				result = result.stream()
					.map(root::relativize)
					.collect(Collectors.toList());
			}
			System.out.println(Strings.join("\n", result));
		}
	}

	private void oldstyle() throws Exception {
		if (!checkNexus())
			return;

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
