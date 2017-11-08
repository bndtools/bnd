package test.activator.inherits;

import org.osgi.framework.BundleActivator;
import org.osgi.service.component.annotations.Component;

import test.activator.Activator;

@Component(name = "inheritedactivator", service = BundleActivator.class)
public class InheritedActivator extends Activator {

}
