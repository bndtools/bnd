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
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.ErrorBuffer;

import aQute.lib.io.IO;

public class StringTemplateEngine implements TemplateEngine {
	
	private static final String TEMPLATE_PROPERTIES = "_template.properties";
	private static final String EXTENSION_ST = ".st";
	
	static class TemplateSettings {
		char leftDelim = '$';
		char rightDelim = '$';
		
		static TemplateSettings readFrom(Properties props) {
			TemplateSettings settings = new TemplateSettings();
			if (props != null) {
				settings.leftDelim = readSingleChar(props, "leftDelim", settings.leftDelim);
				settings.rightDelim = readSingleChar(props, "rightDelim", settings.rightDelim);
			}
			return settings;
		}

		private static char readSingleChar(Properties props, String propName, char dflt) {
			String str = props.getProperty(propName, String.valueOf(dflt)).trim();
			if (str.length() != 1)
				throw new IllegalArgumentException("Setting value for " + propName + "must be a single character");
			return str.charAt(0);
		}
	}
	
	private String compileAndRender(STGroup group, String name, String template, Map<String, List<Object>> params, TemplateSettings settings) throws Exception {
		ErrorBuffer errors = new ErrorBuffer();
		group.setListener(errors);

		ST st;
		try {
			st = new ST(group, template);
		} catch (Exception e) {
			// Wrap the ST exception, which gives us no detail in its message
			throw new IllegalArgumentException(String.format("Failed to compile template '%s': %s", name, errors.toString()));
		}
		for (Entry<String, List<Object>> entry : params.entrySet()) {
			for (Object value : entry.getValue()) {
				st.add(entry.getKey(), value);
			}
		}
		return st.render();
	}

	@Override
	public ResourceMap generateOutputs(ResourceMap inputs, Map<String, List<Object>> parameters) throws Exception {
		// Load general template settings
		Properties settingsProp = new Properties();
		Resource settingsResource = inputs.remove(TEMPLATE_PROPERTIES);
		if (settingsResource != null) {
			try (Reader reader = new InputStreamReader(settingsResource.getContent(), settingsResource.getTextEncoding())) {
				settingsProp.load(reader);
			}
		}
		TemplateSettings settings = TemplateSettings.readFrom(settingsProp);
		
		STGroup stg = new STGroup(settings.leftDelim, settings.rightDelim);
		
		// Assemble a mapping properties file of outputPath=sourcePath
		StringWriter buf = new StringWriter();
		PrintWriter bufPrint = new PrintWriter(buf);
		for (String inputPath : inputs.getPaths()) {
			String outputPath = removeSTExtension(inputPath);
			String escapedSourcePath = escapeDelimiters(inputPath, settings);
			
			bufPrint.printf("%s=%s%n", outputPath, escapedSourcePath);
		}
		bufPrint.close();
		String mappingTemplate = buf.toString();
		String renderedMapping = compileAndRender(stg, "content mapping", mappingTemplate, parameters, settings);
		
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
				String rendered = compileAndRender(stg, sourceName, sourceTemplate, parameters, settings);
				output = new StringResource(rendered);
			} else {
				output = source;
			}
			outputs.put(outputName, output);
		}

		return outputs;
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

	static String escapeDelimiters(String string, TemplateSettings settings) {
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (settings.leftDelim == c || settings.rightDelim == c) {
				builder.append('\\');
			}
			builder.append(c);
		}
		
		return builder.toString();
	}
	
}
