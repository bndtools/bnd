package bndtools.refactor;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.core.refactoring.Change;

public class AddProjectNatureChange extends ProjectNatureChange {

	private final String	natureId;
	private final String	name;

	public AddProjectNatureChange(IProject project, String natureId, String name) {
		super(project);
		this.natureId = natureId;
		this.name = name;
	}

	@Override
	public String getName() {
		return "Add project nature: " + name;
	}

	@Override
	protected Change createInverse() {
		return new RemoveProjectNatureChange(project, natureId, name);
	}

	@Override
	protected boolean modifyNatures(Set<String> natures) {
		return natures.add(natureId);
	}
}
