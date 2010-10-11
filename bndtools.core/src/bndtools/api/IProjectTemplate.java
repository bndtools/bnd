package bndtools.api;

import bndtools.editor.model.BndEditModel;

public interface IProjectTemplate {
    void modifyInitialBndModel(BndEditModel model);
}
