package test.export.annotation.testNoimport;

import org.osgi.annotation.versioning.ProviderType;

import test.export.annotation.testNoimport.used.NoimportUsed;

@ProviderType
public interface NoimportInterface extends NoimportUsed {

}
