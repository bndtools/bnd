package aQute.bnd.service.parameters;

import aQute.bnd.header.Parameters;

/**
 * This interface is a Processor plugin that is consolidating a Parameters. In
 * OSGi most headers follow Parameters syntax. However, different parts of the
 * system might contribute one or more clauses to the same header. The addClause
 * method in Processor can be used to add a single clause. This interface is
 * used to consolidate the aggregate. Some headers need the removal of
 * duplicates and and other headers require sorting to ensure they are
 * consistent between builds. The default ordering of the Parameters is in order
 * of insert. Note that the Parameters can handle duplicate keys by suffixing
 * them with a `~`.
 */
public interface ConsolidateParameters {
	/**
	 * Consolidate a Parameters. If the key is not recognized, return null.
	 *
	 * @param key the key/header, for example Require-Capability
	 * @param parameters the parameters to consolidate. Do not modify it.
	 * @return null if unknown key, otherwise a consolidated Parameters
	 */
	Parameters consolidate(String key, Parameters parameters);

}
