package aQute.bnd.signing;

import static aQute.bnd.osgi.Jar.METAINF_SIGNING_P;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.SignerPlugin;
import aQute.bnd.stream.MapStream;
import aQute.lib.io.IO;
import aQute.libg.command.Command;
import aQute.service.reporter.Reporter;

/**
 * Sign the jar file. -sign : <alias> [ ';' 'password:=' <password> ] [ ';'
 * 'keystore:=' <keystore> ] [ ';' 'sign-password:=' <pw> ] ( ',' ... )*
 *
 * @author aqute
 */

@aQute.bnd.annotation.plugin.BndPlugin(name = "Signer", parameters = JartoolSigner.Config.class)
public class JartoolSigner implements Plugin, SignerPlugin, RegistryPlugin {
	private final static Logger logger = LoggerFactory.getLogger(JartoolSigner.class);

	@interface Config {
		String keystore();

		String storetype() default "JKS";

		String path() default "jarsigner";

		String storepass() default "";

		String keypass() default "";

		String sigFile() default "";

		String digestalg() default "";

		String tsa() default "";

		String tsacert() default "";

		String tsapolicyid() default "";
	}

	String	keystore;
	String	storetype;
	String	path	= "jarsigner";
	String	storepass;
	String	keypass;
	String	sigFile;
	String	digestalg;
	String	tsa;
	String	tsacert;
	String	tsapolicyid;

	private Processor	processor;

	@Override
	public void setProperties(Map<String, String> map) {
		if (map.containsKey("keystore"))
			this.keystore = map.get("keystore");
		if (map.containsKey("storetype"))
			this.storetype = map.get("storetype");
		if (map.containsKey("storepass"))
			this.storepass = map.get("storepass");
		if (map.containsKey("keypass"))
			this.keypass = map.get("keypass");
		if (map.containsKey("path"))
			this.path = map.get("path");
		if (map.containsKey("sigFile"))
			this.sigFile = map.get("sigFile");
		if (map.containsKey("digestalg"))
			this.digestalg = map.get("digestalg");
		if (map.containsKey("tsa"))
			this.tsa = map.get("tsa");
		if (map.containsKey("tsacert"))
			this.tsacert = map.get("tsacert");
		if (map.containsKey("tsapolicyid"))
			this.tsapolicyid = map.get("tsapolicyid");
	}

	@Override
	public void setReporter(Reporter processor) {}

	@Override
	public void setRegistry(Registry registry) {
		processor = registry.getPlugin(Processor.class);
	}

	@Override
	public void sign(Builder builder, String alias) throws Exception {
		File f = builder.getFile(keystore);
		if (!f.isFile()) {
			builder.error("Invalid keystore %s", f.getAbsolutePath());
			return;
		}

		Jar jar = builder.getJar();
		if (!jar.getManifestName()
			.equals(JarFile.MANIFEST_NAME)) {
			builder.error("Signing requires using the standard manifest name %s", JarFile.MANIFEST_NAME);
			return;
		}
		File tmp = File.createTempFile("signedjar", ".jar");
		tmp.deleteOnExit();

		jar.write(tmp);

		Command command = new Command();
		if ((path == null) || path.equals("jarsigner")) {
			if (processor != null) {
				path = processor.getJavaExecutable("jarsigner");
			} else {
				path = IO.getJavaExecutablePath("jarsigner");
			}
		}
		command.add(path);
		if (keystore != null) {
			command.add("-keystore");
			command.add(IO.absolutePath(f));
		}

		if (storetype != null) {
			command.add("-storetype");
			command.add(storetype);
		}

		if (keypass != null) {
			command.add("-keypass");
			command.add(keypass);
		}

		if (storepass != null) {
			command.add("-storepass");
			command.add(storepass);
		}

		if (sigFile != null) {
			command.add("-sigFile");
			command.add(sigFile);
		}

		if (digestalg != null) {
			command.add("-digestalg");
			command.add(digestalg);
		}

		if (tsa != null) {
			command.add("-tsa");
			command.add(tsa);
		}

		if (tsacert != null) {
			command.add("-tsacert");
			command.add(tsacert);
		}

		if (tsapolicyid != null) {
			command.add("-tsapolicyid");
			command.add(tsapolicyid);
		}

		command.add(IO.absolutePath(tmp));
		command.add(alias);
		logger.debug("Jarsigner command: {}", command);
		command.setTimeout(20, TimeUnit.SECONDS);
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		int exitValue = command.execute(out, err);
		if (exitValue != 0) {
			builder.error("Signing Jar out: %s%nerr: %s", out, err);
		} else {
			logger.debug("Signing Jar out: {}\nerr: {}", out, err);
		}

		Jar signed = new Jar(tmp);
		builder.addClose(signed);

		MapStream.of(signed.getDirectory("META-INF"))
			.filterKey(path -> JarFile.MANIFEST_NAME.equals(path) || METAINF_SIGNING_P.matcher(path)
				.matches())
			.forEachOrdered(jar::putResource);
		jar.setDoNotTouchManifest();
	}

	StringBuilder collect(final InputStream in) throws Exception {
		final StringBuilder sb = new StringBuilder();

		Thread tin = new Thread() {
			@Override
			public void run() {
				try {
					try (BufferedReader rdr = IO.reader(in, Constants.DEFAULT_CHARSET)) {
						String line = rdr.readLine();
						while (line != null) {
							sb.append(line);
							line = rdr.readLine();
						}
					}
				} catch (Exception e) {
					// Ignore any exceptions
				}
			}
		};
		tin.start();
		return sb;
	}
}
