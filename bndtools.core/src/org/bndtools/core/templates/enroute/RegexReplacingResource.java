package org.bndtools.core.templates.enroute;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceType;
import org.bndtools.utils.copy.ResourceReplacer;

public class RegexReplacingResource implements Resource {

    private final URL url;
    private final Map<String,String> replaceRegularExpressions;
    private final String encoding;

    public RegexReplacingResource(URL url, Map<String,String> replaceRegularExpressions, String encoding) {
        this.replaceRegularExpressions = replaceRegularExpressions;
        this.url = url;
        this.encoding = encoding;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.File;
    }

    @Override
    public InputStream getContent() throws IOException {
        if (replaceRegularExpressions == null || replaceRegularExpressions.isEmpty())
            return url.openStream();

        ResourceReplacer replacer = new ResourceReplacer(replaceRegularExpressions, url);
        replacer.start();
        return replacer.getStream();
    }

    @Override
    public String getTextEncoding() {
        return encoding;
    }

}
