package test.annotationheaders.cdi.spiweak.a;

import javax.enterprise.inject.spi.Extension;

import aQute.bnd.annotation.cdi.ProvideCDIExtension;

@ProvideCDIExtension.SPIWeak(name = "foo.extension", version = "1.0.0")
public class BasicExtension implements Extension {
}
