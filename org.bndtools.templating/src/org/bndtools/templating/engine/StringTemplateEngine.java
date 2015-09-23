package org.bndtools.templating.engine;

import java.io.IOException;
import java.io.InputStream;
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
import org.stringtemplate.v4.compiler.CompiledST;
import org.stringtemplate.v4.compiler.Compiler;
import org.stringtemplate.v4.compiler.STLexer;
import org.stringtemplate.v4.misc.ErrorBuffer;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instructions;
import st4hidden.org.antlr.runtime.ANTLRInputStream;
import st4hidden.org.antlr.runtime.CommonToken;

public class StringTemplateEngine implements TemplateEngine {
	
	private static final String TEMPLATE_PROPERTIES = "_template.properties";
	private static final String TEMPLATE_DEFS_PREFIX = "_defs/";
	
	static class TemplateSettings {
		char leftDelim = '$';
		char rightDelim = '$';
		Instructions preprocessMatch = new Instructions("*");
		Instructions ignore = null;
		
		private TemplateSettings() {}
		
		static TemplateSettings readFrom(Properties props) {
			TemplateSettings settings = new TemplateSettings();
			if (props != null) {
				settings.leftDelim = readSingleChar(props, "leftDelim", settings.leftDelim);
				settings.rightDelim = readSingleChar(props, "rightDelim", settings.rightDelim);
				
				String match = props.getProperty("match", Constants.DEFAULT_PREPROCESSS_MATCHERS);
				String matchExtra = props.getProperty("match-extra", null);
				if (matchExtra != null)
					match = matchExtra + ", " + match;
				settings.preprocessMatch = new Instructions(match);
				
				String ignore = props.getProperty("ignore", null);
				settings.ignore = ignore != null? new Instructions(ignore) : null;
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
			if (inputPath.startsWith(TEMPLATE_DEFS_PREFIX)) {
				// Definition... load into StringTemplate group and don't generate output
				String inputPathRelative = inputPath.substring(TEMPLATE_DEFS_PREFIX.length());
				
				Resource resource = inputs.get(inputPath);
				loadTemplate(stg, inputPathRelative, resource.getContent(), resource.getTextEncoding());
			} else {
				// Mapping to output file
				String outputPath = inputPath;
				String escapedSourcePath = escapeDelimiters(inputPath, settings);
				
				bufPrint.printf("%s=%s%n", outputPath, escapedSourcePath);
			}
		}
		bufPrint.close();
		String mappingTemplate = buf.toString();
		String renderedMapping = compileAndRender(stg, "_mapping", new StringResource(mappingTemplate), parameters, settings);
		
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
			
			if (settings.ignore == null || !settings.ignore.matches(sourceName)) {
				if (settings.preprocessMatch.matches(sourceName)) {
					// This file is a candidate for preprocessing with ST
					String rendered = compileAndRender(stg, sourceName, source, parameters, settings);
					output = new StringResource(rendered);
				} else {
					// This file should be directly copied
					output = source;
				}
				outputs.put(outputName, output);
			}
		}

		return outputs;
	}

	private void loadTemplate(STGroup stg, String fileName, InputStream is, String encoding) throws IOException {
		ANTLRInputStream charStream = new ANTLRInputStream(is, encoding);
		charStream.name = fileName;
		stg.loadTemplateFile("/", fileName, charStream);
	}
	
	private CompiledST loadRawTemplate(STGroup stg, String name, Resource resource) throws IOException {
		try (InputStream is = resource.getContent()) {
			ANTLRInputStream templateStream = new ANTLRInputStream(is, resource.getTextEncoding());
			String template = templateStream.substring(0, templateStream.size() - 1);
			CompiledST impl = new Compiler(stg).compile(name, template);
			CommonToken nameT = new CommonToken(STLexer.SEMI);
			nameT.setInputStream(templateStream);
			stg.rawDefineTemplate("/" + name, impl, nameT);
			impl.defineImplicitlyDefinedTemplates(stg);
			return impl;
		}
	}
	
	private String compileAndRender(STGroup group, String name, Resource resource, Map<String, List<Object>> params, TemplateSettings settings) throws Exception {
		ErrorBuffer errors = new ErrorBuffer();
		group.setListener(errors);

		ST st;
		try {
			loadRawTemplate(group, name, resource);
			st = group.getInstanceOf(name);
		} catch (Exception e) {
			// Wrap the ST exception, which gives us no detail in its message
			throw new IllegalArgumentException(String.format("Failed to compile template '%s': %s", name, errors.toString()));
		}

		if (st == null)
			throw new Exception("Template name not loaded: " + name);

		for (Entry<String, List<Object>> entry : params.entrySet()) {
			for (Object value : entry.getValue()) {
				st.add(entry.getKey(), value);
			}
		}
		return st.render();
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
