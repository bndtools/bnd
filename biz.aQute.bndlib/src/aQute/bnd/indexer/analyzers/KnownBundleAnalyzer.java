package aQute.bnd.indexer.analyzers;

import java.io.*;
import java.util.Map.Entry;
import java.util.*;

import aQute.bnd.header.*;
import aQute.bnd.indexer.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.version.*;
import aQute.lib.utf8properties.*;

public class KnownBundleAnalyzer implements ResourceAnalyzer {

	Processor p = new Processor();

	public KnownBundleAnalyzer() {
		Properties properties = new UTF8Properties();
		InputStream stream = KnownBundleAnalyzer.class.getResourceAsStream("known-bundles.properties");
		if (stream != null) {
			try {
				properties.load(stream);
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
		p.setProperties(properties);
	}

	public KnownBundleAnalyzer(Properties properties) {
		p.getProperties().putAll(properties);
	}

	public void setKnownBundlesExtra(Properties extras) {
		p.getProperties().putAll(extras);
	}

	public void analyzeResource(Jar resource, ResourceBuilder rb) throws Exception {
		
		String bsn = resource.getBsn();
		if ( bsn == null)
			return;
		
		Version version = Version.parseVersion( resource.getVersion());
		
		for (String key: p.getPropertyKeys(false)) {
			String[] parts = key.split(";");
			if ( ! bsn.equals(parts[0]))
				continue;

			if ( parts.length > 1) {
				if (parts[1].length()==0) {
					VersionRange range = new VersionRange(parts[1]);
					if ( ! range.includes(version))
						continue;
				}				
			}
			
			String type = null;
			if ( parts.length > 2) {
				type = parts[2];
			}
			
			Parameters capreq = new Parameters( p.getProperty(key));
			if ( type == null || "cap".equals(type)) 
				for ( Entry<String,Attrs> entry : capreq.entrySet()) {
					CapReqBuilder b = new CapReqBuilder(entry.getKey(), entry.getValue());
					rb.addCapability(b);			
				}
			else
				for ( Entry<String,Attrs> entry : capreq.entrySet()) {
					CapReqBuilder b = new CapReqBuilder(entry.getKey(), entry.getValue());
					rb.addRequirement(b);			
				}
		}

	}
}
