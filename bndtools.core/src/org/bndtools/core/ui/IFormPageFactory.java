package org.bndtools.core.ui;

import org.eclipse.ui.forms.editor.IFormPage;

import bndtools.api.IBndModel;

public interface IFormPageFactory {
    
    public static enum Mode { build, run, bundle, workspace };
    
    IFormPage createPage(ExtendedFormEditor editor, IBndModel model, String id) throws IllegalArgumentException;
    
    boolean supportsMode(Mode mode);
}
