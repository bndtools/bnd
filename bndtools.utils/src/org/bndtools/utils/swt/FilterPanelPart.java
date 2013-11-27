package org.bndtools.utils.swt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

public class FilterPanelPart {

    private static final String PROP_FILTER = "filter";
    private static final long SEARCH_DELAY = 1000;

    private final ScheduledExecutorService scheduler;

    private String filter;

    private Composite panel;
    private Text txtFilter;

    private final PropertyChangeSupport propSupport = new PropertyChangeSupport(this);
    private final Lock scheduledFilterLock = new ReentrantLock();
    private final Runnable updateFilterTask = new Runnable() {
        public void run() {
            Display display = panel.getDisplay();
            Runnable update = new Runnable() {
                public void run() {
                    String newFilter = txtFilter.getText();
                    setFilter(newFilter);
                }
            };
            if (display.getThread() == Thread.currentThread())
                update.run();
            else
                display.asyncExec(update);
        }
    };
    private ScheduledFuture< ? > scheduledFilterUpdate = null;

    public FilterPanelPart(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public Control createControl(Composite parent) {
        return createControl(parent, 0, 0);
    }

    public Control createControl(Composite parent, int marginWidth, int marginHeight) {
        // CREATE CONTROLS
        panel = new Composite(parent, SWT.NONE);
        panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        txtFilter = new Text(panel, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        txtFilter.setMessage("Enter search string");

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
            public void widgetDefaultSelected(SelectionEvent ev) {
                try {
                    scheduledFilterLock.lock();
                    if (scheduledFilterUpdate != null)
                        scheduledFilterUpdate.cancel(true);
                } finally {
                    scheduledFilterLock.unlock();
                }

                String newFilter = (ev.detail == SWT.CANCEL) ? "" : txtFilter.getText();
                setFilter(newFilter);
            }
        });
        txtFilter.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent ev) {
                try {
                    scheduledFilterLock.lock();
                    if (scheduledFilterUpdate != null)
                        scheduledFilterUpdate.cancel(true);
                    scheduledFilterUpdate = scheduler.schedule(updateFilterTask, SEARCH_DELAY, TimeUnit.MILLISECONDS);
                } finally {
                    scheduledFilterLock.unlock();
                }
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
