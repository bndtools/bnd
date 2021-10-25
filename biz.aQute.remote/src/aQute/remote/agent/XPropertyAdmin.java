package aQute.remote.agent;

import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;

import aQute.remote.api.XPropertyDTO;
import aQute.remote.api.XPropertyDTO.XPropertyType;

public class XPropertyAdmin {

	private XPropertyAdmin() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static List<XPropertyDTO> get(BundleContext context) {
		FrameworkDTO dto = context.getBundle(SYSTEM_BUNDLE_ID)
			.adapt(FrameworkDTO.class);
		return prepareProperties(dto.properties);
	}

	private static List<XPropertyDTO> prepareProperties(Map<String, Object> properties) {
		Map<String, XPropertyDTO> allProperties = new HashMap<>();

		for (Entry<String, Object> property : properties.entrySet()) {
			String key = property.getKey();
			String value = property.getValue()
				.toString();
			XPropertyDTO dto = createPropertyDTO(key, value, XPropertyType.FRAMEWORK);
			allProperties.put(key, dto);
		}

		@SuppressWarnings("rawtypes")
		Map systemProperties = System.getProperties();
		@SuppressWarnings("unchecked")
		Set<Entry<String, String>> sets = ((Map<String, String>) systemProperties).entrySet();
		for (Entry<String, String> property : sets) {
			String key = property.getKey();
			String value = property.getValue();
			XPropertyDTO dto = createPropertyDTO(key, value, XPropertyType.SYSTEM);
			allProperties.put(key, dto);
		}
		return allProperties.values()
			.stream()
			.collect(Collectors.toList());
	}

	private static XPropertyDTO createPropertyDTO(String name, String value, XPropertyType type) {
		XPropertyDTO dto = new XPropertyDTO();
		dto.name = name;
		dto.value = value;
		dto.type = type;
		return dto;
	}

}
