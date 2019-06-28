package bndtools.model.resolution;

import java.util.Collection;

import org.osgi.resource.Requirement;

public class RequirementWrapper {

	public Requirement					requirement;
	public boolean						resolved;
	public Collection<? extends Object>	requirers;

}
