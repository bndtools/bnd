package org.bndtools.core.utils.swt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class FilterPanelPart {

    private static final String PROP_FILTER = "filter";
    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);

    private String filter;

    private Composite panel;
    private Text txtFilter;

    public Control createControl(Composite parent) {
        return createControl(parent, 0, 0);
    }

    public Control createControl(Composite parent, int marginWidth, int marginHeight) {
        // CREATE CONTROLS
        panel = new Composite(parent, SWT.NONE);
        new Label(panel, SWT.NONE).setText("Filter:");
        txtFilter = new Text(panel, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        txtFilter.setMessage("enter search string");

        // INITIAL PROPERTIES
        if (filter != null)
            txtFilter.setText(filter);

        // LAYOUT
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = marginHeight;
        layout.marginWidth = marginWidth;
        panel.setLayout(layout);
        txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // LISTENERS
        txtFilter.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.detail == SWT.CANCEL)
                    setFilter("");
                else
                    setFilter(txtFilter.getText());
            }
        });
        return panel;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        String old = this.filter;
        this.filter = filter;
        propSupport.firePropertyChange(PROP_FILTER, old, filter);
    }

    public void setFocus() {
        if (txtFilter != null && !txtFilter.isDisposed())
            txtFilter.setFocus();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(PROP_FILTER, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(PROP_FILTER, listener);
    }

}
