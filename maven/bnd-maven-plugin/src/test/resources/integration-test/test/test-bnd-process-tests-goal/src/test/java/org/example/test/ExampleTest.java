package org.example.test;

import org.example.api.ExampleProviderInterface;
import org.example.types.ThingyDTO;
import org.junit.Test;

public class ExampleTest {

	@Test
	public void test() {
		// force import
		ExampleProviderInterface epi = new ExampleProviderInterface() {
			public void doSomething(ThingyDTO thing) {
			}
		};
	}

}
