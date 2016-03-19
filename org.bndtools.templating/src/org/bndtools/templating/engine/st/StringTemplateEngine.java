package org.bndtools.templating.engine.st;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.ResourceType;
import org.bndtools.templating.StringResource;
import org.bndtools.templating.TemplateEngine;
import org.osgi.service.component.annotations.Component;
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

@Component(name = "org.bndtools.templating.engine.st", property = {
        "name=stringtemplate", "version=4.0.8"
})
public class StringTemplateEngine implements TemplateEngine {

    private static final String TEMPLATE_PROPERTIES = "_template.properties";
    private static final String TEMPLATE_DEFS_PREFIX = "_defs/";
    private static final String TEMPLATE_FILE_SUFFIX = ".st";

    private static class TemplateSettings {
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

                String process = props.getProperty("process", Constants.DEFAULT_PREPROCESSS_MATCHERS);
                String processBefore = props.getProperty("process.before", null);
                if (processBefore != null)
                    process = processBefore + ", " + process;
                String processAfter = props.getProperty("process.after", null);
                if (processAfter != null)
                    process = process + ", " + processAfter;
                settings.preprocessMatch = new Instructions(process);

                String ignore = props.getProperty("ignore", null);
                settings.ignore = ignore != null ? new Instructions(ignore) : null;
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
    public Collection<String> getTemplateParameterNames(ResourceMap inputs) throws Exception {
        Set<String> names = new HashSet<>();

        // Initialise the engine
        TemplateSettings settings = readSettings(inputs);
        STGroup stg = new STGroup(settings.leftDelim, settings.rightDelim);

        // Assemble a mapping properties file of outputPath=sourcePath
        String mappingTemplate = loadMappingTemplate(inputs, settings, stg);
        Properties contentProps = new Properties();
        contentProps.load(new StringReader(mappingTemplate));

        // Iterate the entries
        @SuppressWarnings("unchecked")
        Enumeration<String> contentEnum = (Enumeration<String>) contentProps.propertyNames();
        while (contentEnum.hasMoreElements()) {
            String outputPath = contentEnum.nextElement().trim();
            collectTemplateParamNames(names, outputPath, settings);

            String sourcePath = contentProps.getProperty(outputPath);
            if (settings.ignore == null || !settings.ignore.matches(sourcePath)) {
                if (settings.preprocessMatch.matches(sourcePath)) {
                    Resource resource = inputs.get(sourcePath);
                    if (resource != null && resource.getType() == ResourceType.File) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getContent(), resource.getTextEncoding()))) {
                            String line = reader.readLine();
                            while (line != null) {
                                collectTemplateParamNames(names, line, settings);
                                line = reader.readLine();
                            }
                        }
                    }
                }
            }
        }

        return names;
    }

    private void collectTemplateParamNames(Set<String> names, String input, TemplateSettings settings) {
        StringBuilder builder = new StringBuilder();

        boolean inParamName = false;
        boolean inEscape = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inEscape) {
                if (inParamName)
                    builder.append(c);
                inEscape = false;
            } else {
                if (c == '\\')
                    inEscape = true;
                else if (inParamName) {
                    if (c == settings.rightDelim) {
                        // Completed a name. Add it to the result.
                        names.add(builder.toString());
                        inParamName = false;
                    } else {
                        builder.append(c);
                    }
                } else {
                    if (c == settings.leftDelim) {
                        // Starting a name. Reset the StringBuilder.
                        builder.setLength(0);
                        inParamName = true;
                    }
                }
            }
        }
    }

    @Override
    public ResourceMap generateOutputs(ResourceMap inputs, Map<String,List<Object>> parameters) throws Exception {
        TemplateSettings settings = readSettings(inputs);
        STGroup stg = new STGroup(settings.leftDelim, settings.rightDelim);

        // Assemble a mapping properties file of outputPath=sourcePath
        String mappingTemplate = loadMappingTemplate(inputs, settings, stg);
        String renderedMapping = compileAndRender(stg, "_mapping", new StringResource(mappingTemplate), parameters);

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
                if (source.getType() == ResourceType.Folder) {
                    output = source;
                } else if (settings.preprocessMatch.matches(sourceName)) {
                    // This file is a candidate for preprocessing with ST
                    String rendered = compileAndRender(stg, sourceName, source, parameters);
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

    private String loadMappingTemplate(ResourceMap inputs, TemplateSettings settings, STGroup stg) throws IOException {
        StringWriter buf = new StringWriter();
        PrintWriter bufPrint = new PrintWriter(buf);
        for (String inputPath : inputs.getPaths()) {
            if (inputPath.startsWith(TEMPLATE_DEFS_PREFIX)) {
                if (inputPath.endsWith(TEMPLATE_FILE_SUFFIX)) {
                    // Definition... load into StringTemplate group and don't generate output
                    String inputPathRelative = inputPath.substring(TEMPLATE_DEFS_PREFIX.length());

                    Resource resource = inputs.get(inputPath);
                    if (resource != null && resource.getType() == ResourceType.File)
                        loadTemplate(stg, inputPathRelative, resource.getContent(), resource.getTextEncoding());
                }
            } else {
                // Mapping to output file
                String outputPath = inputPath;
                String escapedSourcePath = escapeDelimiters(inputPath, settings);

                bufPrint.printf("%s=%s%n", outputPath, escapedSourcePath);
            }
        }
        bufPrint.close();
        String mappingTemplate = buf.toString();
        return mappingTemplate;
    }

    private TemplateSettings readSettings(ResourceMap inputs) throws IOException, UnsupportedEncodingException {
        Properties settingsProp = new Properties();
        Resource settingsResource = inputs.remove(TEMPLATE_PROPERTIES);
        if (settingsResource != null) {
            if (settingsResource.getType() != ResourceType.File)
                throw new IllegalArgumentException(String.format("Template settings resource %s must be a file; found resource type %s.", TEMPLATE_PROPERTIES, settingsResource.getType()));
            try (Reader reader = new InputStreamReader(settingsResource.getContent(), settingsResource.getTextEncoding())) {
                settingsProp.load(reader);
            }
        }
        TemplateSettings settings = TemplateSettings.readFrom(settingsProp);
        return settings;
    }

    private void loadTemplate(STGroup stg, String fileName, InputStream is, String encoding) throws IOException {
        ANTLRInputStream charStream = new ANTLRInputStream(is, encoding);
        charStream.name = fileName;
        try {
            stg.loadTemplateFile("/", fileName, charStream);
        } catch (NullPointerException e) {
            throw new IOException(String.format("Error loading template file %s. Ensure the template contains a template definition matching the file name.", fileName), e);
        }
    }

    private CompiledST loadRawTemplate(STGroup stg, String name, Resource resource) throws IOException {
        if (resource.getType() != ResourceType.File)
            throw new IllegalArgumentException(String.format("Cannot build resource from resource of type %s (name='%s').", resource.getType(), name));
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

    private String compileAndRender(STGroup group, String name, Resource resource, Map<String,List<Object>> params) throws Exception {
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

        for (Entry<String,List<Object>> entry : params.entrySet()) {
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
