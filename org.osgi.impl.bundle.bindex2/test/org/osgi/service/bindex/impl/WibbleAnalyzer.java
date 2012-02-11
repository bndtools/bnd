package org.osgi.service.bindex.impl;

import java.util.List;

import org.osgi.service.bindex.Builder;
import org.osgi.service.bindex.Capability;
import org.osgi.service.bindex.Requirement;
import org.osgi.service.bindex.Resource;
import org.osgi.service.bindex.ResourceAnalyzer;

public class WibbleAnalyzer implements ResourceAnalyzer {

	public void analyzeResource(Resource resource, List<? super Capability> capabilities, List<? super Requirement> requirements) throws Exception {
		capabilities.add(new Builder().setNamespace("wibble").buildCapability());
	}

}
