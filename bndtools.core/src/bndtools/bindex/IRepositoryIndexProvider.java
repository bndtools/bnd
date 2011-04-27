package bndtools.bindex;

import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IRepositoryIndexProvider {

    void initialise(IProgressMonitor monitor) throws Exception;

    URL getUrl();

}
