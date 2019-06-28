package test.annotationheaders.spi.consumer;

import aQute.bnd.annotation.spi.ServiceConsumer;
import test.annotationheaders.spi.SPIService;

@ServiceConsumer(SPIService.class)
public class SPIServiceFactory {}
