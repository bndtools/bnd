package org.bndtools.templating;

import java.io.IOException;
import java.io.InputStream;

public interface Resource {

	InputStream getContent() throws IOException;

	String getTextEncoding();

}
