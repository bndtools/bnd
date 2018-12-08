package org.bndtools.templating.jgit.ui;

import org.eclipse.swt.widgets.Shell;

public interface NewEntryDialogFactory {
    AbstractNewEntryDialog create(Shell parentShell);
}
