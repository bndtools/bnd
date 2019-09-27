package aQute.lib.dtoformatter;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aQute.lib.exceptions.Exceptions;
import aQute.libg.glob.Glob;

public class DTOFormatter implements ObjectFormatter {
	private static final Cell			NULL_CELL	= Table.EMPTY;
	final Map<Class<?>, DTODescription>	descriptors	= new LinkedHashMap<>();

	public interface ItemBuilder<T> extends GroupBuilder<T> {

		ItemDescription zitem();

		default ItemBuilder<T> method(Method readMethod) {
			if (readMethod == null) {
				System.out.println("?");
				return null;
			}
			zitem().member = (o) -> {
				try {
					return readMethod.invoke(o);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			};
			return this;
		}

		default ItemBuilder<T> field(Field field) {
			zitem().member = (o) -> {
				try {
					return field.get(o);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			};
			return this;
		}

		default ItemBuilder<T> minWidth(int w) {
			zitem().minWidth = w;
			return this;
		}

		default ItemBuilder<T> maxWidth(int w) {
			zitem().maxWidth = w;
			return this;
		}

		default ItemBuilder<T> width(int w) {
			zitem().maxWidth = w;
			zitem().minWidth = w;
			return this;
		}

		default ItemBuilder<T> label(String label) {
			zitem().label = label;
			return this;
		}

		default DTOFormatterBuilder<T> count() {
			zitem().format = (base) -> {

				Object o = zitem().member.apply(base);

				if (o instanceof Collection)
					return "" + ((Collection<?>) o).size();

				else if (o.getClass()
					.isArray())
					return "" + Array.getLength(o);

				return "?";
			};
			return this;
		}

	}

	public interface GroupBuilder<T> extends DTOFormatterBuilder<T> {
		GroupDescription zgroup();

		default GroupBuilder<T> title(String title) {
			zgroup().title = title;
			return this;
		}

		default ItemBuilder<T> item(String label) {
			GroupDescription g = zgroup();
			DTODescription d = zdto();
			ItemDescription i = g.items.computeIfAbsent(label, ItemDescription::new);

			ItemBuilder<T> itemBuilder = new ItemBuilder<T>() {

				@Override
				public GroupDescription zgroup() {
					return g;
				}

				@Override
				public DTODescription zdto() {
					return d;
				}

				@Override
				public ItemDescription zitem() {
					return i;
				}

			};

			return itemBuilder;
		}

		default ItemBuilder<T> field(String field) {
			try {
				ItemBuilder<T> b = item(field);
				Field f = zdto().clazz.getField(field);

				b.zitem().member = (o) -> {
					try {
						return f.get(o);
					} catch (Exception e) {
						throw Exceptions.duck(e);
					}
				};
				return b;
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		default ItemBuilder<T> optionalField(String field) {
			try {
				return field(field);
			} catch (Exception e) {
				ItemBuilder<T> b = item(field);
				zgroup().items.remove(field);
				return b;
			}
		}

		default ItemBuilder<T> optionalMethod(String method) {
			try {
				return method(method);
			} catch (Exception e) {
				ItemBuilder<T> b = item(method);
				zgroup().items.remove(method);
				return b;
			}
		}

		default ItemBuilder<T> method(String method) {
			try {
				ItemBuilder<T> b = item(method);
				PropertyDescriptor[] properties = Introspector.getBeanInfo(zdto().clazz)
					.getPropertyDescriptors();

				Stream.of(properties)
					.filter(property -> property.getName()
						.equals(method))
					.filter(property -> property.getReadMethod() != null)
					.forEach(property -> {
						b.method(property.getReadMethod())
							.label(property.getDisplayName());
					});
				return b;
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		default GroupBuilder<T> separator(String separator) {
			zgroup().separator = separator;
			return this;
		}

		default GroupBuilder<T> fields(String string) {
			Glob glob = new Glob(string);
			Field[] fields = zdto().clazz.getFields();
			Stream.of(fields)
				.filter(field -> !Modifier.isStatic(field.getModifiers()))
				.filter(field -> glob.matches(field.getName()))
				.forEach(field -> {
					item(field.getName()).field(field);
				});
			return this;
		}

		default GroupBuilder<T> methods(String string) {
			try {
				Glob glob = new Glob(string);
				PropertyDescriptor[] properties = Introspector.getBeanInfo(zdto().clazz)
					.getPropertyDescriptors();
				Stream.of(properties)
					.filter(property -> glob.matches(property.getName()))
					.filter(property -> property.getReadMethod() != null)
					.forEach(property -> {
						item(property.getDisplayName()).method(property.getReadMethod());
					});
				return this;
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		@SuppressWarnings("unchecked")
		default GroupBuilder<T> as(Function<T, String> map) {
			zgroup().format = (Function<Object, String>) map;
			return this;
		}

		@SuppressWarnings("unchecked")
		default ItemBuilder<T> format(String label, Function<T, Object> format) {
			ItemBuilder<T> b = item(label);
			ItemDescription item = b.zitem();
			item.format = (Function<Object, Object>) format;
			return b;
		}

		default GroupBuilder<T> remove(String string) {
			zgroup().items.remove(string);
			return this;
		}
	}

	public interface DTOFormatterBuilder<T> {

		DTODescription zdto();

		default GroupBuilder<T> inspect() {
			DTODescription d = zdto();

			return new GroupBuilder<T>() {

				@Override
				public DTODescription zdto() {
					return d;
				}

				@Override
				public GroupDescription zgroup() {
					return zdto().inspect;
				}

			};
		}

		default GroupBuilder<T> line() {
			DTODescription d = zdto();

			return new GroupBuilder<T>() {

				@Override
				public DTODescription zdto() {
					return d;
				}

				@Override
				public GroupDescription zgroup() {
					return zdto().line;
				}

			};
		}

		default GroupBuilder<T> part() {
			DTODescription d = zdto();

			return new GroupBuilder<T>() {

				@Override
				public DTODescription zdto() {
					return d;
				}

				@Override
				public GroupDescription zgroup() {
					return zdto().part;
				}

			};
		}
	}

	public <T> DTOFormatterBuilder<T> build(Class<T> clazz) {
		DTODescription dto = getDescriptor(clazz, new DTODescription());

		dto.clazz = clazz;
		descriptors.put(clazz, dto);

		return () -> dto;
	}

	/*********************************************************************************************/

	private Cell inspect(Object object, DTODescription descriptor, ObjectFormatter formatter) {
		Table table = new Table(descriptor.inspect.items.size(), 2, 0);
		int row = 0;

		for (ItemDescription item : descriptor.inspect.items.values()) {
			Object o = getValue(object, item);
			Cell cell = cell(o, formatter);
			table.set(row, 0, item.label);
			table.set(row, 1, cell);
			row++;
		}
		return table;
	}

	private Object getValue(Object object, ItemDescription item) {
		if (item.format != null)
			return item.format.apply(object);

		Object target = object;
		if (!item.self) {
			if (item.member == null) {
				System.out.println("? item " + item.label);
				return "? " + item.label;
			}
			target = item.member.apply(object);
		}

		return target;
	}

	@SuppressWarnings("unchecked")
	public Cell cell(Object o, ObjectFormatter formatter) {
		if (o == null) {
			return NULL_CELL;
		}

		if (o instanceof Collection) {
			return list((Collection<Object>) o, formatter);
		} else if (o.getClass()
			.isArray()) {
			List<Object> list = new ArrayList<>();
			for (int i = 0; i < Array.getLength(o); i++) {
				list.add(Array.get(o, i));
			}
			return list(list, formatter);
		} else if (o instanceof Map) {
			return map((Map<Object, Object>) o, formatter);
		} else if (o instanceof Dictionary) {
			Map<Object, Object> map = new HashMap<>();
			Dictionary<Object, Object> dictionary = (Dictionary<Object, Object>) o;
			Enumeration<Object> e = dictionary.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				Object value = dictionary.get(key);
				map.put(key, value);
			}
			return map(map, formatter);
		} else {
			DTODescription descriptor = getDescriptor(o.getClass());
			if (descriptor == null) {
				CharSequence format = formatter.format(o, ObjectFormatter.PART, formatter);
				return new StringCell(format.toString(), o);
			}
			return part(o, descriptor, formatter);
		}
	}

	private Cell map(Map<Object, Object> map, ObjectFormatter formatter) {
		Table table = new Table(map.size() + 1, 2, 1);
		table.set(0, 0, "Key");
		table.set(0, 1, "Value");

		int row = 1;

		for (Map.Entry<Object, Object> e : map.entrySet()) {

			Cell key = cell(e.getKey(), formatter);
			Cell value = cell(e.getValue(), formatter);
			table.set(row, 0, key);
			table.set(row, 1, value);
			row++;
		}
		return table;
	}

	private Cell list(Collection<Object> list, ObjectFormatter formatter) {

		Class<?> type = null;
		for (Object o : list) {
			type = commonType(o.getClass(), type);
		}

		if (type == null) {
			return new StringCell("", null);
		}

		DTODescription descriptor = getDescriptor(type);
		if (descriptor == null) {

			if (Number.class.isAssignableFrom(type) || type.isPrimitive() || type == Character.class
				|| type == Boolean.class) {
				String s = list.stream()
					.map(Object::toString)
					.collect(Collectors.joining(", "));
				return new StringCell(s, list);
			}

			Table table = new Table(list.size(), 1, 0);
			int r = 0;
			for (Object o : list) {
				Cell c = part(o, null, formatter);
				table.set(r, 0, c);
				r++;
			}
			return table;
		}

		Table table = new Table(list.size() + 1, descriptor.line.items.size(), 1);

		int col = 0;
		for (ItemDescription item : descriptor.line.items.values()) {
			table.set(0, col, item.label);
			col++;
		}

		int row = 1;
		for (Object member : list) {
			col = 0;
			for (ItemDescription item : descriptor.line.items.values()) {
				Object o = getValue(member, item);
				Cell cell = cell(o, formatter);
				table.set(row, col, cell);
				col++;
			}
			row++;
		}
		return table;
	}

	private Class<?> commonType(Class<? extends Object> class1, Class<?> type) {
		if (type == null)
			return class1;

		if (class1.isAssignableFrom(type))
			return class1;

		if (type.isAssignableFrom(class1))
			return type;

		return Object.class;
	}

	private Cell line(Object object, DTODescription description, ObjectFormatter formatter) {
		Table table = new Table(1, description.line.items.size(), 0);
		line(object, description, 0, table, formatter);
		return table;
	}

	private void line(Object object, DTODescription description, int row, Table table, ObjectFormatter formatter) {
		int col = 0;
		for (ItemDescription item : description.line.items.values()) {
			Object o = getValue(object, item);
			Cell cell = cell(o, formatter);
			table.set(row, col, cell);
			col++;
		}
	}

	private Cell part(Object object, DTODescription descriptor, ObjectFormatter formatter) {
		if (descriptor == null) {
			return new StringCell(formatter.format(object, ObjectFormatter.PART, null)
				.toString(), object);
		}

		int col = 0;
		if (descriptor.part.format != null) {
			return new StringCell(descriptor.part.format.apply(object), object);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(descriptor.part.prefix);
		String del = "";
		for (ItemDescription item : descriptor.part.items.values()) {
			Object o = getValue(object, item);
			Cell cell = cell(o, formatter);
			sb.append(del)
				.append(cell);
			del = descriptor.part.separator;
		}
		sb.append(descriptor.part.suffix);
		return new StringCell(sb.toString(), object);
	}

	@Override
	public CharSequence format(Object o, int level, ObjectFormatter formatter) {
		while (o instanceof Wrapper) {
			o = ((Wrapper) o).whatever;
		}

		if (o == null)
			return null;

		if (isSpecial(o)) {
			return cell(o, formatter).toString();
		}

		DTODescription descriptor = getDescriptor(o.getClass());
		if (descriptor == null)
			return null;

		switch (level) {
			case ObjectFormatter.INSPECT :
				return inspect(o, descriptor, formatter).toString();

			case ObjectFormatter.LINE :
				return line(o, descriptor, formatter).toString();

			case ObjectFormatter.PART :
				return part(o, descriptor, formatter).toString();

			default :
				return null;
		}
	}

	private boolean isSpecial(Object o) {
		return o instanceof Map || o instanceof Dictionary || o instanceof Collection || o.getClass()
			.isArray();
	}

	private DTODescription getDescriptor(Class<?> clazz, DTODescription defaultDescriptor) {
		DTODescription descriptor = getDescriptor(clazz);
		if (descriptor != null)
			return descriptor;
		return defaultDescriptor;
	}

	private DTODescription getDescriptor(Class<?> clazz) {
		DTODescription description = descriptors.get(clazz);
		if (description != null)
			return description;

		description = descriptors.entrySet()
			.stream()
			.filter(e -> e.getKey()
				.isAssignableFrom(clazz))
			.map(Map.Entry::getValue)
			.findAny()
			.orElse(null);

		return description;
	}
}
