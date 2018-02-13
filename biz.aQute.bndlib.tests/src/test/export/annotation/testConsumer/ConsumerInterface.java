package test.export.annotation.testConsumer;

import org.osgi.annotation.versioning.ProviderType;

import test.export.annotation.testConsumer.used.ConsumerUsed;

@ProviderType
public interface ConsumerInterface extends ConsumerUsed {

}
