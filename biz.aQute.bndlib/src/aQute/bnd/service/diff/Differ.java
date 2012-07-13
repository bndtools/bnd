package aQute.bnd.service.diff;

import aQute.bnd.osgi.*;

/**
 * Compare two Jars and report the differences.
 */
public interface Differ {
	Tree tree(Analyzer source) throws Exception;

	Tree tree(Jar source) throws Exception;

	Tree deserialize(Tree.Data data) throws Exception;
}
