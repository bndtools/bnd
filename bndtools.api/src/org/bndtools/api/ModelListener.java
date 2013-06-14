package org.bndtools.api;

import aQute.bnd.build.*;

public interface ModelListener {
    void modelChanged(Project model) throws Exception;
}
