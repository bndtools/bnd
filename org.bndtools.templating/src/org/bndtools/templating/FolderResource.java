package org.bndtools.templating;

import java.io.IOException;
import java.io.InputStream;

public class FolderResource implements Resource {

    @Override
    public ResourceType getType() {
        return ResourceType.Folder;
    }

    @Override
    public InputStream getContent() throws IOException {
        return null;
    }

    @Override
    public String getTextEncoding() {
        return null;
    }

}
