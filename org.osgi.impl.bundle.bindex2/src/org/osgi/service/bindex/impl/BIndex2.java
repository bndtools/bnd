package org.osgi.service.bindex.impl;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Filter;
import static org.osgi.framework.FrameworkUtil.createFilter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.bindex.Capability;
import org.osgi.service.bindex.Requirement;
import org.osgi.service.bindex.ResourceAnalyzer;
import org.osgi.service.bindex.ResourceIndexer;

public class BIndex2 implements ResourceIndexer {
	
	static final String REPOSITORY_INCREMENT_OVERRIDE = "-repository.increment.override";
	
	private List<Pair<ResourceAnalyzer, Filter>> analyzers = new LinkedList<Pair<ResourceAnalyzer,Filter>>();
	
	public BIndex2() {
		try {
			Filter bundleFilter = createFilter("(name=*.jar)");
			addAnalyzer(new BundleAnalyzer(), bundleFilter);
		} catch (InvalidSyntaxException e) {
			// Can't happen...?
			throw new RuntimeException("Unexpected internal error compiling filter");
		}
	}
	
	public final void addAnalyzer(ResourceAnalyzer analyzer, Filter filter) {
		synchronized (analyzers) {
			analyzers.add(Pair.create(analyzer, filter));
		}
	}
	
	public final void removeAnalyzer(ResourceAnalyzer analyzer, Filter filter) {
		synchronized (analyzers) {
			analyzers.remove(Pair.create(analyzer, filter));
		}
	}

	public void index(Set<File> files, Writer out, Map<String, String> config) throws Exception {
		PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
		
		pw.print(Schema.XML_PROCESSING_INSTRUCTION);
		
		if (config == null)
			config = new HashMap<String, String>(0);
		
		Tag repoTag = new Tag(Schema.ELEM_REPOSITORY);
		
		String repoName = config.get(REPOSITORY_NAME);
		if (repoName == null)
			repoName = REPOSITORYNAME_DEFAULT;
		repoTag.addAttribute(Schema.ATTR_NAME, repoName);
		
		String increment = config.get(REPOSITORY_INCREMENT_OVERRIDE);
		if (increment == null)
			increment = Long.toString(System.currentTimeMillis());
		repoTag.addAttribute(Schema.ATTR_INCREMENT, increment);
		
		repoTag.addAttribute(Schema.ATTR_XML_NAMESPACE, Schema.NAMESPACE);
		
		repoTag.printOpen(0, pw, false);
		for (File file : files) {
			Tag resourceTag = generateResource(file, config);
			resourceTag.print(2, pw);
		}
		repoTag.printClose(0, pw);
		pw.flush(); pw.close();
	}

	public void indexFragment(Set<File> files, Writer out, Map<String, String> config) throws Exception {
		PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
		
		for (File file : files) {
			Tag resourceTag = generateResource(file, config);
			resourceTag.print(0 , pw);
		}
	}
	
	private Tag generateResource(File file, Map<String, String> config) throws Exception {
		JarResource resource = new JarResource(file);
		List<Capability> caps = new LinkedList<Capability>();
		List<Requirement> reqs = new LinkedList<Requirement>();
		
		// Iterate over the analyzers
		synchronized (analyzers) {
			for (Pair<ResourceAnalyzer, Filter> entry : analyzers) {
				ResourceAnalyzer analyzer = entry.getFirst();
				Filter filter = entry.getSecond();
				
				if (filter == null || filter.match(resource.getProperties())) {
					analyzer.analyzeResource(resource, caps, reqs);
				}
			}
		}
		
		Tag resourceTag = new Tag(Schema.ELEM_RESOURCE);
		for (Capability cap : caps) {
			Tag capTag = new Tag(Schema.ELEM_CAPABILITY);
			capTag.addAttribute(Schema.ATTR_NAMESPACE, cap.getNamespace());
			
			appendAttributeAndDirectiveTags(capTag, cap.getAttributes(), cap.getDirectives());
			
			resourceTag.addContent(capTag);
		}
		
		for (Requirement req : reqs) {
			Tag reqTag = new Tag(Schema.ELEM_REQUIREMENT);
			reqTag.addAttribute(Schema.ATTR_NAMESPACE, req.getNamespace());
			
			appendAttributeAndDirectiveTags(reqTag, req.getAttributes(), req.getDirectives());
			
			resourceTag.addContent(reqTag);
		}
		
		return resourceTag;
	}
	
	static void appendAttributeAndDirectiveTags(Tag parentTag, Map<String, Object> attribs, Map<String, String> directives) {
		for (Entry<String, Object> attribEntry : attribs.entrySet()) {
			Tag attribTag = new Tag(Schema.ELEM_ATTRIBUTE);
			attribTag.addAttribute(Schema.ATTR_NAME, attribEntry.getKey());

			TypedAttribute typedAttrib = TypedAttribute.create(attribEntry.getKey(), attribEntry.getValue());
			parentTag.addContent(typedAttrib.toXML());
		}
		
		for (Entry<String, String> directiveEntry : directives.entrySet()) {
			Tag directiveTag = new Tag(Schema.ELEM_DIRECTIVE);
			directiveTag.addAttribute(Schema.ATTR_NAME, directiveEntry.getKey());
			directiveTag.addAttribute(Schema.ATTR_VALUE, directiveEntry.getValue());
			parentTag.addContent(directiveTag);
		}
	}
	
}
