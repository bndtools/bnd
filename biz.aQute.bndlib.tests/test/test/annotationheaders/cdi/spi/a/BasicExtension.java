package test.annotationheaders.cdi.spi.a;

import javax.enterprise.inject.spi.Extension;

import aQute.bnd.annotation.cdi.ProvideCDIExtension;

@ProvideCDIExtension.SPI(name = "foo.extension", version = "1.0.0")
public class BasicExtension implements Extension {
}
