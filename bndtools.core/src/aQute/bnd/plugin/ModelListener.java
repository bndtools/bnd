package aQute.bnd.plugin;

import aQute.bnd.build.*;


public interface ModelListener {
    void modelChanged(Project model) throws Exception;
}
