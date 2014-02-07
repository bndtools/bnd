package aQute.jpm.platform;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import aQute.jpm.lib.*;
import aQute.lib.io.*;

class MacOS extends Unix {
	static DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	static XPathFactory				xpf	= XPathFactory.newInstance();

	@Override
	public File getGlobal() {
		return new File("/Library/Java/PackageManager").getAbsoluteFile();
	}

	@Override
	public File getGlobalBinDir() {
		return new File("/usr/local/bin").getAbsoluteFile();
	}

	@Override
	public File getLocal() {
		return IO.getFile("~/Library/PackageManager").getAbsoluteFile();
	}

	@Override
	public void shell(String initial) throws Exception {
		run("open -n /Applications/Utilities/Terminal.app");
	}

	@Override
	public String getName() {
		return "MacOS";
	}

	@Override
	public String createCommand(CommandData data, Map<String,String> map, boolean force, String... extra)
			throws Exception {
		if (data.bin == null)
			data.bin = getExecutable(data);

		File f = new File(data.bin);
		if (f.isDirectory()) {
			f = new File(data.bin, data.name);
			data.bin = f.getAbsolutePath();
		}

		if (!force && f.exists())
			return "Command already exists " + data.bin;

		process("macos/command.sh", data, data.bin, map, extra);
		return null;
	}

	@Override
	public String createService(ServiceData data, Map<String,String> map, boolean force, String... extra)
			throws Exception {
		// File initd = getInitd(data);
		File launch = getLaunch(data);
		if (!force && launch.exists())
			return "Cannot create service " + data.name + " because it exists";

		process("macos/launch.sh", data, launch.getAbsolutePath(), map, add(extra, data.serviceLib));
		return null;
	}

	@Override
	public String deleteService(ServiceData data) {
		// File initd = getInitd(data);
		File launch = getLaunch(data);

		if (launch.exists() && !launch.delete())
			return "Cannot delete service " + data.name + " because it exists and cannot be deleted: " + launch;

		return null;
	}

	@Override
	public void installDaemon(boolean user) throws IOException {
		String dest = "~/Library/LaunchAgents/org.jpm4j.run.plist";
		if (!user) {
			dest = "/Library/LaunchAgents/org.jpm4j.run.plist";
		}
		IO.copy(getClass().getResource("macos/daemon.plist"), IO.getFile(dest));
	}

	@Override
	public void uninstallDaemon(boolean user) throws IOException {
		if (user)
			IO.delete(new File("~/Library/LaunchAgents/org.jpm4j.run.plist"));
		else
			IO.delete(new File("/Library/LaunchAgents/org.jpm4j.run.plist"));
	}

	@Override
	public void uninstall() throws IOException {}

	public String defaultCacertsPassword() {
		return "changeit";
	}

	public String toString() {
		return "MacOS/Darwin";
	}

	/**
	 * Return the VMs on the platform.
	 * 
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	@Override
	public void getVMs(Collection<JVM> vms) throws Exception {
		String paths[] = {
				"/System/Library/Java/JavaVirtualMachines", "/Library/Java/JavaVirtualMachines"
		};
		for (String path : paths) {
			for (File vmdir : new File(path).listFiles()) {
				JVM jvm = getJVM(vmdir);
				if (jvm != null)
					vms.add(jvm);
			}
		}
	}

	@Override
	public JVM getJVM(File vmdir) throws Exception {
		if (!vmdir.isDirectory()) {
			return null;
		}

		File contents = new File(vmdir, "Contents");
		if (!contents.isDirectory()) {
			reporter.trace("Found a directory %s, but it does not have the expected Contents directory", vmdir);
			return null;
		}

		File plist = new File(contents, "Info.plist");
		if (!plist.isFile()) {
			reporter.trace("The VM in %s has no Info.plist with the necessary details", vmdir);
			return null;
		}

		File home = new File(contents, "Home");
		String error = verifyVM(home);
		if ( error != null ) {
			reporter.error("Invalid vm directory for MacOS %s: %s", vmdir, error);
			return null;
		}
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		try {
			Document doc = db.parse(plist);
			XPath xp = xpf.newXPath();
			Node versionNode = (Node) xp.evaluate("//dict/key[text()='JVMVersion']", doc, XPathConstants.NODE);
			Node platformVersionNode = (Node) xp.evaluate("//dict/key[text()='JVMPlatformVersion']", doc,
					XPathConstants.NODE);
			Node vendorNode = (Node) xp.evaluate("//dict/key[text()='JVMVendor']", doc, XPathConstants.NODE);
			Node capabilitiesNode = (Node) xp
					.evaluate("//dict/key[text()='JVMCapabilities']", doc, XPathConstants.NODE);

			JVM jvm = new JVM();
			jvm.name = vmdir.getName();
			jvm.path = home.getCanonicalPath();
			jvm.platformRoot = vmdir.getCanonicalPath();
			jvm.version = getSiblingValue(versionNode);
			jvm.platformVersion = getSiblingValue(platformVersionNode);
			jvm.vendor = getSiblingValue(vendorNode);

			return jvm;
		}
		catch (Exception e) {
			reporter.trace("Could not parse the Info.plist in %s, got %s", vmdir, e);
			throw e;
		}
	}

	private String getSiblingValue(Node node) {
		if (node == null)
			return null;
		node = node.getNextSibling();
		while (node.getNodeType() == Node.TEXT_NODE)
			node = node.getNextSibling();

		return node.getTextContent();
	}

}
