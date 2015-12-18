package aQute.libg.sax;

import org.xml.sax.ContentHandler;

public interface ContentFilter extends ContentHandler {
	void setParent(ContentHandler parent);

	ContentHandler getParent();
}
