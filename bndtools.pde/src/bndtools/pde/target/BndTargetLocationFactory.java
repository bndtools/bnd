package bndtools.pde.target;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class BndTargetLocationFactory implements ITargetLocationFactory {
	private final String type;

	public BndTargetLocationFactory(String type) {
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		if (this.type.equals(type)) {
			Element locationElement;
			try {
				DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
				Document document = docBuilder.parse(new ByteArrayInputStream(serializedXML.getBytes("UTF-8")));
				locationElement = document.getDocumentElement();

				if (this.type.equals(locationElement.getAttribute(BndTargetLocation.ATTRIBUTE_LOCATION_TYPE))) {
					return getTargetLocation(locationElement);
				}
			} catch (Exception e) {
				Logger.getLogger(getClass())
					.logError("Problem reading target location " + type, null);
				return null;
			}
		}
		return null;
	}

	public boolean isElement(Node node, String elementName) {
		return node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName()
			.equalsIgnoreCase(elementName);
	}

	public abstract ITargetLocation getTargetLocation(Element locationElement) throws CoreException;
}
