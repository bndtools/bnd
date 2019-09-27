package test.annotationheaders.spi.consumerF;

import aQute.bnd.annotation.spi.ServiceConsumer;
import test.annotationheaders.spi.SPIService;

@ServiceConsumer(Integer.class)
@ServiceConsumer(SPIService.class)
public class ServiceConsumerMultiple {}
