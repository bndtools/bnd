package bndtools.editor.pages;

import org.eclipse.ui.forms.editor.IFormPage;

import bndtools.editor.common.AbstractBaseFormEditor;
import bndtools.editor.model.BndEditModel;

public interface IPageFactory {
    IFormPage createPage(AbstractBaseFormEditor editor, BndEditModel model, String id) throws IllegalArgumentException;
}
