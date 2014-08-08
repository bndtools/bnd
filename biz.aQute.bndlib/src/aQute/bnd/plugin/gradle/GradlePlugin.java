package aQute.bnd.plugin.gradle;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.service.lifecycle.*;

/**
 * The Gradle life cycle plugin. ACtually nothing to do since gradle does not
 * require any specific files in a project
 */
@BndPlugin(name="gradle")
public class GradlePlugin extends LifeCyclePlugin {

	@Override
	public String toString() {
		return "GradlePlugin";
	}
}
