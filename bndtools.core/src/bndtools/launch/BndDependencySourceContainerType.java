package bndtools.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BndDependencySourceContainerType extends AbstractSourceContainerTypeDelegate {

    private static final String ELEMENT_NAME = "bnd";

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType#getMemento
     * (org.eclipse.debug.internal.core.sourcelookup.ISourceContainer)
     */
    @Override
    public String getMemento(ISourceContainer container) throws CoreException {
        Document document = newDocument();
        Element element = document.createElement(ELEMENT_NAME);
        document.appendChild(element);
        return serializeDocument(document);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType# createSourceContainer(java.lang.String)
     */
    @Override
    public ISourceContainer createSourceContainer(String memento) throws CoreException {
        Node node = parseDocument(memento);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (ELEMENT_NAME.equals(element.getNodeName())) {
                return new BndDependencySourceContainer();
            } else if ("default".equals(element.getNodeName())) { // try to gracefully handle old serialized element name
                return new BndDependencySourceContainer();
            }
            abort("Unable to restore Bnd Dependencies source lookup path - expecting bnd element.", null);
        }
        abort("Unable to restore Bnd Dependencies source lookup path - invalid memento.", null);
        return null;
    }

}
