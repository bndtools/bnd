package org.bndtools.core.obr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import aQute.bnd.service.url.TaggedData;
import aQute.bnd.service.url.URLConnector;

public class SimpleURLConnector implements URLConnector {

    public InputStream connect(URL url) throws IOException {
        return connectTagged(url).getInputStream();
    }

    public TaggedData connectTagged(URL url) throws IOException {
        return connectTagged(url, null);
    }

    public TaggedData connectTagged(URL url, String tag) throws IOException {
        return new TaggedData(null, url.openStream());
    }

}
