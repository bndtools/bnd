package aQute.bnd.osgi.metainf;

import static aQute.bnd.osgi.Constants.METAINF_SERVICES;
import static aQute.bnd.osgi.Constants.METAINF_SERVICES_STRATEGY_ANNOTATION;
import static aQute.bnd.osgi.Constants.METAINF_SERVICES_STRATEGY_AUTO;
import static aQute.bnd.osgi.Constants.METAINF_SERVICES_STRATEGY_NONE;

import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Annotation.ElementType;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.metainf.MetaInfService.Implementation;
import aQute.bnd.service.AnalyzerPlugin;

/**
 * process the META-INF/services/* files. These files can contain bnd
 * annotations.
 */

public class MetaInfServiceParser implements AnalyzerPlugin {

	/**
	 * Iterate over the the file in META-INF/services and process them for
	 * annotations.
	 */
	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {

		String strategy = strategy(analyzer);
		switch (strategy) {
			case METAINF_SERVICES_STRATEGY_NONE :
				return false;
			case METAINF_SERVICES_STRATEGY_AUTO : {
				analyzer.addClasspathDefault(ServiceProvider.class);
				break;
			}
			case METAINF_SERVICES_STRATEGY_ANNOTATION : {
				analyzer.addClasspathDefault(ServiceProvider.class);
				break;
			}
		}

		MetaInfService.getServiceFiles(analyzer.getJar())
			.values()
			.stream()
			.flatMap(mis -> mis.getImplementations()
				.values()
				.stream())
			.forEach(impl -> {
				Parameters annotations = impl.getAnnotations();

				if (annotations.isEmpty() && METAINF_SERVICES_STRATEGY_AUTO.equals(strategy)) {
					Attrs attrs = new Attrs();
					attrs.put(Constants.RESOLUTION_DIRECTIVE, Resolution.OPTIONAL);
					attrs.addDirectiveAliases();
					annotations.add(ServiceProvider.class.getName(), attrs);
				}

				annotations.forEach((annotationName, attrs) -> {
					doAnnotationsforMetaInf(analyzer, impl, Processor.removeDuplicateMarker(annotationName), attrs);
				});
			});
		return false;
	}

	/*
	 * Process 1 annotation
	 */
	private void doAnnotationsforMetaInf(Analyzer analyzer, Implementation impl, String annotationName, Attrs attrs) {
		try {
			Map<String, Object> properties = attrs.toTyped();
			properties.putIfAbsent("value", impl.getServiceName()); // default
			TypeRef implementation = analyzer.getTypeRefFromFQN(impl.getImplementationName());
			assert implementation != null;
			Annotation ann = new Annotation(analyzer.getTypeRefFromFQN(annotationName), properties, ElementType.TYPE,
				RetentionPolicy.CLASS);
			analyzer.addAnnotation(ann, implementation);
		} catch (Exception e) {
			analyzer.exception(e, "failed to process %s=%v due to %s", annotationName, attrs, e);
		}
	}

	private String strategy(Analyzer analyzer) {
		return analyzer.getProperty(METAINF_SERVICES, METAINF_SERVICES_STRATEGY_AUTO);
	}
}
