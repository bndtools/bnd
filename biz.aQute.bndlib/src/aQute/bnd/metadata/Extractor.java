package aQute.bnd.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

abstract class Extractor {

	abstract protected void error(String error) throws Exception;

	final protected <T> List<T> replaceNull(List<T> list) {

		if (list == null) {

			return new ArrayList<>();

		} else {

			ListIterator<T> it = list.listIterator();

			while (it.hasNext()) {

				if (it.next() == null) {

					it.remove();
				}
			}

			return list;
		}
	}

	final protected <K, V> Map<K,V> replaceNull(Map<K,V> map) {

		if (map == null) {

			return new LinkedHashMap<>();

		} else {

			Iterator<Entry<K,V>> it = map.entrySet().iterator();

			while (it.hasNext()) {

				Entry<K,V> e = it.next();

				if (e.getKey() == null || e.getValue() == null) {

					it.remove();
				}
			}

			return map;
		}
	}
}
