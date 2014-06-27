package aQute.bnd.indexer;

import aQute.bnd.annotation.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.resource.*;

/**
 * <p>
 * A resource analyzer is responsible for analyzing a resource for some specific
 * purpose, and discovering requirements and capabilities that may not be
 * discovered by the basic Bundle Analyzer.
 * </p>
 * 
 * <p>
 * Clients may implement this interface and register instances as services.
 * </p>
 * 
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
@ConsumerType
public interface ResourceAnalyzer {
	/**
	 * <p>
	 * This method is invoked for each resource that the analyzer is requested
	 * to analyze. Implementations add zero or more capabilities and/or
	 * requirements to the supplied lists.
	 * </p>
	 * 
	 * <p>
	 * Analyzers <b>may</b> examine the lists of already-discovered requirements
	 * and capabilities; for example they may wish to add a certain capability
	 * if (and only if) it has not already been added.
	 * </p>
	 * 
	 * <p>
	 * However, analyzers <b>should not</b> rely on being invoked in any
	 * particular order, i.e. either before or after any other analyzer.
	 * </p>
	 * 
	 * <p>
	 * Analyzers <b>MUST NOT</b> attempt to remove or replace any capability or
	 * requirement from the supplied list. Clients of this method may enforce
	 * this by passing List implementations that throw
	 * {@link UnsupportedOperationException} upon any attempt to call
	 * {@link List#remove(int)}, etc.
	 * </p>
	 * 
	 * 
	 * @param resource
	 *            The current resource.
	 * @param capabilities
	 *            The list of capabilities.
	 * @param requirements
	 *            The list of requirements.
	 * @throws Exception
	 *             If something goes wrong. The error will be logged to the OSGi
	 *             Log Service (if available) and the next ResourceAnalyzer (if
	 *             any) will be asked to analyze the resource.
	 */
	void analyzeResource(Jar jar, ResourceBuilder rb) throws Exception;
}
