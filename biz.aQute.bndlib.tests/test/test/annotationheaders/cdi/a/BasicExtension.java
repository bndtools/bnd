package test.annotationheaders.cdi.a;

import javax.enterprise.inject.spi.Extension;

import aQute.bnd.annotation.cdi.ProvideCDIExtension;

@ProvideCDIExtension(name = "foo.extension", version = "1.0.0")
public class BasicExtension implements Extension {
}
