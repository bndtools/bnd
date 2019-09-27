package bndtools.pde.target;

import java.io.StringWriter;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.ui.target.ITargetLocationEditor;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class BndTargetLocation extends AbstractBundleContainer
	implements ITargetLocationUpdater, ITargetLocationEditor, ILabelProvider {
	static final String		PLUGIN_ID							= "bndtools.pde";

	static final String		MESSAGE_UNABLE_TO_LOCATE_WORKSPACE	= "Unable to locate the Bnd workspace";
	static final String		MESSAGE_UNABLE_TO_RESOLVE_BUNDLES	= "Unable to resolve bundles";

	static final String		ELEMENT_LOCATION					= "location";
	static final String		ATTRIBUTE_LOCATION_TYPE				= "type";

	private final String	type;
	private final Image		containerIcon;

	public BndTargetLocation(String type, String containerIconName) {
		this.type = Objects.requireNonNull(type);
		this.containerIcon = AbstractUIPlugin.imageDescriptorFromPlugin("bndtools.core", "/icons/" + containerIconName)
			.createImage();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ITargetLocationEditor.class) {
			return (T) this;

		} else if (adapter == ITargetLocationUpdater.class) {
			return (T) this;

		} else if (adapter == ILabelProvider.class) {
			return (T) this;

		} else {
			return super.getAdapter(adapter);
		}
	}

	@Override
	public boolean canEdit(ITargetDefinition target, ITargetLocation targetLocation) {
		return targetLocation == this;
	}

	@Override
	public boolean canUpdate(ITargetDefinition target, ITargetLocation targetLocation) {
		return targetLocation == this;
	}

	@Override
	public IStatus update(ITargetDefinition target, ITargetLocation targetLocation, IProgressMonitor monitor) {
		clearResolutionStatus();
		return Status.OK_STATUS;
	}

	@Override
	public Image getImage(Object element) {
		return containerIcon;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {}

	@Override
	public void dispose() {
		containerIcon.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor)
		throws CoreException {
		if (definition instanceof TargetDefinition) {
			return ((TargetDefinition) definition).resolveFeatures(getLocation(false), monitor);
		}
		return new TargetFeature[] {};
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String serialize() {
		Document document;
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
			document = docBuilder.newDocument();

			Element locationElement = document.createElement(ELEMENT_LOCATION);
			locationElement.setAttribute(ATTRIBUTE_LOCATION_TYPE, getType());
			document.appendChild(locationElement);

			serialize(document, locationElement);

			StreamResult result = new StreamResult(new StringWriter());
			Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(new DOMSource(document), result);
			return result.getWriter()
				.toString();
		} catch (Exception e) {
			PDECore.log(e);
			return null;
		}
	}

	protected abstract void serialize(Document document, Element locationElement);
}
