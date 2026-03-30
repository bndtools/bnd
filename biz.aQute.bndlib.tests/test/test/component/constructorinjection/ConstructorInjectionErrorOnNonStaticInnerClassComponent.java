package test.component.constructorinjection;

import org.osgi.service.component.annotations.Component;

@Component(service = ConstructorInjectionErrorOnNonStaticInnerClassComponent.class)
public class ConstructorInjectionErrorOnNonStaticInnerClassComponent {

	@Component(service = ConstructorInjectionErrorOnNonStaticInnerClassComponentInner.class)
	public class ConstructorInjectionErrorOnNonStaticInnerClassComponentInner {
		// this should fail because a component on a non-static inner class
		// cannot be instantiated by DS, because it has no way to get the outer
		// instance
	}
}