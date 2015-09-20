package org.bndtools.templating.engine;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.StringResource;
import org.bndtools.templating.TemplateEngine;
import org.stringtemplate.v4.ST;

import aQute.lib.io.IO;

public class StringTemplateEngine implements TemplateEngine {
	
	private static final String TEMPLATE_PROPERTIES = "_template.properties";
	private static final String EXTENSION_ST = ".st";

	@Override
	public ResourceMap generateOutputs(ResourceMap inputs, Map<String, List<Object>> parameters) throws Exception {
		// Load general template settings
		Properties settings = new Properties();
		Resource settingsResource = inputs.remove(TEMPLATE_PROPERTIES);
		if (settingsResource != null) {
			try (Reader reader = new InputStreamReader(settingsResource.getContent(), settingsResource.getTextEncoding())) {
				settings.load(reader);
			}
		}
		char leftDelim = readSingleChar(settings, "leftDelim", '$');
		char rightDelim = readSingleChar(settings, "rightDelim", '$');
		
		// Assemble a mapping properties file of outputPath=sourcePath
		StringWriter buf = new StringWriter();
		PrintWriter bufPrint = new PrintWriter(buf);
		for (String inputPath : inputs.getPaths()) {
			String outputPath = removeSTExtension(inputPath);
			String escapedSourcePath = escapeDelimiters(inputPath, leftDelim, rightDelim);
			
			bufPrint.printf("%s=%s%n", outputPath, escapedSourcePath);
		}
		bufPrint.close();
		String mappingTemplate = buf.toString();
		ST mappingSt = new ST(mappingTemplate, leftDelim, rightDelim);
		applyParameters(mappingSt, parameters);
		String renderedMapping = mappingSt.render();
		
		Properties contentProps = new Properties();
		contentProps.load(new StringReader(renderedMapping));

		// Iterate the content entries
		ResourceMap outputs = new ResourceMap();
		@SuppressWarnings("unchecked")
		Enumeration<String> contentEnum = (Enumeration<String>) contentProps.propertyNames();
		while (contentEnum.hasMoreElements()) {
			String outputName = contentEnum.nextElement().trim();
			String sourceName = contentProps.getProperty(outputName, "").trim();

			Resource source = inputs.get(sourceName);
			if (source == null)
				throw new RuntimeException(String.format("Internal error in template engine: could not find input resource '%s'", sourceName));
			Resource output;
			if (isTemplate(sourceName)) {
				String sourceTemplate = IO.collect(source.getContent(), source.getTextEncoding());
				ST st = new ST(sourceTemplate, leftDelim, rightDelim);
				applyParameters(st, parameters);
				String rendered = st.render();
				output = new StringResource(rendered);
			} else {
				output = source;
			}
			outputs.put(outputName, output);
		}

		return outputs;
	}

	private static char readSingleChar(Properties props, String propName, char dflt) {
		String str = props.getProperty(propName, String.valueOf(dflt)).trim();
		if (str.length() != 1)
			throw new IllegalArgumentException("Setting value for " + propName + "must be a single character");
		return str.charAt(0);
	}

	private static void applyParameters(ST st, Map<String, List<Object>> parameters) {
		for (Entry<String, List<Object>> entry : parameters.entrySet()) {
			for (Object value : entry.getValue()) {
				st.add(entry.getKey(), value);
			}
		}
	}

	private static boolean isTemplate(String path) {
		return path.endsWith(EXTENSION_ST);
	}

	private static String removeSTExtension(String path) {
		String result = path;
		if (isTemplate(path))
			result = result.substring(0, result.length() - EXTENSION_ST.length());
		return result;
	}

	static String escapeDelimiters(String string, char... delims) {
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);

			boolean isDelim = false;
			for (char delim : delims) {
				if (delim == c) {
					isDelim = true;
					break;
				}
			}
			
			if (isDelim)
				builder.append('\\');
			builder.append(c);
		}
		
		return builder.toString();
	}
	
}
