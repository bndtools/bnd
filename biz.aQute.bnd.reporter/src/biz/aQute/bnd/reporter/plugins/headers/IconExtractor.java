package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.IconDTO;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class IconExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleIcons";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_ICON, false);
		final List<IconDTO> icons = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final IconDTO icon = new IconDTO();
			
			icon.url = cleanKey(entry.getKey());
			
			if (entry.getValue().containsKey("size")) {
				icon.size = Integer.valueOf(entry.getValue().get("size"));
			}
			icons.add(icon);
		}
		if (!icons.isEmpty()) {
			result = icons;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
