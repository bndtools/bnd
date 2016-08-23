---
layout: default
class: Header
title: Service-Component ::= RESOURCE ( ',' RESOURCE ) 
summary: XML documents containing component descriptions must be specified by the Service-Component header in the manifest.  
---

	/**
	 * Analyze the class space for any classes that have an OSGi annotation for DS.
	 */
	public class DSAnnotations implements AnalyzerPlugin {
	
		public boolean analyzeJar(Analyzer analyzer) throws Exception {
			Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS));
			if (header.size() == 0)
				return false;
	
			Instructions instructions = new Instructions(header);
			Collection<Clazz> list = analyzer.getClassspace().values();
			String sc = analyzer.getProperty(Constants.SERVICE_COMPONENT);
			List<String> names = new ArrayList<String>();
			if (sc != null && sc.trim().length() > 0)
				names.add(sc);
	
			for (Clazz c: list) {
				for (Instruction instruction : instructions.keySet()) {
	
					if (instruction.matches(c.getFQN())) {
						if (instruction.isNegated())
							break;
						ComponentDef definition = AnnotationReader.getDefinition(c, analyzer);
						if (definition != null) {
							definition.sortReferences();
							definition.prepare(analyzer);
							String name = "OSGI-INF/" + definition.name + ".xml";
							names.add(name);
							analyzer.getJar().putResource(name, new TagResource(definition.getTag()));
						}
					}
				}
			}
			sc = Processor.append(names.toArray(new String[names.size()]));
			analyzer.setProperty(Constants.SERVICE_COMPONENT, sc);
			return false;
		}
	
		@Override
		public String toString() {
			return "DSAnnotations";
		}
	}

				package aQute.bnd.make.component;
			
			import java.io.*;
			import java.util.*;
			import java.util.Map.Entry;
			
			import aQute.bnd.annotation.component.*;
			import aQute.bnd.component.*;
			import aQute.bnd.header.*;
			import aQute.bnd.make.metatype.*;
			import aQute.bnd.osgi.*;
			import aQute.bnd.osgi.Clazz.QUERY;
			import aQute.bnd.osgi.Descriptors.TypeRef;
			import aQute.bnd.service.*;
			import aQute.lib.tag.*;
			
			/**
			 * This class is an analyzer plugin. It looks at the properties and tries to
			 * find out if the Service-Component header contains the bnd shortut syntax. If
			 * not, the header is copied to the output, if it does, an XML file is created
			 * and added to the JAR and the header is modified appropriately.
			 */
			public class ServiceComponent implements AnalyzerPlugin {
			
				public boolean analyzeJar(Analyzer analyzer) throws Exception {
			
					ComponentMaker m = new ComponentMaker(analyzer);
			
					Map<String,Map<String,String>> l = m.doServiceComponent();
			
					analyzer.setProperty(Constants.SERVICE_COMPONENT, Processor.printClauses(l));
			
					analyzer.getInfo(m, Constants.SERVICE_COMPONENT + ": ");
					m.close();
			
					return false;
				}
			
				private static class ComponentMaker extends Processor {
					Analyzer	analyzer;
			
					ComponentMaker(Analyzer analyzer) {
						super(analyzer);
						this.analyzer = analyzer;
					}
			
					/**
					 * Iterate over the Service Component entries. There are two cases:
					 * <ol>
					 * <li>An XML file reference</li>
					 * <li>A FQN/wildcard with a set of attributes</li>
					 * </ol>
					 * An XML reference is immediately expanded, an FQN/wildcard is more
					 * complicated and is delegated to
					 * {@link #componentEntry(Map, String, Map)}.
					 * 
					 * @throws Exception
					 */
					Map<String,Map<String,String>> doServiceComponent() throws Exception {
						Map<String,Map<String,String>> serviceComponents = newMap();
						String header = getProperty(SERVICE_COMPONENT);
						Parameters sc = parseHeader(header);
			
						for (Entry<String,Attrs> entry : sc.entrySet()) {
							String name = entry.getKey();
							Map<String,String> info = entry.getValue();
			
							try {
								if (name.indexOf('/') >= 0 || name.endsWith(".xml")) {
									// Normal service component, we do not process it
									serviceComponents.put(name, EMPTY);
								} else {
									componentEntry(serviceComponents, name, info);
								}
							}
							catch (Exception e) {
								e.printStackTrace();
								error("Invalid " + Constants.SERVICE_COMPONENT + " header: %s %s, throws %s", name, info, e);
								throw e;
							}
						}
						return serviceComponents;
					}
			
					/**
					 * Parse an entry in the Service-Component header. This header supports
					 * the following types:
					 * <ol>
					 * <li>An FQN + attributes describing a component</li>
					 * <li>A wildcard expression for finding annotated components.</li>
					 * </ol>
					 * The problem is the distinction between an FQN and a wildcard because
					 * an FQN can also be used as a wildcard. If the info specifies
					 * {@link Constants#NOANNOTATIONS} then wildcards are an error and the
					 * component must be fully described by the info. Otherwise the
					 * FQN/wildcard is expanded into a list of classes with annotations. If
					 * this list is empty, the FQN case is interpreted as a complete
					 * component definition. For the wildcard case, it is checked if any
					 * matching classes for the wildcard have been compiled for a class file
					 * format that does not support annotations, this can be a problem with
					 * JSR14 who silently ignores annotations. An error is reported in such
					 * a case.
					 * 
					 * @param serviceComponents
					 * @param name
					 * @param info
					 * @throws Exception
					 * @throws IOException
					 */
					private void componentEntry(Map<String,Map<String,String>> serviceComponents, String name,
							Map<String,String> info) throws Exception, IOException {
			
						boolean annotations = !Processor.isTrue(info.get(NOANNOTATIONS));
						boolean fqn = Verifier.isFQN(name);
			
						if (annotations) {
			
							// Annotations possible!
			
							Collection<Clazz> annotatedComponents = analyzer.getClasses("", QUERY.ANNOTATED.toString(),
									Component.class.getName(), //
									QUERY.NAMED.toString(), name //
									);
			
							if (fqn) {
								if (annotatedComponents.isEmpty()) {
			
									// No annotations, fully specified in header
			
									createComponentResource(serviceComponents, name, info);
								} else {
			
									// We had a FQN so expect only one
			
									for (Clazz c : annotatedComponents) {
										annotated(serviceComponents, c, info);
									}
								}
							} else {
			
								// We did not have an FQN, so expect the use of wildcards
			
								if (annotatedComponents.isEmpty())
									checkAnnotationsFeasible(name);
								else
									for (Clazz c : annotatedComponents) {
										annotated(serviceComponents, c, info);
									}
							}
						} else {
							// No annotations
							if (fqn)
								createComponentResource(serviceComponents, name, info);
							else
								error("Set to %s but entry %s is not an FQN ", NOANNOTATIONS, name);
			
						}
					}
			
					/**
					 * Check if annotations are actually feasible looking at the class
					 * format. If the class format does not provide annotations then it is
					 * no use specifying annotated components.
					 * 
					 * @param name
					 * @return
					 * @throws Exception
					 */
					private Collection<Clazz> checkAnnotationsFeasible(String name) throws Exception {
						Collection<Clazz> not = analyzer.getClasses("", QUERY.NAMED.toString(), name //
								);
			
						if (not.isEmpty()) {
							if ("*".equals(name))
								return not;
							error("Specified %s but could not find any class matching this pattern", name);
						}
			
						for (Clazz c : not) {
							if (c.getFormat().hasAnnotations())
								return not;
						}
			
						warning("Wildcards are used (%s) requiring annotations to decide what is a component. Wildcard maps to classes that are compiled with java.target < 1.5. Annotations were introduced in Java 1.5",
								name);
			
						return not;
					}
			
					void annotated(Map<String,Map<String,String>> components, Clazz c, Map<String,String> info) throws Exception {
						// Get the component definition
						// from the annotations
						Map<String,String> map = ComponentAnnotationReader.getDefinition(c, this);
			
						// Pick the name, the annotation can override
						// the name.
						String localname = map.get(COMPONENT_NAME);
						if (localname == null)
							localname = c.getFQN();
			
						// Override the component info without manifest
						// entries. We merge the properties though.
			
						String merged = Processor.merge(info.remove(COMPONENT_PROPERTIES), map.remove(COMPONENT_PROPERTIES));
						if (merged != null && merged.length() > 0)
							map.put(COMPONENT_PROPERTIES, merged);
						map.putAll(info);
						createComponentResource(components, localname, map);
					}
			
					private void createComponentResource(Map<String,Map<String,String>> components, String name,
							Map<String,String> info) throws Exception {
			
						// We can override the name in the parameters
						if (info.containsKey(COMPONENT_NAME))
							name = info.get(COMPONENT_NAME);
			
						// Assume the impl==name, but allow override
						String impl = name;
						if (info.containsKey(COMPONENT_IMPLEMENTATION))
							impl = info.get(COMPONENT_IMPLEMENTATION);
			
						TypeRef implRef = analyzer.getTypeRefFromFQN(impl);
						// Check if such a class exists
						analyzer.referTo(implRef);
			
						boolean designate = designate(name, info.get(COMPONENT_DESIGNATE), false)
								|| designate(name, info.get(COMPONENT_DESIGNATEFACTORY), true);
			
						// If we had a designate, we want a default configuration policy of
						// require.
						if (designate && info.get(COMPONENT_CONFIGURATION_POLICY) == null)
							info.put(COMPONENT_CONFIGURATION_POLICY, "require");
			
						// We have a definition, so make an XML resources
						Resource resource = createComponentResource(name, impl, info);
						analyzer.getJar().putResource("OSGI-INF/" + name + ".xml", resource);
			
						components.put("OSGI-INF/" + name + ".xml", EMPTY);
			
					}
			
					/**
					 * Create a Metatype and Designate record out of the given
					 * configurations.
					 * 
					 * @param name
					 * @param config
					 * @throws Exception 
					 */
					private boolean designate(String name, String config, boolean factory) throws Exception {
						if (config == null)
							return false;
			
						for (String c : Processor.split(config)) {
							TypeRef ref = analyzer.getTypeRefFromFQN(c);
							Clazz clazz = analyzer.findClass(ref);
							if (clazz != null) {
								analyzer.referTo(ref);
								MetaTypeReader r = new MetaTypeReader(clazz, analyzer);
								r.setDesignate(name, factory);
								String rname = "OSGI-INF/metatype/" + name + ".xml";
			
								analyzer.getJar().putResource(rname, r);
							} else {
								analyzer.error("Cannot find designated configuration class %s for component %s", c, name);
							}
						}
						return true;
					}
			
					/**
					 * Create the resource for a DS component.
					 * 
					 * @param list
					 * @param name
					 * @param info
					 * @throws UnsupportedEncodingException
					 */
					Resource createComponentResource(String name, String impl, Map<String, String> info)
							throws Exception {
						HeaderReader hr = new HeaderReader(analyzer);
						Tag tag = hr.createComponentTag(name, impl, info);
						hr.close();
						return new TagResource(tag);
					}
				}
			
			}
				
				
				
				
				
				
					private void verifyComponent() {
		String serviceComponent = main.get(Constants.SERVICE_COMPONENT);
		if (serviceComponent != null) {
			Parameters map = parseHeader(serviceComponent);
			for (String component : map.keySet()) {
				if (component.indexOf("*") < 0 && !dot.exists(component)) {
					error(Constants.SERVICE_COMPONENT + " entry can not be located in JAR: " + component);
				} else {
					// validate component ...
				}
			}
		}
	}

				