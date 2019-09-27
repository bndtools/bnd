package bndtools.tasks;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;

import bndtools.model.resolution.RequirementWrapper;

public interface CapReqLoader extends Closeable {

	String getShortLabel();

	String getLongLabel();

	Map<String, List<Capability>> loadCapabilities() throws Exception;

	Map<String, List<RequirementWrapper>> loadRequirements() throws Exception;

}
