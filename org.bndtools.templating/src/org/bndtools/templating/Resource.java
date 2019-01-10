package org.bndtools.templating;

import java.io.IOException;
import java.io.InputStream;

public interface Resource {

    /**
     * Return the type of the resource.
     */
    ResourceType getType();

    /**
     * Get the content of the resource. May return <code>null</code> if the resource type is not
     * {@link ResourceType#File}.
     *
     * @throws IOException
     */
    InputStream getContent() throws IOException;

    /**
     * Get the content encoding of the resource.
     */
    String getTextEncoding();

}
