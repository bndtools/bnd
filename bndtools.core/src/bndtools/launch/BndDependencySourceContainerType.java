package bndtools.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BndDependencySourceContainerType extends AbstractSourceContainerTypeDelegate {

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType#getMemento
     * (org.eclipse.debug.internal.core.sourcelookup.ISourceContainer)
     */
    public String getMemento(ISourceContainer container) throws CoreException {
        Document document = newDocument();
        Element element = document.createElement("default"); //$NON-NLS-1$
        document.appendChild(element);
        return serializeDocument(document);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType# createSourceContainer(java.lang.String)
     */
    public ISourceContainer createSourceContainer(String memento) throws CoreException {
        Node node = parseDocument(memento);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if ("default".equals(element.getNodeName())) { //$NON-NLS-1$
                return new DefaultSourceContainer();
            }
            abort("Unable to restore default source lookup path - expecting default element.", null);
        }
        abort("Unable to restore default source lookup path - invalid memento.", null);
        return null;
    }

}
