package bndtools.editor.project;

import org.eclipse.jface.viewers.LabelProvider;

import bndtools.api.EE;

public class EELabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
        EE ee = (EE) element;

        return ee.getEEName();
    }
}
