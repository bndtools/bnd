package bndtools.refactor;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.core.refactoring.Change;

public class RemoveProjectNatureChange extends ProjectNatureChange {

	private final String	natureId;
	private final String	name;

	public RemoveProjectNatureChange(IProject project, String natureId, String name) {
		super(project);
		this.natureId = natureId;
		this.name = name;
	}

	@Override
	public String getName() {
		return "Remove project nature: " + name;
	}

	@Override
	protected Change createInverse() {
		return new AddProjectNatureChange(project, natureId, name);
	}

	@Override
	protected boolean modifyNatures(Set<String> natures) {
		return natures.remove(natureId);
	}
}
