package aQute.bnd.metadata;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.service.metatype.annotations.AttributeType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.metadata.dto.AttributeDefinitionDTO;
import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.metadata.dto.IconDTO;
import aQute.bnd.metadata.dto.LocalizableAttributeDefinitionDTO;
import aQute.bnd.metadata.dto.LocalizableObjectClassDefinitionDTO;
import aQute.bnd.metadata.dto.LocalizableOptionDTO;
import aQute.bnd.metadata.dto.ObjectClassDefinitionDTO;
import aQute.bnd.metadata.dto.OptionDTO;
import aQute.bnd.metadata.dto.TypedPropertyDTO;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

class MetatypeExtractor extends MetadataExtractor {

	final private static String	METATYPE_DIR				= "OSGI-INF/metatype";
	final private static String	DEFAULT_LOCALIZATION_BASE	= "OSGI-INF/l10n/";

	final private static String	DESIGNATE_TAG				= "Designate";
	final private static String	OCD_TAG						= "OCD";
	final private static String	AD_TAG						= "AD";
	final private static String	ICON_TAG					= "Icon";
	final private static String	OPTION_TAG					= "Option";

	final private static String	LOCALIZATION_ATTR			= "localization";
	final private static String	ID_ATTR						= "id";
	final private static String	NAME_ATTR					= "name";
	final private static String	DESCRIPTION_ATTR			= "description";
	final private static String	RESOURCE_ATTR				= "resource";
	final private static String	SIZE_ATTR					= "size";
	final private static String	PID_ATTR					= "pid";
	final private static String	FACTORY_PID_ATTR			= "factoryPid";
	final private static String	CARDINALITY_ATTR			= "cardinality";
	final private static String	TYPE_ATTR					= "type";
	final private static String	DEFAULT_ATTR				= "default";
	final private static String	REQUIRED_ATTR				= "required";
	final private static String	MIN_ATTR					= "min";
	final private static String	MAX_ATTR					= "max";
	final private static String	LABEL_ATTR					= "label";
	final private static String	VALUE_ATTR					= "value";

	@Override
	public void extract(BundleMetadataDTO dto, Jar jar) {

		DocumentBuilder db = null;

		try {

			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		} catch (ParserConfigurationException e) {

			throw new RuntimeException(e);

		}

		dto.metatypes = new LinkedList<>();

		for (Resource metatype : jar.getDirectories().get(METATYPE_DIR).values()) {

			try (InputStream in = metatype.openInputStream()) {

				Document doc = db.parse(in);

				Element root = doc.getDocumentElement();

				String localizationPath = null;
				ObjectClassDefinitionDTO odto = new ObjectClassDefinitionDTO();

				NodeList ocds = root.getElementsByTagName(OCD_TAG);

				if (ocds.getLength() > 0) {

					Element ocd = ((Element) ocds.item(0));

					if (ocd.hasAttribute(ID_ATTR)) {

						odto.id = ocd.getAttribute(ID_ATTR);
					}

					Map<String,Map<String,String>> localizations = new HashMap<>();

					if (root.hasAttribute(LOCALIZATION_ATTR)) {

						localizationPath = root.getAttribute(LOCALIZATION_ATTR);

					} else {

						if (odto.id != null) {

							localizationPath = DEFAULT_LOCALIZATION_BASE + odto.id;
						}
					}

					if (localizationPath != null) {

						localizations = getLocalization(localizationPath, jar);
					}

					odto.localizations = new HashMap<>();

					if (ocd.hasAttribute(NAME_ATTR)) {

						odto.name = getUnlocalizedAttr(ocd.getAttribute(NAME_ATTR), localizations);

						for (Entry<String,String> entry : getLocalizedAttr(ocd.getAttribute(NAME_ATTR), localizations)
								.entrySet()) {

							if (!odto.localizations.containsKey(entry.getKey())) {

								odto.localizations.put(entry.getKey(), new LocalizableObjectClassDefinitionDTO());
							}

							odto.localizations.get(entry.getKey()).name = entry.getValue();
						}
					}

					if (ocd.hasAttribute(DESCRIPTION_ATTR)) {

						odto.description = getUnlocalizedAttr(ocd.getAttribute(DESCRIPTION_ATTR), localizations);

						for (Entry<String,String> entry : getLocalizedAttr(ocd.getAttribute(DESCRIPTION_ATTR),
								localizations).entrySet()) {

							if (!odto.localizations.containsKey(entry.getKey())) {

								odto.localizations.put(entry.getKey(), new LocalizableObjectClassDefinitionDTO());
							}

							odto.localizations.get(entry.getKey()).description = entry.getValue();
						}
					}

					odto.pids = new LinkedList<>();
					odto.factoryPids = new LinkedList<>();

					NodeList designates = root.getElementsByTagName(DESIGNATE_TAG);

					for (int i = 0; i < designates.getLength(); i++) {

						Element designate = (Element) designates.item(i);

						if (designate.hasAttribute(PID_ATTR)) {

							odto.pids.add(designate.getAttribute(PID_ATTR));
						}

						if (designate.hasAttribute(FACTORY_PID_ATTR)) {

							odto.factoryPids.add(designate.getAttribute(FACTORY_PID_ATTR));
						}
					}

					odto.icons = new LinkedList<>();

					NodeList icons = ocd.getElementsByTagName(ICON_TAG);

					for (int i = 0; i < icons.getLength(); i++) {

						Element icon = (Element) icons.item(i);

						IconDTO idto = new IconDTO();

						if (icon.hasAttribute(RESOURCE_ATTR)) {

							idto.url = icon.getAttribute(RESOURCE_ATTR);
						}

						if (icon.hasAttribute(SIZE_ATTR)) {

							try {

								idto.size = Integer.valueOf(icon.getAttribute(SIZE_ATTR));

							} catch (Exception expected) {

								// Nothing to do
							}
						}
						odto.icons.add(idto);
					}

					odto.attributes = new LinkedList<>();

					NodeList ads = ocd.getElementsByTagName(AD_TAG);

					for (int i = 0; i < ads.getLength(); i++) {

						Element ad = (Element) ads.item(i);

						AttributeDefinitionDTO adto = new AttributeDefinitionDTO();

						adto.property = new TypedPropertyDTO();
						adto.localizations = new HashMap<>();

						if (ad.hasAttribute(NAME_ATTR)) {

							adto.name = getUnlocalizedAttr(ad.getAttribute(NAME_ATTR), localizations);

							for (Entry<String,String> entry : getLocalizedAttr(ad.getAttribute(NAME_ATTR),
									localizations).entrySet()) {

								if (!adto.localizations.containsKey(entry.getKey())) {

									adto.localizations.put(entry.getKey(), new LocalizableAttributeDefinitionDTO());
								}

								adto.localizations.get(entry.getKey()).name = entry.getValue();
							}
						}

						if (ad.hasAttribute(DESCRIPTION_ATTR)) {

							adto.description = getUnlocalizedAttr(ad.getAttribute(DESCRIPTION_ATTR), localizations);

							for (Entry<String,String> entry : getLocalizedAttr(ad.getAttribute(DESCRIPTION_ATTR),
									localizations).entrySet()) {

								if (!adto.localizations.containsKey(entry.getKey())) {

									adto.localizations.put(entry.getKey(), new LocalizableAttributeDefinitionDTO());
								}

								adto.localizations.get(entry.getKey()).description = entry.getValue();
							}
						}

						if (ad.hasAttribute(ID_ATTR)) {

							adto.property.name = ad.getAttribute(ID_ATTR);
						}

						if (ad.hasAttribute(CARDINALITY_ATTR)) {

							try {

								adto.cardinality = Integer.valueOf(ad.getAttribute(CARDINALITY_ATTR));

							} catch (Exception e) {

								adto.cardinality = 0;
							}

						} else {

							adto.cardinality = 0;
						}

						if (adto.cardinality == 0) {

							adto.property.multipleValues = false;

						} else {

							adto.property.multipleValues = true;
						}

						if (ad.hasAttribute(TYPE_ATTR)) {

							adto.property.type = ad.getAttribute(TYPE_ATTR);

							if (adto.property.type.equals(AttributeType.PASSWORD.toString())) {

								adto.property.type = "String";
							}

						} else {

							adto.property.type = "String";
						}

						if (ad.hasAttribute(DEFAULT_ATTR)) {

							List<String> values = new LinkedList<>(
									Arrays.asList(ad.getAttribute(DEFAULT_ATTR).split("(?<!\\\\),")));

							List<String> valuesEsc = new LinkedList<>();

							for (String v : values) {

								valuesEsc.add(v.replaceAll("\\\\(?<!\\\\\\\\)", ""));
							}

							adto.property.values = new LinkedList<>(valuesEsc);
						}

						if (ad.hasAttribute(REQUIRED_ATTR)) {

							adto.required = Boolean.valueOf(ad.getAttribute(REQUIRED_ATTR));

						} else {

							adto.required = true;
						}

						if (ad.hasAttribute(MAX_ATTR)) {

							adto.max = ad.getAttribute(MAX_ATTR);
						}

						if (ad.hasAttribute(MIN_ATTR)) {

							adto.min = ad.getAttribute(MIN_ATTR);
						}

						adto.options = new LinkedList<>();

						NodeList options = ad.getElementsByTagName(OPTION_TAG);

						for (int j = 0; j < options.getLength(); j++) {

							Element option = (Element) options.item(j);

							OptionDTO opdto = new OptionDTO();

							opdto.localizations = new HashMap<>();

							if (option.hasAttribute(LABEL_ATTR)) {

								opdto.label = getUnlocalizedAttr(option.getAttribute(LABEL_ATTR), localizations);

								for (Entry<String,String> entry : getLocalizedAttr(option.getAttribute(LABEL_ATTR),
										localizations).entrySet()) {

									if (!opdto.localizations.containsKey(entry.getKey())) {

										opdto.localizations.put(entry.getKey(), new LocalizableOptionDTO());
									}

									opdto.localizations.get(entry.getKey()).label = entry.getValue();
								}
							}

							if (option.hasAttribute(VALUE_ATTR)) {

								opdto.value = option.getAttribute(VALUE_ATTR);
							}

							adto.options.add(opdto);
						}

						odto.attributes.add(adto);
					}

					dto.metatypes.add(odto);
				}

			} catch (Exception e) {

				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void verify(BundleMetadataDTO dto) throws Exception {

		dto.metatypes = replaceNull(dto.metatypes);

		for (ObjectClassDefinitionDTO e : dto.metatypes) {

			if (e.id == null) {

				error("the metatype does not declare an id: metatype index = " + dto.metatypes.indexOf(e));
			}

			if (e.name == null) {

				error("the metatype does not declare a name: metatype index = " + dto.metatypes.indexOf(e));
			}

			e.attributes = replaceNull(e.attributes);

			for (AttributeDefinitionDTO p : e.attributes) {

				if (p.cardinality == null) {

					error("the attribute does not declare a cardinality: attribute index = " + e.attributes.indexOf(p)
							+ ", metatype index = " + dto.metatypes.indexOf(e));
				}

				p.localizations = replaceNull(p.localizations);

				if (p.name == null) {

					error("the attribute does not declare a name: attribute index = " + e.attributes.indexOf(p)
							+ ", metatype index = " + dto.metatypes.indexOf(e));
				}

				p.options = replaceNull(p.options);

				for (OptionDTO o : p.options) {

					o.localizations = replaceNull(o.localizations);

					if (o.value == null) {

						error("the option does not declare a value: option index = " + p.options.indexOf(o)
								+ ",attribute index = " + e.attributes.indexOf(p) + ", metatype index = "
								+ dto.metatypes.indexOf(e));
					}
				}

				if (p.property == null) {

					error("the attribute does not declare a property: attribute index = " + e.attributes.indexOf(p)
							+ ", metatype index = " + dto.metatypes.indexOf(e));
				}

				if (p.required == null) {

					error("the attribute does not declare if it is required: attribute index = "
							+ e.attributes.indexOf(p) + ", metatype index = " + dto.metatypes.indexOf(e));
				}

				if (p.property.multipleValues == null) {

					error("the attribute does not declare a property cardinality: attribute index = "
							+ e.attributes.indexOf(p) + ", metatype index = " + dto.metatypes.indexOf(e));
				}

				if (p.property.name == null) {

					error("the attribute does not declare a property name: attribute index = " + e.attributes.indexOf(p)
							+ ", metatype index = " + dto.metatypes.indexOf(e));
				}

				if (p.property.type == null) {

					error("the attribute does not declare a property type: attribute index = " + e.attributes.indexOf(p)
							+ ", metatype index = " + dto.metatypes.indexOf(e));
				}

				if (p.property.multipleValues && p.cardinality == 0) {

					error("the attribute cardinality does not match the property cardinality: attribute index = "
							+ e.attributes.indexOf(p) + ", metatype index = " + dto.metatypes.indexOf(e));
				}
			}

			e.factoryPids = replaceNull(e.factoryPids);
			e.icons = replaceNull(e.icons);

			for (IconDTO i : e.icons) {

				if (i.url == null) {

					error("the metatype declare an icon with no url: icon index = " + e.icons.indexOf(i)
							+ ", metatype index = " + dto.metatypes.indexOf(e));
				}
			}

			e.pids = replaceNull(e.pids);
			e.localizations = replaceNull(e.localizations);
		}
	}

	private Map<String,Map<String,String>> getLocalization(String path, Jar jar) {

		Map<String,Map<String,String>> result = new HashMap<>();

		for (Entry<String,Resource> entry : jar.getResources().entrySet()) {

			if (entry.getKey().startsWith(path)) {

				String lang = entry.getKey().substring(path.length()).replaceFirst("\\..*", "");

				if (lang.startsWith("_")) {

					lang = lang.substring(1);
				}

				try (InputStream inProp = entry.getValue().openInputStream()) {

					Properties prop = new Properties();

					prop.load(inProp);

					Map<String,String> properties = new HashMap<>();

					for (String key : prop.stringPropertyNames()) {

						properties.put(key, prop.getProperty(key));
					}

					result.put(lang, properties);

				} catch (Exception e) {

					throw new RuntimeException(e);
				}
			}
		}

		return result;
	}

	private String getUnlocalizedAttr(String value, Map<String,Map<String,String>> localizations) {

		if (value.startsWith("%")) {

			String variable = value.substring(1);

			if (localizations.containsKey("")) {

				return localizations.get("").get(variable);

			} else {

				return null;
			}

		} else {

			return value;
		}
	}

	private Map<String,String> getLocalizedAttr(String value, Map<String,Map<String,String>> localizations) {

		Map<String,String> result = new HashMap<>();

		if (value.startsWith("%")) {

			String variable = value.substring(1);

			for (String language : localizations.keySet()) {

				if (!language.equals("") && localizations.get(language) != null) {

					result.put(language, localizations.get(language).get(variable));
				}
			}
		}

		return result;
	}
}