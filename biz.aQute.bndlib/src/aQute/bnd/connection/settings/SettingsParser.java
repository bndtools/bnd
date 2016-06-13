package aQute.bnd.connection.settings;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.osgi.dto.DTO;
import aQute.lib.converter.Converter;

public class SettingsParser {
	final static Converter				cnv			= new Converter();
	final static DocumentBuilderFactory	dbf			= DocumentBuilderFactory.newInstance();
	final static XPathFactory			xpf			= XPathFactory.newInstance();
	final SettingsDTO					settings	= new SettingsDTO();
	final XPath							xp;
	final Document						doc;

	/*
	 * <proxies> <proxy> <id>example-proxy</id> <active>true</active>
	 * <protocol>http</protocol> <host>proxy.example.com</host>
	 * <port>8080</port> <username>proxyuser</username>
	 * <password>somepassword</password>
	 * <nonProxyHosts>www.google.com|*.example.com</nonProxyHosts> </proxy>
	 * </proxies>
	 */

	public SettingsParser(File file) throws Exception {
		DocumentBuilder db = dbf.newDocumentBuilder();
		xp = xpf.newXPath();
		doc = db.parse(file);

		parse("/settings/proxies/proxy", ProxyDTO.class, settings.proxies);
		parse("/settings/servers/server", ServerDTO.class, settings.servers);
	}

	public SettingsDTO getSettings() {
		return settings;
	}

	<T extends DTO> void parse(String what, Class<T> type, List<T> map)
			throws XPathExpressionException, Exception {
		NodeList proxies = (NodeList) xp.evaluate(what, doc, XPathConstants.NODESET);
		for (int i = 0; i < proxies.getLength(); i++) {
			Node node = proxies.item(i);
			T dto = type.newInstance();
			parse(node, dto);
			map.add(dto);
		}
	}

	<T extends DTO> void parse(Node node, T dto) throws Exception {

		for (Field f : dto.getClass().getFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;

			String value = xp.evaluate(f.getName(), node);
			if (value == null || value.isEmpty())
				continue;

			if (f.getType().isAnnotation())
				value = value.toUpperCase();

			Object o = cnv.convert(f.getGenericType(), value);
			f.set(dto, o);
		}
	}

}
