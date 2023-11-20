package bndtools.facades.core;

import org.eclipse.core.runtime.IAdapterFactory;
import org.osgi.annotation.versioning.ConsumerType;

import bndtools.facades.util.EclipseBinder;

@ConsumerType
public class AdapterFactoryFacade extends EclipseBinder<IAdapterFactory> implements IAdapterFactory {

	public AdapterFactoryFacade() {
		super(IAdapterFactory.class, null);
	}

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		return get().getAdapter(adaptableObject, adapterType);
	}

	@Override
	public Class<?>[] getAdapterList() {
		return get().getAdapterList();
	}

}
