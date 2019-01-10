package org.bndtools.templating;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class URLResource implements Resource {

    private final URL url;
    private final String encoding;

    public URLResource(URL url, String encoding) {
        this.url = url;
        this.encoding = encoding;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.File;
    }

    @Override
    public InputStream getContent() throws IOException {
        return url.openStream();
    }

    @Override
    public String getTextEncoding() {
        return encoding;
    }

}
