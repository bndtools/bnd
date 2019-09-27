package test.annotationheaders.spi.providerB;

import aQute.bnd.annotation.spi.ServiceProvider;
import test.annotationheaders.spi.SPIService;

@ServiceProvider(value = SPIService.class, attribute = {
	"foo=bar"
})
public class Provider implements SPIService {}
