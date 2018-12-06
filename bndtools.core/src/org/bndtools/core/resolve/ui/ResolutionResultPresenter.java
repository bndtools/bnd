package org.bndtools.core.resolve.ui;

public interface ResolutionResultPresenter {

    void updateButtons();

    void setMessage(String message, int level);

    void recalculate();

}
