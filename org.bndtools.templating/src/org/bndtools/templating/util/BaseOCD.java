package org.bndtools.templating.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.osgi.service.metatype.ObjectClassDefinition;

public abstract class BaseOCD implements ObjectClassDefinition {

    private final String name;
    private final String description;
    private final URI iconUri;

    public BaseOCD(String name, String description, URI iconUri) {
        this.name = name;
        this.description = description;
        this.iconUri = iconUri;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getID() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public InputStream getIcon(int size) throws IOException {
        return iconUri != null ? iconUri.toURL()
            .openStream() : null;
    }

}
