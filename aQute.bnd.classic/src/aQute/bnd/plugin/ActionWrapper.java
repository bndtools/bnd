package aQute.bnd.plugin;

import org.osgi.framework.*;

import aQute.bnd.build.*;
import aQute.bnd.service.action.*;

public class ActionWrapper implements Action {
    ServiceReference ref;
    BundleContext context;
    
    public ActionWrapper(BundleContext context, ServiceReference ref) {
        this.ref = ref;
        this.context = context;
    }

    public void execute(Project project, String action) throws Exception {
        Action a = (Action) context.getService(ref);
        if ( a == null )
            throw new IllegalStateException("Command provider is gone");
        
        try {
            a.execute(project, action);
        } finally {
            context.ungetService(ref);
        }
    }

}
