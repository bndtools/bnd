package aQute.libg.dtos;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.dto.DTO;

public class DTOMap extends AbstractMap<String, Object> {

	private final DTOsImpl	dtos;
	private final Object	dto;
	private final Field[]	fields;

	public DTOMap(DTOsImpl dtos, Object dto) {
		this.dtos = dtos;
		this.dto = dto;
		this.fields = dtos.getFields(dto);
	}

	@Override
	public int size() {
		return fields.length;
	}

	@Override
	public boolean isEmpty() {
		return fields.length == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (!(key instanceof String))
			return false;

		return dtos.bsearch(fields, 0, fields.length, (String) key) >= 0;
	}

	@Override
	public boolean containsValue(Object value) {
		for (Field f : fields) {
			Object o;
			try {
				o = f.get(dto);

				if (o == value)
					return true;
				if (o == null)
					return false;

				return o.equals(value);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// Ignore since we only have public fields
			}
		}
		return false;
	}

	@Override
	public Object get(Object key) {
		try {
			if (!(key instanceof String))
				return null;

			Field field = dtos.getField(fields, (String) key);
			if (field == null)
				return null;

			Object o = field.get(dto);
			if (o instanceof DTO) {
				return new DTOMap(dtos, o);
			} else
				return o;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// cannot happen
			return null;
		}
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return new AbstractSet<Map.Entry<String, Object>>() {

			@Override
			public Iterator<java.util.Map.Entry<String, Object>> iterator() {
				return new Iterator<Map.Entry<String, Object>>() {
					int n = 0;

					@Override
					public boolean hasNext() {
						return n < fields.length;
					}

					@Override
					public java.util.Map.Entry<String, Object> next() {
						final Field field = fields[n];
						n++;
						return new Map.Entry<String, Object>() {

							@Override
							public String getKey() {
								return field.getName();
							}

							@Override
							public Object getValue() {
								try {
									return field.get(dto);
								} catch (IllegalArgumentException | IllegalAccessException e) {
									throw new RuntimeException(e);
								}
							}

							@Override
							public Object setValue(Object value) {
								try {
									Object old = field.get(dto);
									field.set(dto, value);
									return old;
								} catch (IllegalArgumentException | IllegalAccessException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException("A DTO map cannot remove entries");
					}
				};
			}

			@Override
			public int size() {
				return DTOMap.this.size();
			}
		};
	}
}
