package aQute.bnd.build.model;

import aQute.bnd.build.model.clauses.HeaderClause;

/**
 * This is a special {@link HeaderClause} which knows from which property-key it
 * came from. Also it has knowledge about if it <i>isLocal</i>. local means that
 * it is in the current file you are looking it (vs. inherited via
 * mergedProperties)
 */
public class BndEditModelHeaderClause extends HeaderClause {

	private String	propertyKey;
	private boolean isLocal;

	public BndEditModelHeaderClause(String propertyKey, HeaderClause headerClause, boolean isLocal) {
		super(headerClause.getName(), headerClause.getAttribs());
		this.propertyKey = propertyKey;
		this.isLocal = isLocal;
	}

	public String key() {
		return propertyKey;
	}

	public boolean isLocal() {
		return isLocal;
	}


	public String displayTitle() {
		return attribs.get("name", getName());
	}
}
