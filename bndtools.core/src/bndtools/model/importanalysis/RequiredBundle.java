package bndtools.model.importanalysis;

import java.util.Map;

import bndtools.model.clauses.HeaderClause;

public class RequiredBundle extends HeaderClause {
    private boolean satisfied;

    public RequiredBundle(String name, Map<String, String> attribs, boolean satisfied) {
        super(name, attribs);
        this.satisfied = satisfied;
    }
    public boolean isSatisfied() {
        return satisfied;
    }
}