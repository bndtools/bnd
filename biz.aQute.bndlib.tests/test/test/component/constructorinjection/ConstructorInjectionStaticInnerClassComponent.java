package test.component.constructorinjection;

import org.osgi.service.component.annotations.Component;

@Component(service = ConstructorInjectionStaticInnerClassComponent.class)
public class ConstructorInjectionStaticInnerClassComponent {

	@Component(service = ConstructorInjectionStaticInnerClassComponentInner.class)
	public static class ConstructorInjectionStaticInnerClassComponentInner {
		// this static inner class component should work because it can be
		// instantiated by DS
	}
}