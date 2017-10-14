package biz.aQute.bnd.reporter.plugin;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.reporter.ReportGeneratorPlugin;
import aQute.bnd.service.reporter.XmlReportPart;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.LocaleHelper;
import biz.aQute.bnd.reporter.generator.ReportResult;

public class MetatypesGeneratorPlugin implements ReportGeneratorPlugin {

	final private static String METATYPE_DIR = "OSGI-INF/metatype";
	final private static String DEFAULT_LOCALIZATION_BASE = "OSGI-INF/l10n/";
	final private static String DEFAULT_BUNDLE_LOCALIZATION_BASE = "OSGI-INF/l10n/bundle";

	final private static String METATYPE_TAG = "metatype";

	final private static String DESIGNATE_TAG = "Designate";
	final private static String ATTRIBUTE_TAG = "attribute";
	final private static String OCD_TAG = "OCD";
	final private static String AD_TAG = "AD";
	final private static String ICON_TAG = "Icon";
	final private static String OPTION_TAG = "Option";
	final private static String FACTORY_PID_TAG = "factory-pid";

	final private static String LOCALIZATION_ATTR = "localization";
	final private static String ID_ATTR = "id";
	final private static String NAME_ATTR = "name";
	final private static String DESCRIPTION_ATTR = "description";
	final private static String RESOURCE_ATTR = "resource";
	final private static String SIZE_ATTR = "size";
	final private static String PID_ATTR = "pid";
	final private static String FACTORY_PID_ATTR = "factoryPid";
	final private static String CARDINALITY_ATTR = "cardinality";
	final private static String TYPE_ATTR = "type";
	final private static String DEFAULT_ATTR = "default";
	final private static String REQUIRED_ATTR = "required";
	final private static String MIN_ATTR = "min";
	final private static String MAX_ATTR = "max";
	final private static String LABEL_ATTR = "label";
	final private static String VALUE_ATTR = "value";

	@Override
	public XmlReportPart report(final Jar jar, final String locale, Reporter reporter) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		final ReportResult top = new ReportResult();

		DocumentBuilder db = null;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new RuntimeException(e);
		}

		if (jar.getDirectories().get(METATYPE_DIR) != null) {
			for (final Resource metatype : jar.getDirectories().get(METATYPE_DIR).values()) {
				try (InputStream in = metatype.openInputStream()) {
					Document doc = null;
					try {
						doc = db.parse(in);
					} catch (Exception e) {
						doc = null;
					}
					if (doc != null) {
						final Element root = doc.getDocumentElement();
						final Tag mTag = new Tag(METATYPE_TAG);

						final NodeList ocds = root.getElementsByTagName(OCD_TAG);
						if (ocds.getLength() > 0) {
							final Element ocd = ((Element) ocds.item(0));

							String ocdId = null;
							if (ocd.hasAttribute(ID_ATTR)) {
								ocdId = ocd.getAttribute(ID_ATTR);
								mTag.addContent(new Tag(ID_ATTR, ocdId));
							} else {
								ocdId = "!! MISSING !!";
								mTag.addContent(new Tag(ID_ATTR, ocdId));
								reporter.warning("Metatype plugin: missing ocd id attribute in metatype");
							}

							final LocaleHelper loHelper;
							if (root.hasAttribute(LOCALIZATION_ATTR)) {
								loHelper = getLocalizationHelper(jar, locale, root.getAttribute(LOCALIZATION_ATTR));
							} else {
								loHelper = getLocalizationHelper(jar, locale, DEFAULT_LOCALIZATION_BASE + ocdId);
							}

							if (ocd.hasAttribute(NAME_ATTR)) {
								final String name = loHelper.get(ocd.getAttribute(NAME_ATTR));
								if (name == null) {
									mTag.addContent(new Tag(NAME_ATTR, "!! MISSING !!"));
									reporter.warning(
											"Metatype plugin: missing ocd name attribute for metatype: " + ocdId);
								} else {
									mTag.addContent(new Tag(NAME_ATTR, name));
								}
							} else {
								mTag.addContent(new Tag(NAME_ATTR, "!! MISSING !!"));
								reporter.warning("Metatype plugin: missing ocd name attribute for metatype: " + ocdId);
							}

							if (ocd.hasAttribute(DESCRIPTION_ATTR)) {
								final String description = loHelper.get(ocd.getAttribute(DESCRIPTION_ATTR));
								if (description != null) {
									mTag.addContent(new Tag(DESCRIPTION_ATTR, description));
								}
							}

							final NodeList designates = root.getElementsByTagName(DESIGNATE_TAG);
							for (int i = 0; i < designates.getLength(); i++) {
								final Element designate = (Element) designates.item(i);

								if (designate.hasAttribute(PID_ATTR)) {
									mTag.addContent(new Tag(PID_ATTR, designate.getAttribute(PID_ATTR)));
								}

								if (designate.hasAttribute(FACTORY_PID_ATTR)) {
									mTag.addContent(new Tag(FACTORY_PID_TAG, designate.getAttribute(FACTORY_PID_ATTR)));
								}
							}

							final NodeList icons = ocd.getElementsByTagName(ICON_TAG);
							for (int i = 0; i < icons.getLength(); i++) {

								final Element icon = (Element) icons.item(i);

								final Tag iTag = new Tag(ICON_TAG.toLowerCase());

								if (icon.hasAttribute(RESOURCE_ATTR)) {
									iTag.addContent(new Tag(RESOURCE_ATTR, icon.getAttribute(RESOURCE_ATTR)));
								} else {
									iTag.addContent(new Tag(RESOURCE_ATTR, "!! MISSING !!"));
									reporter.warning(
											"Metatype plugin: missing ocd resource icon attribute for metatype: "
													+ ocdId);
								}

								if (icon.hasAttribute(SIZE_ATTR)) {
									iTag.addContent(new Tag(SIZE_ATTR, icon.getAttribute(SIZE_ATTR)));
								}

								mTag.addContent(iTag);
							}

							final NodeList ads = ocd.getElementsByTagName(AD_TAG);
							for (int i = 0; i < ads.getLength(); i++) {

								final Element ad = (Element) ads.item(i);

								final Tag aTag = new Tag(ATTRIBUTE_TAG);

								if (ad.hasAttribute(NAME_ATTR)) {
									final String name = loHelper.get(ad.getAttribute(NAME_ATTR));
									if (name == null) {
										aTag.addContent(new Tag(NAME_ATTR, "!! MISSING !!"));
										reporter.warning(
												"Metatype plugin: missing AD name attribute for metatype: " + ocdId);
									} else {
										aTag.addContent(new Tag(NAME_ATTR, name));
									}
								} else {
									aTag.addContent(new Tag(NAME_ATTR, "!! MISSING !!"));
									reporter.warning(
											"Metatype plugin: missing AD name attribute for metatype: " + ocdId);
								}

								if (ad.hasAttribute(DESCRIPTION_ATTR)) {
									final String description = loHelper.get(ad.getAttribute(DESCRIPTION_ATTR));
									if (description != null) {
										aTag.addContent(new Tag(DESCRIPTION_ATTR, description));
									}
								}

								if (ad.hasAttribute(ID_ATTR)) {
									aTag.addContent(new Tag(ID_ATTR, ad.getAttribute(ID_ATTR)));
								} else {
									aTag.addContent(new Tag(ID_ATTR, "!! MISSING !!"));
									reporter.warning("Metatype plugin: missing AD id attribute for metatype: " + ocdId);
								}

								if (ad.hasAttribute(CARDINALITY_ATTR)) {
									aTag.addContent(new Tag(CARDINALITY_ATTR, ad.getAttribute(CARDINALITY_ATTR)));
								} else {
									aTag.addContent(new Tag(CARDINALITY_ATTR, 0));
								}

								if (ad.hasAttribute(TYPE_ATTR)) {
									aTag.addContent(new Tag(TYPE_ATTR, ad.getAttribute(TYPE_ATTR)));
								} else {
									aTag.addContent(new Tag(TYPE_ATTR, "String"));
								}

								if (ad.hasAttribute(DEFAULT_ATTR)) {

									final List<String> values = new LinkedList<>(
											Arrays.asList(ad.getAttribute(DEFAULT_ATTR).split("(?<!\\\\),")));

									final List<String> valuesEsc = new LinkedList<>();

									for (final String v : values) {

										valuesEsc.add(v.replaceAll("\\\\(?<!\\\\\\\\)", ""));
									}

									for (final String v : valuesEsc) {
										aTag.addContent(new Tag(DEFAULT_ATTR, v));
									}
								}

								if (ad.hasAttribute(REQUIRED_ATTR)) {
									aTag.addContent(new Tag(REQUIRED_ATTR, ad.getAttribute(REQUIRED_ATTR)));
								} else {
									aTag.addContent(new Tag(REQUIRED_ATTR, true));
								}

								if (ad.hasAttribute(MAX_ATTR)) {
									aTag.addContent(new Tag(MAX_ATTR, ad.getAttribute(MAX_ATTR)));
								}

								if (ad.hasAttribute(MIN_ATTR)) {
									aTag.addContent(new Tag(MIN_ATTR, ad.getAttribute(MIN_ATTR)));
								}

								final NodeList options = ad.getElementsByTagName(OPTION_TAG);
								for (int j = 0; j < options.getLength(); j++) {
									final Element option = (Element) options.item(j);
									final Tag oTag = new Tag(OPTION_TAG.toLowerCase());

									String value = null;
									if (option.hasAttribute(VALUE_ATTR)) {
										value = option.getAttribute(VALUE_ATTR);
										oTag.addContent(new Tag(VALUE_ATTR, value));
									} else {
										oTag.addContent(new Tag(VALUE_ATTR, "!! MISSING !!"));
										reporter.warning(
												"Metatype plugin: missing AD option value attribute for metatype: "
														+ ocdId);
									}

									if (option.hasAttribute(LABEL_ATTR)) {
										final String label = loHelper.get(option.getAttribute(LABEL_ATTR));
										if (label != null) {
											oTag.addContent(new Tag(LABEL_ATTR, label));
										} else {
											oTag.addContent(new Tag(LABEL_ATTR, value));
										}
									} else {
										oTag.addContent(new Tag(LABEL_ATTR, value));
									}

									mTag.addContent(oTag);
								}
								mTag.addContent(aTag);
							}
							top.add(mTag);
						}
					}
				} catch (final Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		return top;
	}

	private LocaleHelper getLocalizationHelper(final Jar jar, final String locale, final String basePath) {
		LocaleHelper result;

		result = LocaleHelper.get(jar, locale, basePath);

		if (result == null) {
			try {
				String path = jar.getManifest().getMainAttributes().getValue(Constants.BUNDLE_LOCALIZATION);

				if (path == null) {
					path = DEFAULT_BUNDLE_LOCALIZATION_BASE;
				}

				result = LocaleHelper.get(jar, locale, path);
			} catch (final Exception e) {
				return LocaleHelper.empty();
			}
		}

		if (result == null) {
			return LocaleHelper.empty();
		}

		return result;
	}
}
