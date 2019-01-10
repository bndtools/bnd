package org.bndtools.templating;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import aQute.lib.io.IO;

public class FileResource implements Resource {

    private final File file;
    private final String encoding;

    public FileResource(File file, String encoding) {
        this.file = file;
        this.encoding = encoding;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.File;
    }

    @Override
    public InputStream getContent() throws IOException {
        return IO.stream(file);
    }

    @Override
    public String getTextEncoding() {
        return encoding;
    }

}
