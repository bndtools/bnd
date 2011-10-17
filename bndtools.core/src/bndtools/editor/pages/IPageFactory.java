package bndtools.editor.pages;

import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;

import bndtools.editor.model.BndEditModel;

public interface IPageFactory {
    IFormPage createPage(FormEditor editor, BndEditModel model, String id) throws IllegalArgumentException;
}
