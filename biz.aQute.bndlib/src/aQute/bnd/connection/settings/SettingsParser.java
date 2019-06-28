package aQute.bnd.connection.settings;

import java.io.File;

import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.lib.xpath.XPathParser;

public class SettingsParser extends XPathParser {
	final SettingsDTO	settings	= new SettingsDTO();
	private final Macro	replacer	= new Processor().getReplacer();

	/*
	 * <proxies> <proxy> <id>example-proxy</id> <active>true</active>
	 * <protocol>http</protocol> <host>proxy.example.com</host>
	 * <port>8080</port> <username>proxyuser</username>
	 * <password>somepassword</password>
	 * <nonProxyHosts>www.google.com|*.example.com</nonProxyHosts> </proxy>
	 * </proxies>
	 */

	public SettingsParser(File file) throws Exception {
		super(file);

		parse("/settings/proxies/proxy", ProxyDTO.class, settings.proxies);
		parse("/settings/servers/server", ServerDTO.class, settings.servers);
	}

	public SettingsDTO getSettings() {
		return settings;
	}

	@Override
	protected String processValue(String value) {
		return replacer.process(value);
	}
}
