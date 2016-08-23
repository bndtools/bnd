---
layout: default
class: Builder
title: -metatype CLASS-SPEC ( ',' CLASS-SPEC )*   
summary:  Create metatype XML resources in the bundle based on the OCD annotations.
---

		/**
		 * This class is responsible for meta type types. It is a plugin that can
		 * 
		 * @author aqute
		 */
		public class MetatypePlugin implements AnalyzerPlugin {
		
			public boolean analyzeJar(Analyzer analyzer) throws Exception {
		
				Parameters map = analyzer.parseHeader(analyzer.getProperty(Constants.METATYPE));
		
				Jar jar = analyzer.getJar();
				for (String name : map.keySet()) {
					Collection<Clazz> metatypes = analyzer.getClasses("", QUERY.ANNOTATED.toString(), Meta.OCD.class.getName(), //
							QUERY.NAMED.toString(), name //
							);
					for (Clazz c : metatypes) {
						jar.putResource("OSGI-INF/metatype/" + c.getFQN() + ".xml", new MetaTypeReader(c, analyzer));
					}
				}
				return false;
			}
			@Override
			public String toString() {
				return "MetatypePlugin";
			}
		
		}
