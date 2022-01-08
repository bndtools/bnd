package bndtools.central;

import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This component adds whiteboard facility to IQueryListeners.
 */
@Component(immediate = true)
public class QueryWhiteboard {
	final IWorkbench wb;

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addQueryListener(IQueryListener listener) {
		NewSearchUI.addQueryListener(listener);
	}

	void removeQueryListener(IQueryListener listener) {
		NewSearchUI.removeQueryListener(listener);
	}

	@Activate
	public QueryWhiteboard(@Reference
	IWorkbench wb) {
		this.wb = wb;
	}
}
