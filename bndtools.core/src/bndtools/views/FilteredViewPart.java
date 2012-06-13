package bndtools.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

/**
 * An abstract base class for views that contain a textual filter field at the top, which can be hidden or revealed by
 * toggling a button in the toolbar.
 */
public abstract class FilteredViewPart extends ViewPart {

    private Text txtFilter;
    private Action filterAction;

    private Composite stackPanel;
    private StackLayout stack;
    private Composite topPanel;
    private Composite mainPanel;

    /**
     * Implements {@link ViewPart#createPartControl(Composite)}
     */
    @Override
    public final void createPartControl(Composite parent) {
        // Create controls
        stackPanel = new Composite(parent, SWT.NONE);
        stack = new StackLayout();
        stackPanel.setLayout(stack);

        topPanel = new Composite(stackPanel, SWT.NONE);

        // Filter panel
        Composite filterPanel = new Composite(topPanel, SWT.NONE);
        new Label(filterPanel, SWT.NONE).setText("Filter:");
        txtFilter = new Text(filterPanel, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        txtFilter.setMessage("enter search string");

        // Main panel
        mainPanel = new Composite(stackPanel, SWT.NONE);
        createMainControl(mainPanel);

        // Add listeners
        txtFilter.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.detail == SWT.CANCEL)
                    updatedFilter("");
                else
                    updatedFilter(txtFilter.getText());
            }
        });

        // Layout
        stack.topControl = mainPanel;

        GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        topPanel.setLayout(layout);

        filterPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        filterPanel.setLayout(new GridLayout(2, false));
        txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        mainPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        // Toolbar
        createActions();
        fillToolBar(getViewSite().getActionBars().getToolBarManager());
    }

    private void createActions() {
        filterAction = new FilterAction();
    }

    /**
     * Fill the view toolbar. Subclasses may override but must call <code>super.fillToolBar</code>
     * 
     * @param toolBar
     *            The toolbar manager supplied by the workbench
     */
    protected void fillToolBar(IToolBarManager toolBar) {
        toolBar.add(filterAction);
    }

    /**
     * Create the main content of the view, below the filter bar.
     * 
     * @param container
     *            The parent composite for the main content. Subclasses should set an appropriate layout on this
     *            composite.
     */
    protected abstract void createMainControl(Composite container);

    /**
     * Called when the filter string is modified by the user. Subclasses should implement this method to apply the
     * filter to the controls they create.
     * 
     * @param filterString
     *            The new filter string, or an empty string ("") if there is no filter (e.g., because the user hid the
     *            filter bar).
     */
    protected abstract void updatedFilter(String filterString);

    /*
     * The filter toggle button
     */
    private class FilterAction extends Action {
        public FilterAction() {
            super("Filter", IAction.AS_CHECK_BOX);
            setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/filter.gif"));
        }

        @Override
        public void run() {
            if (filterAction.isChecked()) {
                stack.topControl = topPanel;
                mainPanel.setParent(topPanel);
                updatedFilter(txtFilter.getText());
            } else {
                stack.topControl = mainPanel;
                mainPanel.setParent(stackPanel);
                updatedFilter("");
            }
            stackPanel.layout(true, true);
            setFocus();
        }
    }

    @Override
    public void setFocus() {
        if (filterAction.isChecked()) {
            txtFilter.setFocus();
        } else {
            doSetFocus();
        }
    }

    /**
     * Called when the view receives keyboard focus. Subclasses should implement to control
     */
    protected void doSetFocus() {}
}