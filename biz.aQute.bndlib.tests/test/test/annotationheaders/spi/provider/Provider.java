package test.annotationheaders.spi.provider;

import aQute.bnd.annotation.spi.ServiceProvider;
import test.annotationheaders.spi.SPIService;

@ServiceProvider(SPIService.class)
public class Provider implements SPIService {}
