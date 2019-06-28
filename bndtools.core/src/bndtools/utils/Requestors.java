package bndtools.utils;

import java.util.Collection;
import java.util.Collections;

public class Requestors {
	public static <T> Requestor<Collection<? extends T>> emptyCollection() {
		return monitor -> Collections.emptyList();
	}
}
