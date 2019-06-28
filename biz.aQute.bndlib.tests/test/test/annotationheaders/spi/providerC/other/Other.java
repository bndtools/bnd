package test.annotationheaders.spi.providerC.other;

import aQute.bnd.annotation.spi.ServiceProvider;
import test.annotationheaders.spi.SPIService;

@ServiceProvider(value = SPIService.class, attribute = {
	"foo=bar"
})
public class Other implements SPIService {}
