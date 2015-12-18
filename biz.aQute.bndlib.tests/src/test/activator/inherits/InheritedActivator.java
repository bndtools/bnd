package test.activator.inherits;

import org.osgi.framework.BundleActivator;

import aQute.bnd.annotation.component.Component;
import test.activator.Activator;

@Component(name = "inheritedactivator", provide = BundleActivator.class)
public class InheritedActivator extends Activator {

}
