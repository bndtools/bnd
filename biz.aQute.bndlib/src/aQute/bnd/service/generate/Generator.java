package aQute.bnd.service.generate;

import java.util.Optional;

public interface Generator<O> {

	/**
	 * Generate files
	 *
	 * @param context the context
	 * @param options a spec interface
	 * @return set of modified files
	 */
	Optional<String> generate(BuildContext context, O options) throws Exception;
}
