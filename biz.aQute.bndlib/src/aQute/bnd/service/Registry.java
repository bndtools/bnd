package aQute.bnd.service;

import java.util.*;

/**
 * A registry for objects.
 */
public interface Registry {
	<T> List<T> getPlugins(Class<T> c);

	<T> T getPlugin(Class<T> c);
}
