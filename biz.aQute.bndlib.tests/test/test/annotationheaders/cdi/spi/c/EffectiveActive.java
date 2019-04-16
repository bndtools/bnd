package test.annotationheaders.cdi.spi.c;

import javax.enterprise.inject.spi.Extension;

import aQute.bnd.annotation.cdi.ProvideCDIExtension;

@ProvideCDIExtension.SPI(name = "foo.extension", version = "1.0.0", effective = "active")
public class EffectiveActive implements Extension {
}
