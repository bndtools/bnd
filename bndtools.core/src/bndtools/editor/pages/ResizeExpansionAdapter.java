package bndtools.editor.pages;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;

public class ResizeExpansionAdapter extends ExpansionAdapter {

	private final Composite	layoutParent;
	private final Control	control;

	public ResizeExpansionAdapter(Control control) {
		this(control.getParent(), control);
	}

	public ResizeExpansionAdapter(Composite layoutParent, Control control) {
		this.layoutParent = layoutParent;
		this.control = control;
	}

	@Override
	public void expansionStateChanged(ExpansionEvent e) {
		Object layoutData = (Boolean.TRUE.equals(e.data)) ? PageLayoutUtils.createExpanded()
			: PageLayoutUtils.createCollapsed();
		control.setLayoutData(layoutData);
		layoutParent.layout(true, true);
	}

}
