package org.osgi.service.bindex.impl;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Version;
import org.osgi.service.bindex.Capability;
import org.osgi.service.bindex.Requirement;
import org.osgi.service.bindex.ResourceIndexer;

public class ResourceIndexerImpl implements ResourceIndexer {
	
	private final BundleAnalyzer analyzer = new BundleAnalyzer();

	public void index(Set<File> files, Writer out, Map<String, String> config) throws Exception {
		// TODO Auto-generated method stub

	}

	public void indexFragment(Set<File> files, Writer out, Map<String, String> config) throws Exception {
		PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
		
		for (File file : files) {
			JarResource resource = new JarResource(file);
			List<Capability> caps = new LinkedList<Capability>();
			List<Requirement> reqs = new LinkedList<Requirement>();
			analyzer.analyseResource(resource, caps, reqs);
			
			Tag resourceTag = new Tag(Schema.ELEM_RESOURCE);
			for (Capability cap : caps) {
				Tag capTag = new Tag(Schema.ELEM_CAPABILITY);
				capTag.addAttribute(Schema.ATTR_NAMESPACE, cap.getNamespace());
				
				for (Entry<String, Object> attribEntry : cap.getAttributes().entrySet()) {
					Tag attribTag = new Tag(Schema.ELEM_ATTRIBUTE);
					attribTag.addAttribute(Schema.ATTR_NAME, attribEntry.getKey());
					
					Object value = attribEntry.getValue();
					String v = value.toString();
					
					if (value instanceof Version) {
						attribTag.addAttribute(Schema.ATTR_TYPE, Schema.TYPE_VERSION);
					}
					attribTag.addAttribute(Schema.ATTR_VALUE, v);
					capTag.addContent(attribTag);
				}
				
				resourceTag.addContent(capTag);
			}
			
			resourceTag.print(0 , pw);
		}
	}

}
