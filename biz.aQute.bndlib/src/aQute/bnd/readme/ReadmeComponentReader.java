package aQute.bnd.readme;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

class ReadmeComponentReader {

	private DocumentBuilderFactory			dbf				= DocumentBuilderFactory.newInstance();
	private DocumentBuilder					db				= null;

	private Jar								_jar;
	private Domain							_domain;

	private List<ComponentDescriptionDTO>	_componentsDTO	= new LinkedList<>();
	private List<ObjectClassDefinitionDTO>	_metatypesDTO	= new LinkedList<>();

	public ReadmeComponentReader(Jar jar, Domain domain) {

		if (jar == null) {

			throw new NullPointerException("jar");
		}

		if (domain == null) {

			throw new NullPointerException("domain");
		}

		_jar = jar;
		_domain = domain;

		try {

			db = dbf.newDocumentBuilder();

			extractComponent();

			if (!_componentsDTO.isEmpty()) {

				extractMetatype();
			}

		} catch (ParserConfigurationException e) {

			// Nothing to do
		}
	}

	public List<ComponentDescriptionDTO> getComponentDescription() {

		return new LinkedList<>(_componentsDTO);
	}

	public List<ObjectClassDefinitionDTO> getObjectClassDefinitionDTO() {

		return new LinkedList<>(_metatypesDTO);
	}

	private void extractMetatype() {

		for (Resource metatype : _jar.getDirectories().get("OSGI-INF/metatype").values()) {

			try (InputStream in = metatype.openInputStream()) {

				ObjectClassDefinitionDTO dto = new ObjectClassDefinitionDTO();
				Document doc = db.parse(in);

				Element root = doc.getDocumentElement();

				dto.pid = new LinkedList<>();
				dto.factoryPid = new LinkedList<>();

				NodeList designates = root.getElementsByTagName("Designate");

				for (int i = 0; i < designates.getLength(); i++) {

					Element designate = (Element) designates.item(i);

					if (!designate.getAttribute("pid").isEmpty()) {

						dto.pid.add(designate.getAttribute("pid"));
					}

					if (!designate.getAttribute("factoryPid").isEmpty()) {

						dto.factoryPid.add(designate.getAttribute("factoryPid"));
					}
				}

				dto.attributes = new LinkedHashMap<>();

				NodeList ocds = root.getElementsByTagName("OCD");

				if (ocds.getLength() > 0) {

					NodeList ads = ((Element) ocds.item(0)).getElementsByTagName("AD");

					for (int i = 0; i < ads.getLength(); i++) {

						Element ad = (Element) ads.item(i);

						AttributeDefinitionDTO attDto = new AttributeDefinitionDTO();

						attDto.id = ad.getAttribute("id");
						attDto.description = ad.getAttribute("description");
						attDto.cardinality = ad.hasAttribute("cardinality")?Integer.valueOf(ad.getAttribute("cardinality")):0;
						attDto.type = ad.getAttribute("type");

						if (attDto.type.isEmpty()) {

							attDto.type = "String";
						}

						if (attDto.cardinality != 0) {

							if (ad.hasAttribute("default")) {

								String value = ad.getAttribute("default");
								List<String> values = new LinkedList<>(Arrays.asList(value.split("(?<!\\\\),")));
								List<String> valuesNotEmpty = new LinkedList<>();

								if (attDto.type.equalsIgnoreCase("String")) {

									for (String v : values) {

										String vv = v.replaceAll("\\\\(?<!\\\\\\\\)", "");
										
										if (!v.isEmpty()) {

											valuesNotEmpty.add(vv);
										}
									}

									values.clear();
									values.addAll(valuesNotEmpty);
									valuesNotEmpty.clear();
								}

								for (String v : values) {

									if (!v.isEmpty()) {

										valuesNotEmpty.add(v);
									}
								}

								if (!valuesNotEmpty.isEmpty()) {

									attDto.defaultValue = convert(attDto.type, valuesNotEmpty.toArray(new String[] {}));
								}
							}

							attDto.type = attDto.type + "[]";

						} else {

							if (ad.hasAttribute("default")) {

								String value = ad.getAttribute("default");

								if (attDto.type.equalsIgnoreCase("String")) {

									value = value.replaceAll("\\\\(?<!\\\\\\\\)", "");
								}

								attDto.defaultValue = convert(attDto.type, value);
							}
						}

						dto.attributes.put(attDto.id, attDto);
					}
				}

				_metatypesDTO.add(dto);

			} catch (Exception e) {

				// Nothing to do
			}
		}

	}

	private void extractComponent() {

		for (String path : _domain.getParameters("Service-Component").keySet()) {


			try (InputStream in = _jar.getResource(path).openInputStream()) {

				ComponentDescriptionDTO dto = new ComponentDescriptionDTO();
				Document doc = db.parse(in);

				Element root = doc.getDocumentElement();

				dto.name = root.getAttribute("name");
				
				if(dto.name.isEmpty()) {
					
					dto.name = ((Element) root.getElementsByTagName("implementation").item(0)).getAttribute("class");
				}
								
				dto.immediate = Boolean.parseBoolean(root.getAttribute("immediate"));
				
				if(root.getAttribute("enabled").isEmpty()) {
					
					dto.defaultEnabled = true;

				}else {
				
					dto.defaultEnabled=Boolean.parseBoolean(root.getAttribute("enabled"));
				}
				
				dto.factory=root.getAttribute("factory");		
				
				if (!root.hasAttribute("configuration-policy")) {
					
					dto.configurationPolicy = null;

				}else {
				
					dto.configurationPolicy=root.getAttribute("configuration-policy");
				}

				if (!root.hasAttribute("configuration-pid")) {
					
					dto.configurationPid = new String[] {dto.name};

				}else {
				
					dto.configurationPid=root.getAttribute("configuration-pid").split(" ");
				}
				
				NodeList services = root.getElementsByTagName("service");
				
				if(services.getLength() > 0) {
					
					Element service = (Element) services.item(0);
					
					if(service.getAttribute("scope").isEmpty()) {
						
						dto.scope = null;

					}else {
					
						dto.scope=service.getAttribute("scope");
					}
					
					NodeList provides = root.getElementsByTagName("provide");
					dto.serviceInterfaces = new String[provides.getLength()];
				
					for(int i = 0; i < provides.getLength() ; i++) {
						
						Element provide = (Element) provides.item(i);
						dto.serviceInterfaces[i]=provide.getAttribute("interface");
					}

				} else {

					dto.serviceInterfaces = new String[] {};
				}
				
				for(int i = 0; i < services.getLength() ; i++) {
					
					Element service = (Element) services.item(i);
				}
				
				dto.properties = new LinkedHashMap<>();
				
				NodeList properties = root.getElementsByTagName("property");

				for (int i = 0; i < properties.getLength(); i++) {

					Element property = (Element) properties.item(i);

					String name = property.getAttribute("name");
					String type = property.getAttribute("type");
					String value = property.getAttribute("value");
					Object valueTrue = null;

					if (type.isEmpty()) {

						type = "String";
					}

					if (value.isEmpty()) {

						valueTrue = convert(type, property.getTextContent().split("\n"));

					} else {

						valueTrue = convert(type, value);
					}

					dto.properties.put(name, valueTrue);
				}
				
				
				NodeList propertiesFile = root.getElementsByTagName("properties");

				for (int i = 0; i < propertiesFile.getLength(); i++) {

					Element property = (Element) propertiesFile.item(i);
					String pathProp = property.getAttribute("entry");

					try (InputStream inProp = _jar.getResource(pathProp).openInputStream()) {

						Properties prop = new Properties();

						prop.load(inProp);

						for (Entry<Object,Object> entry : prop.entrySet()) {

							dto.properties.put((String) entry.getKey(), entry.getValue());
						}
					}
				}

				 _componentsDTO.add(dto);

			} catch (Exception e) {

				// Nothing to do
			}
		}
	}

	private Object[] convert(String typeName, String[] values) {

		Object[] result = null;

		if (typeName.equalsIgnoreCase("String")) {

			result = new String[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = values[j];
			}

		} else if (typeName.equalsIgnoreCase("Double")) {

			result = new Double[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = Double.valueOf(values[j]);
			}

		} else if (typeName.equalsIgnoreCase("Float")) {

			result = new Float[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = Float.valueOf(values[j]);
			}

		} else if (typeName.equalsIgnoreCase("Integer")) {

			result = new Integer[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = Integer.valueOf(values[j]);
			}

		} else if (typeName.equalsIgnoreCase("Byte")) {

			result = new Byte[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = Byte.valueOf(values[j]);
			}

		} else if (typeName.equalsIgnoreCase("Character")) {

			result = new Character[values.length];

			for (int j = 0; j < values.length; j++) {

				int val = Integer.valueOf(values[j]);
				result[j] = (char) val;
			}

		} else if (typeName.equalsIgnoreCase("Boolean")) {

			result = new Boolean[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = Boolean.valueOf(values[j]);
			}

		} else if (typeName.equalsIgnoreCase("Long")) {

			result = new Long[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = Long.valueOf(values[j]);
			}

		} else if (typeName.equalsIgnoreCase("Short")) {

			result = new Short[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = Short.valueOf(values[j]);
			}
		} else {

			result = new String[values.length];

			for (int j = 0; j < values.length; j++) {

				result[j] = values[j];
			}
		}

		return result;
	}

	private Object convert(String typeName, String value) {

		Object result = null;

		if (typeName.equalsIgnoreCase("String")) {

			result = value;

		} else if (typeName.equalsIgnoreCase("Double")) {

			result = Double.valueOf(value);

		} else if (typeName.equalsIgnoreCase("Float")) {

			result = Float.valueOf(value);

		} else if (typeName.equalsIgnoreCase("Integer")) {

			result = Integer.valueOf(value);

		} else if (typeName.equalsIgnoreCase("Byte")) {

			result = Byte.valueOf(value);

		} else if (typeName.equalsIgnoreCase("Character")) {

			int val = Integer.valueOf(value);
			result = Character.valueOf((char) val);

		} else if (typeName.equalsIgnoreCase("Boolean")) {

			result = Boolean.valueOf(value);

		} else if (typeName.equalsIgnoreCase("Long")) {

			result = Long.valueOf(value);

		} else if (typeName.equalsIgnoreCase("Short")) {

			result = Short.valueOf(value);

		} else {

			result = value;
		}

		return result;
	}
}
