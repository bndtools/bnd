package org.bndtools.templating.jgit.ui;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Shell;

import aQute.bnd.header.Attrs;
import aQute.libg.tuple.Pair;

public abstract class AbstractNewEntryDialog extends TitleAreaDialog {

    public AbstractNewEntryDialog(Shell parentShell) {
        super(parentShell);
    }

    public abstract void setEntry(Pair<String, Attrs> entry);

    public abstract Pair<String, Attrs> getEntry();

}
