package aQute.bnd.plugin.jpms;

import static aQute.bnd.osgi.Constants.INTERNAL_EXPORT_TO_MODULES_DIRECTIVE;
import static aQute.bnd.osgi.Constants.INTERNAL_OPEN_TO_MODULES_DIRECTIVE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.lib.strings.Strings;

public class JPMSAnnotations implements AnalyzerPlugin {

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Set<PackageRef> processed = new HashSet<>();

		for (Clazz c : analyzer.getClassspace()
			.values()) {

			PackageRef packageRef = c.getClassName()
				.getPackageRef();

			if (processed.contains(packageRef)) {
				continue;
			}

			processed.add(packageRef);

			Clazz packageInfo = analyzer.getPackageInfo(packageRef);

			if (packageInfo == null) {
				continue;
			}

			// Add the -internal-open-to-modules directive
			// No modules means open to all
			packageInfo.annotations("aQute/bnd/annotation/jpms/Open")
				.forEach(annotation -> {
					String modules = "";

					if (annotation.get("value") != null) {
						modules = Arrays.stream(annotation.<Object[]> get("value"))
							.map(Object::toString)
							.collect(Collectors.joining(","));
					}

					analyzer.getContained()
						.get(packageRef)
						.put(INTERNAL_OPEN_TO_MODULES_DIRECTIVE, modules);
				});

			// Only add the -export-to-modules directive when modules are listed

			packageInfo.annotations("org/osgi/annotation/bundle/Export")
				.forEach(export -> {

					List<String> modules = packageInfo.annotations("aQute/bnd/annotation/jpms/ExportTo")
						.map(annotation -> annotation.get("value"))
						.map(Object[].class::cast)
						.flatMap(Arrays::stream)
						.map(Object::toString)
						.collect(Collectors.toList());

					if (!modules.isEmpty()) {
						analyzer.getContained()
							.get(packageRef)
							.put(INTERNAL_EXPORT_TO_MODULES_DIRECTIVE, Strings.join(modules));
					}
				});
		}

		return false;
	}

}
