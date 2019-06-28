package aQute.p2.provider;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.lib.strings.Strings;

/**
 * <pre>
 * {@code
 * <?xml version='1.0' encoding='UTF-8'?>
 * <?compositeArtifactRepository version='1.0.0'?>
 * <repository name='&quot;Eclipse Project Test Site&quot;'
 *     type=
'org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version
='1.0.0'>
 *   <properties size='1'>
 *     <property name='p2.timestamp' value='1243822502440'/>
 *   </properties>
 *   <children size='2'>
 *     <child location='childOne'/>
 *     <child location='childTwo'/>
 *   </children>
 * </repository>}
 * </pre>
 */

class CompositeArtifacts extends XML {
	final List<URI>	uris	= new ArrayList<>();
	final URI		base;

	CompositeArtifacts(InputStream in, URI base) throws Exception {
		super(getDocument(in));
		this.base = base;
	}

	void parse() throws Exception {
		NodeList nodes = getNodes("repository/children/child");
		for (int i = 0; i < nodes.getLength(); i++) {
			Node child = nodes.item(i);
			String textContent = Strings.trim(getAttribute(child, "location"));
			URI uri = base.resolve(textContent);
			uris.add(uri);
		}
	}

}
