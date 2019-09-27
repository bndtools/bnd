package test.annotationheaders.spi.consumerE;

import aQute.bnd.annotation.spi.ServiceConsumer;
import test.annotationheaders.spi.SPIService;

@Custom
public class ServiceConsumerMetaAnnotatingCustom {}

@ServiceConsumer(SPIService.class)
@interface Custom {}
