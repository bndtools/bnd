package biz.aQute.bnd.pde.launch2bndrun;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.EE;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import aQute.bnd.properties.IDocument;
import aQute.lib.io.IO;

public class LaunchToBndrun {

	VersionedClause			framework;
	List<VersionedClause>	bundles				= new ArrayList<>();
	List<VersionedClause>	bundlesDecorator	= new ArrayList<>();

	BndEditModel			model;
	IDocument				doc;

	public LaunchToBndrun(int defaultStartLevel, InputStream is) throws Exception {
		this.doc = new aQute.bnd.properties.Document("");

		try {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document xmlDocument = builder.parse(is);

			XPath xPath = XPathFactory.newInstance()
				.newXPath();
			String expression;
			NodeList nodeList;

			expression = "/launchConfiguration";
			Node node = (Node) xPath.compile(expression)
				.evaluate(xmlDocument, XPathConstants.NODE);

			if (node == null) {
				throw new IllegalArgumentException("No root <launchConfiguration> found");
			}

			// Workspace bundles first
			expression = "/launchConfiguration/setAttribute[@key='selected_workspace_bundles']/setEntry";
			nodeList = (NodeList) xPath.compile(expression)
				.evaluate(xmlDocument, XPathConstants.NODESET);
			for (int i = 0; i < nodeList.getLength(); i++) {
				node = nodeList.item(i);
				String value = node.getAttributes()
					.getNamedItem("value")
					.getNodeValue();
				launchBundleToVersionedClause(value, true);
			}
			expression = "/launchConfiguration/setAttribute[@key='selected_target_bundles']/setEntry";
			nodeList = (NodeList) xPath.compile(expression)
				.evaluate(xmlDocument, XPathConstants.NODESET);
			for (int i = 0; i < nodeList.getLength(); i++) {
				node = nodeList.item(i);
				String value = node.getAttributes()
					.getNamedItem("value")
					.getNodeValue();
				launchBundleToVersionedClause(value, false);
			}
			expression = "/launchConfiguration/stringAttribute[@key='org.eclipse.jdt.launching.VM_ARGUMENTS']/@value";
			String vmArgs = (String) xPath.compile(expression)
				.evaluate(xmlDocument, XPathConstants.STRING);

			// TODO: quote params with commas?
			model = new BndEditModel();
			model.setRunVMArgs(Stream.of(vmArgs.split("\\s+"))
				.filter(x -> !x.startsWith("-D"))
				.collect(Collectors.joining(", ")));
			model.setRunProperties(Stream.of(vmArgs.split("\\s+"))
				.filter(x -> x.startsWith("-D"))
				.map(x -> x.substring(2)
					.split("="))
				.collect(Collectors.toMap(x -> x[0], x -> x[1])));
			if (framework != null) {
				model.setRunFw(framework.toString());
			}
			model.setRunBundles(bundles);
			Attrs attrs = new Attrs();
			attrs.put(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, String.valueOf(defaultStartLevel));
			VersionedClause defaultRunLevel = new VersionedClause("*", attrs);
			bundlesDecorator.add(defaultRunLevel);
			model.setRunBundlesDecorator(bundlesDecorator);

			expression = "/launchConfiguration/stringAttribute[@key='org.eclipse.jdt.launching.PROGRAM_ARGUMENTS']/@value";
			String programArgs = (String) xPath.compile(expression)
				.evaluate(xmlDocument, XPathConstants.STRING);
			model.setRunProgramArgs(Stream.of(programArgs.split("\\s+"))
				.collect(Collectors.joining(", ")));

			expression = "/launchConfiguration/stringAttribute[@key='org.eclipse.jdt.launching.JRE_CONTAINER']/@value";
			String jre = (String) xPath.compile(expression)
				.evaluate(xmlDocument, XPathConstants.STRING);

			Pattern p = Pattern.compile("StandardVMType/([^/]*)$");
			Matcher m = p.matcher(jre);
			if (m.find()) {
				model.setEE(EE.parse(m.group(1)));
			}
			model.saveChangesTo(doc);
		} catch (XPathExpressionException e) {
			// This should never happen as our expressions are all constants
			throw new RuntimeException("Internal error", e);
		}
	}

	public BndEditModel getModel() {
		return model;
	}

	public IDocument getDoc() {
		return doc;
	}

	public String getContents() {
		return getDoc().get();
	}

	public void writeTo(Path file) throws UnsupportedEncodingException, IOException {
		Files.write(file, getContents().getBytes("utf-8"));
	}

	void launchBundleToVersionedClause(String bundle, boolean ws) {

		int index = bundle.indexOf('@');

		String bundleVersion = index < 0 ? bundle : bundle.substring(0, index);

		int versionIndex = bundleVersion.indexOf('*');

		String bsn = versionIndex < 0 ? bundleVersion : bundleVersion.substring(0, versionIndex);

		VersionedClause vc = new VersionedClause(bsn);
		if (versionIndex > 0) {
			final String versionRange = bundleVersion.substring(versionIndex + 1);
			vc.setVersionRange("[" + versionRange + "," + versionRange + "]");
		} else if (ws) {
			vc.setVersionRange("snapshot");
		}
		if (index > 0) {
			String runlevelInfo = bundle.substring(index + 1);
			String[] bits = runlevelInfo.split(":");
			switch (bits[0]) {
				case "-1" :
					framework = vc;
					break;
				case "default" :
					bundles.add(vc);
					break;
				default :
					VersionedClause decorator = vc.clone();
					Attrs attrs = decorator.getAttribs();
					attrs.remove(Constants.VERSION_ATTRIBUTE);
					attrs.put(Constants.RUNBUNDLES_STARTLEVEL_ATTRIBUTE, bits[0]);
					bundlesDecorator.add(decorator);
					bundles.add(vc);
					break;
			}
		} else {
			bundles.add(vc);
		}
	}

	public static void main(String[] args) {
		Path path = Path
			.of("C:\\Users\\fr.jeremy\\git\\idempiere\\org.adempiere.server-feature\\server.product.launch");
		LaunchToBndrun bndrun;
		try {
			bndrun = new LaunchToBndrun(4, IO.stream(path));
			System.err.println("bndrun: " + bndrun.getContents());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
