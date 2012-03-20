package bndtools.model.importanalysis;

import aQute.libg.header.Attrs;
import bndtools.model.clauses.HeaderClause;

public class RequiredBundle extends HeaderClause {
    private boolean satisfied;

    public RequiredBundle(String name, Attrs attribs, boolean satisfied) {
        super(name, attribs);
        this.satisfied = satisfied;
    }
    public boolean isSatisfied() {
        return satisfied;
    }
}