package bndtools.javasearch;

import java.util.Arrays;
import java.util.Optional;

import org.bndtools.api.IStartupParticipant;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;

import bndtools.central.Central;

public class AddQueryListenerStartupParticipant implements IStartupParticipant, IQueryListener {

	@Override
	public void start() {
		NewSearchUI.addQueryListener(this);
	}

	@Override
	public void stop() {
		NewSearchUI.removeQueryListener(this);
	}

	@Override
	public void queryAdded(ISearchQuery query) {
	}

	@Override
	public void queryRemoved(ISearchQuery query) {
	}

	@Override
	public void queryStarting(ISearchQuery query) {
	}

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
					.filter(IType.class::isInstance)
					.map(IType.class::cast)
					.filter(IMember::isBinary)
					.filter(type -> Optional.ofNullable(type.getPath())
						.map(IPath::lastSegment)
						.filter(path -> path.endsWith(".jar"))
						.isPresent())
					.filter(type -> {
						return Optional.ofNullable(type.getResource())
							.map(IResource::getProject)
							.filter(Central::isBndProject)
							.map(project -> {
								IResource member = ResourcesPlugin.getWorkspace()
									.getRoot()
									.findMember(type.getPath());
								return member != null && member.isDerived(IResource.CHECK_ANCESTORS);
							})
							.orElse(false);
					})
					.flatMap(type -> Arrays.stream(result.getMatches(
						type)))
					.forEach(result::removeMatch);
			}
		}
	}

}
