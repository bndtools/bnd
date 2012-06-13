package bndtools.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;

public class Requestors {
    public static <T> Requestor<Collection< ? extends T>> emptyCollection() {
        return new Requestor<Collection< ? extends T>>() {
            public Collection<T> request(IProgressMonitor monitor) throws InvocationTargetException {
                return Collections.emptyList();
            }
        };
    }
}
