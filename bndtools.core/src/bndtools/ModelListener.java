package bndtools;

import aQute.bnd.build.*;

public interface ModelListener {
    void modelChanged(Project model) throws Exception;
}
