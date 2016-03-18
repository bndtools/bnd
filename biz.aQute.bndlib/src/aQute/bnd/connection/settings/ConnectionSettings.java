package aQute.bnd.connection.settings;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.url.ProxyHandler;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.bnd.url.BasicAuthentication;
import aQute.bnd.url.HttpsVerification;
import aQute.libg.glob.Glob;

public class ConnectionSettings extends Processor {
	private static final String	M2_SETTINGS_XML				= "~/.m2/settings.xml";
	private static final String	BND_CONNECTION_SETTINGS_XML	= "~/.bnd/connection-settings.xml";
	private static final String	CONNECTION_SETTINGS			= "-connection-settings";
	private HttpClient			client;

	public ConnectionSettings(Processor processor, HttpClient client) throws Exception {
		super(processor);
		this.client = client;
	}

	public void readSettings() throws Exception {
		Parameters connectionSettings = new Parameters(mergeProperties(CONNECTION_SETTINGS));
		if (connectionSettings.isEmpty()) {

			File file = aQute.lib.io.IO.getFile(BND_CONNECTION_SETTINGS_XML);
			if (!file.isFile()) {
				file = aQute.lib.io.IO.getFile(M2_SETTINGS_XML);
				if (!file.isFile()) {
					return;
				}
			}
			parse(file);
			return;
		}

		for (Map.Entry<String, Attrs> entry : connectionSettings.entrySet()) {

			String key = entry.getKey();
			if ("false".equalsIgnoreCase(key))
				continue;

			switch (key) {
			case "maven":
				key = M2_SETTINGS_XML;
				break;

			case "bnd":
				key = BND_CONNECTION_SETTINGS_XML;
				break;
			}

			boolean ignoreError = false;
			if (key.startsWith("-")) {
				ignoreError = true;
				key = key.substring(1);
			}

			File file = aQute.lib.io.IO.getFile(getParent().getPropertiesFile().getParentFile(), key);
			if (!file.isFile()) {

				if (!ignoreError) {
					SetLocation error = getParent()
							.error("Specified -connection-settings: %s, but no such file or is directory", file);
					FileLine header = getParent().getHeader(CONNECTION_SETTINGS, key);
					if (header != null)
						header.set(error);
				}
			} else
				parse(file);
		}

	}

	public URLConnectionHandler createUrlConnectionHandler(ServerDTO serverDTO) {

		final Glob match = new Glob(serverDTO.match == null ? serverDTO.id : serverDTO.match);
		final BasicAuthentication basic = getBasicAuthentication(serverDTO.username, serverDTO.password);
		final HttpsVerification https = new HttpsVerification(serverDTO.trust, serverDTO.verify, getParent());

		return new URLConnectionHandler() {

			@Override
			public boolean matches(URL url) {
				String scheme = url.getProtocol().toLowerCase();

				StringBuilder address = new StringBuilder();
				address.append(scheme).append("://").append(url.getHost());

				if (url.getPort() != url.getDefaultPort())
					address.append(":").append(url.getPort());

				return match.matcher(address).matches();
			}

			@Override
			public void handle(URLConnection connection) throws Exception {
				if (basic != null)
					basic.handle(connection);

				if (isHttps(connection) && https != null) {
					https.handle(connection);
				}

			}

			boolean isHttps(URLConnection connection) {
				return "https".equalsIgnoreCase(connection.getURL().getProtocol());
			}

			public String toString() {
				return "Server [ match=" + match + ", basic=" + basic + ", https=" + https + "]";
			}
		};
	}

	public BasicAuthentication getBasicAuthentication(String username, String password) {
		if (username != null && password != null) {
			return new BasicAuthentication(username, password, getParent());
		}
		return null;
	}

	/**
	 * Create Proxy Handler from ProxyDTO
	 */
	public static ProxyHandler createProxyHandler(final ProxyDTO proxyDTO) {
		return new ProxyHandler() {
			Glob globs[];
			private ProxySetup proxySetup;

			@Override
			public ProxySetup forURL(URL url) throws Exception {
				switch (proxyDTO.protocol) {

				case DIRECT:
					break;

				case HTTP:
					String scheme = url.getProtocol();
					if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
						// ok
					} else
						return null;

					break;

				case SOCKS:
					break;

				default:
					break;
				}

				String host = url.getHost();
				if (host != null) {

					if (isNonProxyHost(host))
						return null;

				}
				// not synchronized because conflicts only do some double work

				if (proxySetup == null) {
					proxySetup = new ProxySetup();
					if (proxyDTO.username != null && proxyDTO.password != null)
						proxySetup.authentication = new PasswordAuthentication(proxyDTO.username,
								proxyDTO.password.toCharArray());

					SocketAddress socketAddress;
					if (proxyDTO.host != null)
						socketAddress = new InetSocketAddress(proxyDTO.host, proxyDTO.port);
					else
						socketAddress = new InetSocketAddress(proxyDTO.port);

					proxySetup.proxy = new Proxy(proxyDTO.protocol, socketAddress);
				}
				return proxySetup;
			}

			public boolean isNonProxyHost(String host) {

				Glob[] globs = getNonProxyHosts(proxyDTO);

				for (Glob glob : globs) {
					if (glob.matcher(host).matches())
						return true;
				}
				return false;
			}

			public Glob[] getNonProxyHosts(final ProxyDTO proxyDTO) {
				// not synchronized because conflicts only do some double work
				if (globs == null) {
					if (proxyDTO.nonProxyHosts == null)
						globs = new Glob[0];
					else {
						String parts[] = proxyDTO.nonProxyHosts.split("\\s*\\|\\s*");
						globs = new Glob[parts.length];
						for (int i = 0; i < parts.length; i++)
							globs[i] = new Glob(parts[i]);
					}
				}
				return globs;
			}
		};
	}

	private void parse(File file) throws Exception {
		assert file != null : "File must be set";
		assert file.isFile() : "File must be a file and exist";

		SettingsParser parser = new SettingsParser(file);

		SettingsDTO settings = parser.getSettings();
		for (ProxyDTO proxyDTO : settings.proxies) {
			if (isActive(proxyDTO)) {

				add(proxyDTO);
			}
		}
		ServerDTO deflt = null;
		for (ServerDTO serverDTO : settings.servers) {
			serverDTO.trust = makeAbsolute(file, serverDTO.trust);

			if ("default".equals(serverDTO.id))
				deflt = serverDTO;
			else {
				add(serverDTO);
			}
		}

		if (deflt != null)
			add(deflt);

	}

	final static String	IPNR_PART_S	= "([01]\\d\\d)|(2[0-4]\\d)|(25[0-5])";
	final static String	IPNR_S		= IPNR_PART_S + "." + IPNR_PART_S + "." + IPNR_PART_S + "." + IPNR_PART_S;
	static Pattern		MASK_P		= Pattern.compile("(?<if>[^:]):(?<ip>[^/])/(?<valid>.*)");

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
					address = InetAddress.getByName(pp[0]).getAddress();
					maskLength = pp.length > 1 ? Integer.parseInt(pp[1]) : address.length * 8;

				}

				Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();

				while (e.hasMoreElements()) {
					NetworkInterface ni = e.nextElement();

					if (ni == null)
						continue;

					if (!ni.isUp())
						continue;

					if (g.matcher(ni.getName()).matches()) {

						if (address == null)
							return true;

						for (InterfaceAddress ia : ni.getInterfaceAddresses()) {

							byte[] iaa = ia.getAddress().getAddress();

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
				exception(e, "Failed to parse proxy 'mask' clause in settings: %s", clause);
			}

		return false;
	}

	public static String makeAbsolute(File cwd, String trust) {
		if (trust == null)
			return null;
		StringBuilder sb = new StringBuilder();
		String del = "";

		String[] parts = trust.split("\\s*,\\s*");
		for (String part : parts) {
			File file = new File(cwd, part).getAbsoluteFile();
			sb.append(del);
			del = ",";
			sb.append(file.getAbsolutePath());
		}
		return sb.toString();
	}

	public void add(ServerDTO server) {
		client.addURLConnectionHandler(createUrlConnectionHandler(server));
	}

	public void add(ProxyDTO proxy) {
		client.addProxyHandler(createProxyHandler(proxy));
	}

}
