package test.referenced.annotation.testReferenced;

import org.osgi.annotation.bundle.Referenced;

import test.export.annotation.testConsumer.ConsumerInterface;
import test.export.annotation.testProvider.ProviderInterface;

@Referenced({
	ProviderInterface.class, ConsumerInterface.class
})
public class TestReferenced {

	public TestReferenced() {
	}

}
