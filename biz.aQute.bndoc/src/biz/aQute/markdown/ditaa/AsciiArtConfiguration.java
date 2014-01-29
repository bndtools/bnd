package biz.aQute.markdown.ditaa;

import aQute.lib.env.*;
import biz.aQute.markdown.*;

public interface AsciiArtConfiguration extends Configuration {

	float quality_scale();
	
	Header symbols();
}
