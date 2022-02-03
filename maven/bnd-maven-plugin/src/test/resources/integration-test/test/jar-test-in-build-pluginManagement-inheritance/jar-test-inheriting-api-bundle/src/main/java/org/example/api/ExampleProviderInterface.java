package org.example.api;

import org.example.types.ThingyDTO;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface ExampleProviderInterface {

	void doSomething(ThingyDTO thing);

}
