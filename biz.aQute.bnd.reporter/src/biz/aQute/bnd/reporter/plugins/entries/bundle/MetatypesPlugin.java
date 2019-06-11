package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.component.dto.AttributeDefinitionDTO;
import biz.aQute.bnd.reporter.component.dto.IconDTO;
import biz.aQute.bnd.reporter.component.dto.ObjectClassDefinitionDTO;
import biz.aQute.bnd.reporter.component.dto.OptionDTO;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.helpers.LocaleHelper;

/**
 * This plugin allows to add all the metatype resources defined in a bundle to
 * the report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.METATYPES)
public class MetatypesPlugin implements ReportEntryPlugin<Jar>, Plugin {

	final private static String			METATYPE_DIR						= "OSGI-INF/metatype";
	final private static String			DEFAULT_LOCALIZATION_BASE			= "OSGI-INF/l10n/";
	final private static String			DEFAULT_BUNDLE_LOCALIZATION_BASE	= "OSGI-INF/l10n/bundle";

	final private static String			DESIGNATE_TAG						= "Designate";
	final private static String			OCD_TAG								= "OCD";
	final private static String			AD_TAG								= "AD";
	final private static String			ICON_TAG							= "Icon";
	final private static String			OPTION_TAG							= "Option";

	final private static String			LOCALIZATION_ATTR					= "localization";
	final private static String			ID_ATTR								= "id";
	final private static String			NAME_ATTR							= "name";
	final private static String			DESCRIPTION_ATTR					= "description";
	final private static String			RESOURCE_ATTR						= "resource";
	final private static String			SIZE_ATTR							= "size";
	final private static String			PID_ATTR							= "pid";
	final private static String			FACTORY_PID_ATTR					= "factoryPid";
	final private static String			CARDINALITY_ATTR					= "cardinality";
	final private static String			TYPE_ATTR							= "type";
	final private static String			DEFAULT_ATTR						= "default";
	final private static String			REQUIRED_ATTR						= "required";
	final private static String			MIN_ATTR							= "min";
	final private static String			MAX_ATTR							= "max";
	final private static String			LABEL_ATTR							= "label";
	final private static String			VALUE_ATTR							= "value";

	private Reporter					_reporter;
	private final Map<String, String>	_properties							= new HashMap<>();

	public MetatypesPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.METATYPES);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Jar.class.getCanonicalName());
	}

	@Override
	public Object extract(final Jar jar, final Locale locale) throws Exception {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		final DocumentBuilder db = DocumentBuilderFactory.newInstance()
			.newDocumentBuilder();

		final List<ObjectClassDefinitionDTO> metas = new LinkedList<>();
		if (jar.getDirectories()
			.get(METATYPE_DIR) != null) {
			for (final Resource metatype : jar.getDirectories()
				.get(METATYPE_DIR)
				.values()) {
				try (InputStream in = metatype.openInputStream()) {
					Document doc = null;
					try {
						doc = db.parse(in);
					} catch (final Exception e) {
						_reporter.warning("unable to parse metatype xml file", e);
						doc = null;
					}
					if (doc != null) {
						final Element root = doc.getDocumentElement();
						final ObjectClassDefinitionDTO meta = new ObjectClassDefinitionDTO();

						final NodeList ocds = root.getElementsByTagName(OCD_TAG);
						if (ocds.getLength() > 0) {
							final Element ocd = ((Element) ocds.item(0));

							if (ocd.hasAttribute(ID_ATTR)) {
								meta.id = ocd.getAttribute(ID_ATTR);
							} else {
								meta.id = "!! MISSING !!";
								_reporter.warning("missing ocd id attribute in metatype");
							}

							final LocaleHelper loHelper;
							if (root.hasAttribute(LOCALIZATION_ATTR)) {
								loHelper = getLocalizationHelper(jar, locale, root.getAttribute(LOCALIZATION_ATTR));
							} else {
								loHelper = getLocalizationHelper(jar, locale, DEFAULT_LOCALIZATION_BASE + meta.id);
							}

							if (ocd.hasAttribute(NAME_ATTR)) {
								final String name = loHelper.get(ocd.getAttribute(NAME_ATTR));
								if (name == null) {
									meta.name = "!! MISSING !!";
									_reporter.warning("missing ocd name attribute for metatype: %s", meta.id);
								} else {
									meta.name = name;
								}
							} else {
								meta.name = "!! MISSING !!";
								_reporter.warning("missing ocd name attribute for metatype: %s", meta.id);
							}

							if (ocd.hasAttribute(DESCRIPTION_ATTR)) {
								final String description = loHelper.get(ocd.getAttribute(DESCRIPTION_ATTR));
								if (description != null) {
									meta.description = description;
								}
							}

							final NodeList designates = root.getElementsByTagName(DESIGNATE_TAG);
							for (int i = 0; i < designates.getLength(); i++) {
								final Element designate = (Element) designates.item(i);

								if (designate.hasAttribute(PID_ATTR)) {
									meta.pids.add(designate.getAttribute(PID_ATTR));
								}

								if (designate.hasAttribute(FACTORY_PID_ATTR)) {
									meta.factoryPids.add(designate.getAttribute(FACTORY_PID_ATTR));
								}
							}

							final NodeList icons = ocd.getElementsByTagName(ICON_TAG);
							for (int i = 0; i < icons.getLength(); i++) {

								final Element icon = (Element) icons.item(i);

								final IconDTO iconDto = new IconDTO();

								if (icon.hasAttribute(RESOURCE_ATTR)) {
									iconDto.resource = icon.getAttribute(RESOURCE_ATTR);
								} else {
									iconDto.resource = "!! MISSING !!";
									_reporter.warning("missing ocd resource icon attribute for metatype: %s", meta.id);
								}
								if (icon.hasAttribute(SIZE_ATTR)) {
									iconDto.size = Integer.valueOf(icon.getAttribute(SIZE_ATTR));
								}
								meta.icons.add(iconDto);
							}

							final NodeList ads = ocd.getElementsByTagName(AD_TAG);
							for (int i = 0; i < ads.getLength(); i++) {

								final Element ad = (Element) ads.item(i);

								final AttributeDefinitionDTO attrDto = new AttributeDefinitionDTO();

								if (ad.hasAttribute(NAME_ATTR)) {
									final String name = loHelper.get(ad.getAttribute(NAME_ATTR));
									if (name == null) {
										attrDto.name = "!! MISSING !!";
										_reporter.warning("missing AD name attribute for metatype: %s", meta.id);
									} else {
										attrDto.name = name;
									}
								} else {
									attrDto.name = "!! MISSING !!";
									_reporter.warning("missing AD name attribute for metatype: %s", meta.id);
								}

								if (ad.hasAttribute(DESCRIPTION_ATTR)) {
									final String description = loHelper.get(ad.getAttribute(DESCRIPTION_ATTR));
									if (description != null) {
										attrDto.description = description;
									}
								}

								if (ad.hasAttribute(ID_ATTR)) {
									attrDto.id = ad.getAttribute(ID_ATTR);
								} else {
									attrDto.id = "!! MISSING !!";
									_reporter.warning("missing AD id attribute for metatype: %s", meta.id);
								}

								if (ad.hasAttribute(CARDINALITY_ATTR)) {
									attrDto.cardinality = Integer.parseInt(ad.getAttribute(CARDINALITY_ATTR));
								}

								if (ad.hasAttribute(TYPE_ATTR)) {
									attrDto.type = ad.getAttribute(TYPE_ATTR);
								}

								if (ad.hasAttribute(DEFAULT_ATTR)) {

									final List<String> values = new LinkedList<>(
										Arrays.asList(ad.getAttribute(DEFAULT_ATTR)
											.split("(?<!\\\\),")));

									final List<String> valuesEsc = new LinkedList<>();

									for (final String v : values) {

										valuesEsc.add(v.replaceAll("\\\\(?<!\\\\\\\\)", ""));
									}

									for (final String v : valuesEsc) {
										attrDto.values.add(v);
									}
								}

								if (ad.hasAttribute(REQUIRED_ATTR)) {
									attrDto.required = Boolean.parseBoolean(ad.getAttribute(REQUIRED_ATTR));
								}

								if (ad.hasAttribute(MAX_ATTR)) {
									attrDto.max = ad.getAttribute(MAX_ATTR);
								}

								if (ad.hasAttribute(MIN_ATTR)) {
									attrDto.min = ad.getAttribute(MIN_ATTR);
								}

								final NodeList options = ad.getElementsByTagName(OPTION_TAG);
								for (int j = 0; j < options.getLength(); j++) {
									final Element option = (Element) options.item(j);
									final OptionDTO oDto = new OptionDTO();

									String value = null;
									if (option.hasAttribute(VALUE_ATTR)) {
										value = option.getAttribute(VALUE_ATTR);
										oDto.value = value;
									} else {
										oDto.value = "!! MISSING !!";
										_reporter.warning("missing AD option value attribute for metatype %s", meta.id);
									}

									if (option.hasAttribute(LABEL_ATTR)) {
										final String label = loHelper.get(option.getAttribute(LABEL_ATTR));
										if (label != null) {
											oDto.label = label;
										} else {
											oDto.label = oDto.value;
										}
									} else {
										oDto.label = oDto.value;
									}

									attrDto.options.add(oDto);
								}
								meta.attributes.add(attrDto);
							}
							metas.add(meta);
						}
					}
				}
			}
		}
		return !metas.isEmpty() ? metas : null;
	}

	private LocaleHelper getLocalizationHelper(final Jar jar, final Locale locale, final String basePath)
		throws Exception {
		LocaleHelper result;

		result = LocaleHelper.createIfPresent(jar, locale, basePath);

		if (result == null) {
			String path = jar.getManifest()
				.getMainAttributes()
				.getValue(Constants.BUNDLE_LOCALIZATION);

			if (path == null || path.isEmpty()) {
				path = DEFAULT_BUNDLE_LOCALIZATION_BASE;
			}

			result = LocaleHelper.createIfPresent(jar, locale, path);
		}

		if (result == null) {
			return LocaleHelper.empty();
		}

		return result;
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public void setReporter(final Reporter processor) {
		_reporter = processor;
	}
}
