package bndtools.javasearch;

import java.util.Arrays;
import java.util.Optional;

import org.bndtools.api.IStartupParticipant;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IMember;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import bndtools.central.Central;

public class JavaSearchStartupParticipant implements IStartupParticipant, IQueryListener {

	@Override
	public void start() {
		NewSearchUI.addQueryListener(this);
		createBndtoolsJavaWorkingSet();
	}

	private void createBndtoolsJavaWorkingSet() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
			.getWorkingSetManager();
		IWorkingSet workingSet = workingSetManager.getWorkingSet(BndtoolsJavaWorkingSetUpdater.WORKING_SET_NAME);
		if (workingSet == null) {
			workingSet = workingSetManager.createWorkingSet(BndtoolsJavaWorkingSetUpdater.WORKING_SET_NAME,
				new IAdaptable[0]);
			workingSet.setLabel(BndtoolsJavaWorkingSetUpdater.WORKING_SET_NAME);
			workingSet.setId(BndtoolsJavaWorkingSetUpdater.ID);
			workingSetManager.addWorkingSet(workingSet);
		}
	}

	@Override
	public void stop() {
		NewSearchUI.removeQueryListener(this);
	}

	@Override
	public void queryAdded(ISearchQuery query) {}

	@Override
	public void queryRemoved(ISearchQuery query) {}

	@Override
	public void queryStarting(ISearchQuery query) {}

	@Override
	public void queryFinished(ISearchQuery query) {
		String name = query.getClass()
			.getName();

		if (name.equals("org.eclipse.jdt.internal.ui.search.JavaSearchQuery")) {
			ISearchResult searchResult = query.getSearchResult();

			// try to remove search results for types that come from binary jars
			// in generated folder
			if (searchResult instanceof AbstractTextSearchResult) {
				AbstractTextSearchResult result = (AbstractTextSearchResult) searchResult;
				Arrays.stream(result.getElements())
					.filter(IMember.class::isInstance)
					.map(IMember.class::cast)
					.filter(IMember::isBinary)
					.filter(member -> Optional.ofNullable(member.getResource())
						.map(IResource::getProject)
						.filter(Central::isBndProject)
						.isPresent())
					.filter(member -> Optional.ofNullable(member.getPath())
						.filter(this::isDerived)
						.map(IPath::lastSegment)
						.filter(file -> file.endsWith(".jar"))
						.isPresent())
					.flatMap(member -> Arrays.stream(result.getMatches(member)))
					.forEach(result::removeMatch);
			}
		}
	}

	private boolean isDerived(IPath path) {
		IResource resource = ResourcesPlugin.getWorkspace()
			.getRoot()
			.findMember(path);
		return resource != null && resource.isDerived(IResource.CHECK_ANCESTORS);
	}
}
