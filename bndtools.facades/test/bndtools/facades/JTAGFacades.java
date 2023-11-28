package bndtools.facades;

import java.util.function.Supplier;

import bndtools.facades.IAdapterFactoryFacade.Delegate;

public interface JTAGFacades {

	static Supplier<Delegate> facade(IAdapterFactoryFacade.Facade f) {
		return f.bind;
	}
}
