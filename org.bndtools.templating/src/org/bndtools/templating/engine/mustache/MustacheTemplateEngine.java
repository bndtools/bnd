package org.bndtools.templating.engine.mustache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.ResourceType;
import org.bndtools.templating.StringResource;
import org.bndtools.templating.TemplateEngine;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.service.component.annotations.Component;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instructions;

@Component(name = "org.bndtools.templating.engine.mustache", property = {
        "name=mustache", "version=0.8.18"
})
public class MustacheTemplateEngine implements TemplateEngine {

    private static final String TEMPLATE_PROPERTIES = "_template.properties";
    private static final String DEFAULT_LEFT_DELIM = "{{";
    private static final String DEFAULT_RIGHT_DELIM = "}}";

    private static class TemplateSettings {
        String leftDelim = DEFAULT_LEFT_DELIM;
        String rightDelim = DEFAULT_RIGHT_DELIM;
        Instructions preprocessMatch = new Instructions("*");
        Instructions ignore = null;

        private TemplateSettings() {}

        static TemplateSettings readFrom(Properties props) {
            TemplateSettings settings = new TemplateSettings();
            if (props != null) {
                settings.leftDelim = props.getProperty("leftDelim", DEFAULT_LEFT_DELIM);
                settings.rightDelim = props.getProperty("leftDelim", DEFAULT_RIGHT_DELIM);
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
    }

    private final DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();

    @Override
    public Collection<String> getTemplateParameterNames(ResourceMap inputs) throws Exception {
        return getTemplateParameterNames(inputs, new NullProgressMonitor());
    }

    @Override
    public Collection<String> getTemplateParameterNames(ResourceMap inputs, IProgressMonitor monitor) throws Exception {
        final List<String> paramNames = new LinkedList<>();
        TemplateSettings settings = readSettings(inputs);
        DefaultMustacheFactory factory = new DefaultMustacheFactory();
        factory.setObjectHandler(new ReflectionObjectHandler() {
            @Override
            public Wrapper find(String name, Object[] scopes) {
                paramNames.add(name);
                return super.find(name, scopes);
            }
        });
        int counter = 0;
        for (Entry<String,Resource> entry : inputs.entries()) {
            String inputPath = entry.getKey();
            factory.compile(new StringReader(inputPath), "mapping", settings.leftDelim, settings.rightDelim).execute(new StringWriter(), Collections.emptyMap());
            Resource source = entry.getValue();
            if (settings.ignore == null || !settings.ignore.matches(inputPath)) {
                if (source.getType() == ResourceType.File && settings.preprocessMatch.matches(inputPath)) {
                    InputStreamReader reader = new InputStreamReader(source.getContent(), source.getTextEncoding());
                    factory.compile(reader, "temp" + (counter++), settings.leftDelim, settings.rightDelim).execute(new StringWriter(), Collections.emptyMap()).toString();
                }
            }
        }
        return paramNames;
    }

    @Override
    public ResourceMap generateOutputs(ResourceMap inputs, Map<String,List<Object>> parameters) throws Exception {
        return generateOutputs(inputs, parameters, new NullProgressMonitor());
    }

    @Override
    public ResourceMap generateOutputs(ResourceMap inputs, Map<String,List<Object>> parameters, IProgressMonitor monitor) throws Exception {
        TemplateSettings settings = readSettings(inputs);

        ResourceMap outputs = new ResourceMap();
        Map<String,Object> flattenedParams = flattenParameters(parameters);

        for (Entry<String,Resource> entry : inputs.entries()) {
            String inputPath = entry.getKey();
            Resource source = entry.getValue();
            String outputPath = mustacheFactory.compile(new StringReader(inputPath), "mapping", settings.leftDelim, settings.rightDelim).execute(new StringWriter(), flattenedParams).toString();

            if (settings.ignore == null || !settings.ignore.matches(inputPath)) {
                Resource output;
                switch (source.getType()) {
                case Folder :
                    output = source;
                    break;
                case File :
                    if (settings.preprocessMatch.matches(inputPath)) {
                        // This file should be processed with the template engine
                        InputStreamReader reader = new InputStreamReader(source.getContent(), source.getTextEncoding());
                        String rendered = mustacheFactory.compile(reader, outputPath, settings.leftDelim, settings.rightDelim).execute(new StringWriter(), flattenedParams).toString();
                        output = new StringResource(rendered);
                    } else {
                        // This file should be directly copied
                        output = source;
                    }
                    break;
                default :
                    throw new IllegalArgumentException("Unknown resource type " + source.getType());
                }
                outputs.put(outputPath, output);
            }
        }

        return outputs;
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

    private Map<String,Object> flattenParameters(Map<String,List<Object>> parameters) {
        Map<String,Object> flattened = new HashMap<>(parameters.size());
        for (Entry<String,List<Object>> entry : parameters.entrySet()) {
            List<Object> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                if (list.size() == 1) {
                    flattened.put(entry.getKey(), list.get(0));
                } else {
                    flattened.put(entry.getKey(), list);
                }
            }
        }
        return flattened;
    }
}
