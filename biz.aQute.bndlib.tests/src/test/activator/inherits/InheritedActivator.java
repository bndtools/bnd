package test.activator.inherits;

import org.osgi.framework.*;

import test.activator.*;
import aQute.bnd.annotation.component.*;

@Component(name="inheritedactivator",provide=BundleActivator.class)
public class InheritedActivator extends Activator {

}
