package aQute.remote.agent;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import aQute.remote.api.XBundleInfoDTO;
import aQute.remote.api.XServiceDTO;

public class XServiceAdmin {

	private XServiceAdmin() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static List<XServiceDTO> get(BundleContext context) {
		requireNonNull(context);
		FrameworkDTO dto = context.getBundle(Constants.SYSTEM_BUNDLE_ID)
			.adapt(FrameworkDTO.class);
		return dto.services.stream()
			.map(s -> toDTO(s, context))
			.collect(toList());
	}

	private static XServiceDTO toDTO(ServiceReferenceDTO refDTO, BundleContext context) {
		XServiceDTO dto = new XServiceDTO();

		XBundleInfoDTO bundleInfo = new XBundleInfoDTO();
		bundleInfo.id = refDTO.bundle;
		bundleInfo.symbolicName = bsn(refDTO.bundle, context);

		dto.id = refDTO.id;
		dto.bundleId = bundleInfo.id;
		dto.registeringBundle = bundleInfo.symbolicName;
		dto.properties = refDTO.properties.entrySet()
			.stream()
			.collect(toMap(Map.Entry::getKey, e -> arrayToString(e.getValue())));
		dto.usingBundles = getUsingBundles(refDTO.usingBundles, context);
		dto.types = getObjectClass(refDTO.properties);

		return dto;
	}

	private static List<String> getObjectClass(Map<String, Object> properties) {
		Object objectClass = properties.get(Constants.OBJECTCLASS);
		return Arrays.asList((String[]) objectClass);
	}

	private static String arrayToString(Object value) {
		if (value instanceof String[]) {
			return Arrays.asList((String[]) value)
				.toString();
		}
		return value.toString();
	}

	private static List<XBundleInfoDTO> getUsingBundles(long[] usingBundles, BundleContext context) {
		List<XBundleInfoDTO> bundles = new ArrayList<>();
		for (long id : usingBundles) {
			String bsn = bsn(id, context);

			XBundleInfoDTO dto = new XBundleInfoDTO();
			dto.id = id;
			dto.symbolicName = bsn;

			bundles.add(dto);
		}
		return bundles;
	}

	private static String bsn(final long id, final BundleContext context) {
		for (final Bundle b : context.getBundles()) {
			if (b.getBundleId() == id) {
				return b.getSymbolicName();
			}
		}
		return null;
	}

}
