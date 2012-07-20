package aQute.libg.sax;

import org.xml.sax.*;

public interface ContentFilter extends ContentHandler {
	void setParent(ContentHandler parent);

	ContentHandler getParent();
}
