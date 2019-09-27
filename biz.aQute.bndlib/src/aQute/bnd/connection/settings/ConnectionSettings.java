package aQute.bnd.connection.settings;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.service.url.ProxyHandler;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.bnd.url.BasicAuthentication;
import aQute.bnd.url.BearerAuthentication;
import aQute.bnd.url.HttpsVerification;
import aQute.bnd.util.home.Home;
import aQute.lib.collections.Iterables;
import aQute.lib.concurrentinit.ConcurrentInitialize;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.mavenpasswordobfuscator.MavenPasswordObfuscator;
import aQute.lib.strings.Strings;
import aQute.lib.xpath.XPathParser;
import aQute.libg.glob.Glob;
import aQute.service.reporter.Reporter.SetLocation;

public class ConnectionSettings {
	final static Logger							logger							= LoggerFactory
		.getLogger(ConnectionSettings.class);
	public static final String					M2_SETTINGS_SECURITY_XML		= "~/.m2/settings-security.xml";
	public static final String					M2_SETTINGS_SECURITY_PROPERTY	= "settings.security";
	private static final String					M2_SETTINGS_XML					= "~/.m2/settings.xml";
	private static final String					BND_CONNECTION_SETTINGS_XML		= Home.getUserHomeBnd()
		+ "/connection-settings.xml";
	private static final String					CONNECTION_SETTINGS				= "-connection-settings";
	private final Processor						processor;
	private final HttpClient					client;
	private final List<ServerDTO>				servers							= new ArrayList<>();
	private final ConcurrentInitialize<String>	mavenMasterPassphrase;
	private final List<String>					parsed							= new ArrayList<>();

	public ConnectionSettings(Processor processor, HttpClient client) throws Exception {
		this.processor = Objects.requireNonNull(processor);
		this.client = client;
		mavenMasterPassphrase = new MasterPassphrase(processor);
	}

	private static final class MasterPassphrase extends ConcurrentInitialize<String> {
		private final Processor processor;

		MasterPassphrase(Processor processor) {
			this.processor = processor;
		}

		@Override
		public String create() throws Exception {
			String path = System.getProperty(M2_SETTINGS_SECURITY_PROPERTY, M2_SETTINGS_SECURITY_XML);
			File file = IO.getFile(path);
			if (!file.isFile()) {
				logger.info("No Maven security settings file {}", path);
				return null;
			}

			XPathParser sp = new XPathParser(file);
			String master = sp.parse("/settingsSecurity/master");
			if (master == null || master.isEmpty()) {
				processor.warning("Found Maven security settings file %s but not master password in it", path);
				return null;
			} else {
				if (!MavenPasswordObfuscator.isObfuscatedPassword(master)) {
					processor.warning("Master password in %s was not obfuscated, using actual value", path);
					return master;
				}
				try {
					return MavenPasswordObfuscator.decrypt(master, M2_SETTINGS_SECURITY_PROPERTY);
				} catch (Exception e) {
					processor.exception(e, "Could not decrypt the master password from %s with key %s", path,
						M2_SETTINGS_SECURITY_PROPERTY);
					return null;
				}
			}
		}
	}

	public void readSettings() throws Exception {
		Parameters connectionSettings = new Parameters(processor.mergeProperties(CONNECTION_SETTINGS), processor);
		if (connectionSettings.isEmpty()) {
			File file = IO.getFile(BND_CONNECTION_SETTINGS_XML);
			if (!file.isFile()) {
				file = IO.getFile(M2_SETTINGS_XML);
				if (!file.isFile()) {
					return;
				}
			}
			parse(file);
			return;
		}

		List<File> tmps = new ArrayList<>();
		try {
			for (Map.Entry<String, Attrs> entry : connectionSettings.entrySet()) {
				String key = entry.getKey();
				if ("false".equalsIgnoreCase(key)) {
					continue;
				}

				switch (key) {
					case "maven" :
						key = M2_SETTINGS_XML;
						break;

					case "bnd" :
						key = BND_CONNECTION_SETTINGS_XML;
						break;

					case "env" :
						Attrs attrs = entry.getValue();
						String variable = attrs.get("var");
						if (variable == null) {
							processor.error(
								"Specified -connection-settings: %s, with 'env' but the 'var' parameter was not found",
								connectionSettings);
						} else {
							String value = System.getenv(variable);
							if (value != null) {
								File tmp = File.createTempFile(variable, ".xml");
								IO.store(value, tmp);
								key = IO.absolutePath(tmp);
								tmps.add(tmp);
							} else {
								processor.error(
									"Specified -connection-settings: %s, but no such environment variable %s is found",
									connectionSettings, variable);
							}
						}
						break;
				}

				boolean ignoreError = false;
				if (key.startsWith("-")) {
					ignoreError = true;
					key = key.substring(1);
				}

				key = Processor.removeDuplicateMarker(key);
				if ("server".equals(key)) {
					parseServer(entry.getValue());
				} else {
					File file = processor.getFile(key);
					if (!file.isFile()) {
						if (!ignoreError) {
							SetLocation error = processor
								.error("Specified -connection-settings: %s, but no such file or is directory", file);
							FileLine header = processor.getHeader(CONNECTION_SETTINGS, key);
							if (header != null)
								header.set(error);
						}
					} else {
						parse(file);
					}
				}
			}
		} finally {
			tmps.forEach(IO::delete);
		}

	}

	/**
	 * Set the parameters from within, i.e. not via file
	 *
	 * @param uri the uri that must match
	 * @param value the values
	 * @throws Exception
	 */
	private void parseServer(Attrs value) throws Exception {
		parsed.add("direct: " + value);
		ServerDTO server = Converter.cnv(ServerDTO.class, value);
		if (isBasicAuth(server) || isBearerAuth(server) || isPrivateKey(server) || isHttpsVerification(server)) {

			if (server.id == null)
				server.id = "*";

			add(server);
		}
	}

	private boolean isPrivateKey(ServerDTO server) {
		if (isEmpty(server.privateKey) || isEmpty(server.passphrase))
			return false;
		else
			return true;
	}

	private boolean isBasicAuth(ServerDTO server) {
		if (isEmpty(server.username) || isEmpty(server.password))
			return false;
		else
			return true;
	}

	private boolean isBearerAuth(ServerDTO server) {
		if (isEmpty(server.username) && !isEmpty(server.password))
			return true;
		else
			return false;
	}

	private boolean isHttpsVerification(ServerDTO server) {
		if (isEmpty(server.trust))
			return false;
		else
			return true;
	}

	private boolean isEmpty(String s) {
		return s == null || s.trim()
			.isEmpty();
	}

	public URLConnectionHandler createURLConnectionHandler(ServerDTO serverDTO) {
		return new SettingsURLConnectionHandler(serverDTO, processor);
	}

	private static final class SettingsURLConnectionHandler implements URLConnectionHandler {
		private final Glob					match;
		private final URLConnectionHandler	handler;
		private final URLConnectionHandler	https;

		SettingsURLConnectionHandler(ServerDTO serverDTO, Processor processor) {
			match = new Glob(serverDTO.match != null ? serverDTO.match : serverDTO.id);
			if (serverDTO.password == null) {
				handler = null;
			} else if (serverDTO.username != null) {
				handler = new BasicAuthentication(serverDTO.username, serverDTO.password, processor);
			} else {
				handler = new BearerAuthentication(serverDTO.password, processor);
			}

			// verify=false, trust.isEmpty -> void default check
			// verify=false, !trust.isEmpty -> ignore
			// verify=true, trust.isEmpty -> use default check
			// verify=true, !trust.isEmpty -> verify against given certs

			boolean hasCerts = serverDTO.trust != null && !serverDTO.trust.isEmpty();
			if (serverDTO.verify == false || hasCerts)
				https = new HttpsVerification(serverDTO.trust, serverDTO.verify, processor);
			else
				https = null; // verify & no certs ==> default
		}

		@Override
		public boolean matches(URL url) {
			String scheme = url.getProtocol()
				.toLowerCase();

			StringBuilder address = new StringBuilder();
			address.append(scheme)
				.append("://")
				.append(url.getHost());

			if (url.getPort() > 0 && url.getPort() != url.getDefaultPort())
				address.append(":")
					.append(url.getPort());

			return match.matcher(address)
				.matches();
		}

		@Override
		public void handle(URLConnection connection) throws Exception {
			if (handler != null) {
				handler.handle(connection);
			}

			if ((https != null) && isHttps(connection)) {
				https.handle(connection);
			}
		}

		private boolean isHttps(URLConnection connection) {
			return "https".equalsIgnoreCase(connection.getURL()
				.getProtocol());
		}

		@Override
		public String toString() {
			return "Server [ match=" + match + ", handler=" + handler + ", https=" + https + "]";
		}
	}

	/**
	 * Create Proxy Handler from ProxyDTO
	 */
	public ProxyHandler createProxyHandler(ProxyDTO proxyDTO) {
		return new SettingsProxyHandler(proxyDTO);
	}

	private static final class SettingsProxyHandler implements ProxyHandler {
		private final ProxyDTO	proxyDTO;
		private List<Glob>		globs;
		private ProxySetup		proxySetup;

		SettingsProxyHandler(ProxyDTO proxyDTO) {
			this.proxyDTO = proxyDTO;
		}

		@Override
		public ProxySetup forURL(URL url) throws Exception {
			Proxy.Type type;

			switch (proxyDTO.protocol.toUpperCase()) {

				case "DIRECT" :
					type = Type.DIRECT;
					break;

				case "HTTP" :
					type = Type.HTTP;
					if (url.getProtocol()
						.equalsIgnoreCase("http")) {
						// ok
					} else
						return null;

					break;
				case "HTTPS" :
					type = Type.HTTP;
					if (url.getProtocol()
						.equalsIgnoreCase("https")) {
						// ok
					} else
						return null;
					break;

				case "SOCKS" :
					type = Type.SOCKS;
					break;

				default :
					type = Type.HTTP;
					break;
			}

			if (isNonProxyHost(url.getHost())) {
				return null;
			}

			// not synchronized because conflicts only do some double work
			if (proxySetup == null) {
				final ProxySetup pendingProxySetup = new ProxySetup();
				if (proxyDTO.username != null && proxyDTO.password != null)
					pendingProxySetup.authentication = new PasswordAuthentication(proxyDTO.username,
						proxyDTO.password.toCharArray());

				SocketAddress socketAddress;
				if (proxyDTO.host != null)
					socketAddress = new InetSocketAddress(proxyDTO.host, proxyDTO.port);
				else
					socketAddress = new InetSocketAddress(proxyDTO.port);

				pendingProxySetup.proxy = new Proxy(type, socketAddress);

				proxySetup = pendingProxySetup;
			}
			return proxySetup;
		}

		private boolean isNonProxyHost(String host) {
			if (host == null) {
				return false;
			}
			return getNonProxyHosts().stream()
				.anyMatch(g -> g.matcher(host)
					.matches());
		}

		private List<Glob> getNonProxyHosts() {
			// not synchronized because conflicts only do some double work
			if (globs != null) {
				return globs;
			}
			if (proxyDTO.nonProxyHosts == null) {
				return globs = emptyList();
			}
			return globs = Processor.split(proxyDTO.nonProxyHosts, "\\s*\\|\\s*")
				.stream()
				.map(Glob::new)
				.collect(toList());
		}
	}

	private void parse(File file) throws Exception {
		try {
			assert file != null : "File must be set";
			assert file.isFile() : "File must be a file and exist";
			parsed.add(file.getAbsolutePath());
			SettingsParser parser = new SettingsParser(file);

			SettingsDTO settings = parser.getSettings();
			for (ProxyDTO proxyDTO : settings.proxies) {
				if (isActive(proxyDTO)) {

					add(proxyDTO);
				}
			}
			ServerDTO deflt = null;
			for (ServerDTO serverDTO : settings.servers) {
				serverDTO.trust = makeAbsolute(file.getParentFile(), serverDTO.trust);

				if (MavenPasswordObfuscator.isObfuscatedPassword(serverDTO.password)) {
					String masterPassphrase = mavenMasterPassphrase.get();
					if (masterPassphrase != null) {
						try {
							serverDTO.password = MavenPasswordObfuscator.decrypt(serverDTO.password, masterPassphrase);
						} catch (Exception e) {
							processor.exception(e, "Could not decrypt the password for server %s", serverDTO.id);
						}
					}
				}
				if ("default".equals(serverDTO.id))
					deflt = serverDTO;
				else {
					add(serverDTO);
				}
			}

			if (deflt != null)
				add(deflt);
		} catch (SAXParseException e) {
			processor.error("Invalid XML in connection settings for file : %s: %s", file, e.getMessage());
		}
	}

	private boolean isActive(ProxyDTO proxy) throws SocketException {
		if (!proxy.active)
			return false;

		String mask = proxy.mask;
		if (mask == null)
			return true;

		String[] clauses = mask.split("\\s*,\\s*");
		for (String clause : clauses)
			try {

				String parts[] = clause.split("\\s*:\\s*");

				Glob g = new Glob(parts[0]);
				byte[] address = null;
				int maskLength = 0;

				if (parts.length > 1) {

					String pp[] = parts[1].split("/");
					address = InetAddress.getByName(pp[0])
						.getAddress();
					maskLength = pp.length > 1 ? Integer.parseInt(pp[1]) : address.length * 8;

				}

				for (NetworkInterface ni : Iterables.iterable(NetworkInterface.getNetworkInterfaces())) {
					if (ni == null)
						continue;

					if (!ni.isUp())
						continue;

					if (g.matcher(ni.getName())
						.matches()) {

						if (address == null)
							return true;

						for (InterfaceAddress ia : ni.getInterfaceAddresses()) {

							byte[] iaa = ia.getAddress()
								.getAddress();

							if (address.length != iaa.length)
								continue;

							if (maskLength != 0 && ia.getNetworkPrefixLength() != maskLength)
								continue;

							if (Arrays.equals(address, iaa))
								return true;

						}
					}
				}

			} catch (Exception e) {
				processor.exception(e, "Failed to parse proxy 'mask' clause in settings: %s", clause);
			}

		return false;
	}

	public static String makeAbsolute(File cwd, String trust) {
		if (trust == null || trust.trim()
			.isEmpty())
			return null;

		return Processor.split(trust)
			.stream()
			.map(part -> resolve(cwd, part))
			.collect(joining(","));
	}

	static String resolve(File dir, String part) {
		return IO.getFile(dir, part)
			.getPath();
	}

	public void add(ServerDTO server) {
		servers.add(server);
		if (client != null)
			client.addURLConnectionHandler(createURLConnectionHandler(server));
	}

	public void add(ProxyDTO proxy) {
		if (client != null)
			client.addProxyHandler(createProxyHandler(proxy));
	}

	public List<ServerDTO> getServerDTOs() {
		return servers;
	}

	public List<String> getParsedFiles() {
		return parsed;
	}

	public void report(Formatter f) {
		f.format("-connection-settings          %s%n", processor.getProperty(CONNECTION_SETTINGS, "<>"));
		f.format("Parsed files:%n");

		getParsedFiles().forEach(file -> f.format("   %s%n", file));

		getServerDTOs().forEach(server -> {
			f.format("%n");
			f.format("Id                  %s%n", server.id);
			f.format("Username            %s%n", server.username);
			f.format("Password            %s%n", server.password == null ? "<>" : "*******");
			f.format("Private Key         %s%n", server.privateKey == null ? "<>" : "*******");
			f.format("Passphrase          %s%n", server.passphrase == null ? "<>" : "*******");
			if (server.trust != null && !server.trust.trim()
				.isEmpty()) {
				f.format("Trust%n");
				List<String> paths = Strings.split(server.trust);
				for (String path : paths)
					try {
						File file = new File(path);
						if (file.isFile()) {
							f.format("    %s%n", file);
							List<X509Certificate> certificates = new ArrayList<>();
							HttpsVerification.getCertificates(path, certificates);
							for (X509Certificate certificate : certificates) {
								f.format("        Subject     %s%n", certificate.getSubjectDN());
								byte[] bytes = certificate.getSerialNumber()
									.toByteArray();
								f.format("        Serial Nr   %s%n", Hex.separated(bytes, ":"));
								f.format("        Issuer      %s%n", certificate.getIssuerDN());
								f.format("        Type        %s%n", certificate.getType());
								try {
									certificate.checkValidity();
									f.format("        Valid       yes%n");
								} catch (Exception e) {
									f.format("        Valid       %s%n", e.getMessage());
								}
							}

						} else {
							f.format("    %s NO SUCH FILE%n", file);
						}
					} catch (Exception e) {
						f.format("        Unexpected connection settings  '%s'%n", Exceptions.causes(e));
					}
				f.format("Verify              %s%n", server.verify);
			}
		});
	}
}
