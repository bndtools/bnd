package org.example.impl;

import org.example.api.ExampleProviderInterface;
import org.example.types.ThingyDTO;
import org.osgi.service.component.annotations.Component;

@Component
public class ExampleComponent implements ExampleProviderInterface {

	public void doSomething(ThingyDTO thing) {
	}

}
