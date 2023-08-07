package aQute.libg.classloaders;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CompositeClassLoader extends ClassLoader {
	@SafeVarargs
	public static ClassLoader of(ClassLoader... elements) {
		List<ClassLoader> list = Arrays.stream(elements)
			.filter(Objects::nonNull)
			.distinct()
			.collect(Collectors.toList());
		int size = list.size();
		if (size == 0) {
			throw new IllegalArgumentException("Must have at least one element");
		}
		if (size == 1) {
			return list.get(0);
		}
		return new CompositeClassLoader(list);
	}

	private final List<ClassLoader> elements;

	private CompositeClassLoader(List<ClassLoader> elements) {
		super(null); // no parent
		this.elements = List.copyOf(elements); // immutable list
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Stream<Class<?>> classes = elements.stream()
			.<Class<?>>map(loader -> {
				try {
					return loader.loadClass(name);
				} catch (ClassNotFoundException e) {
					return null;
				}
			})
			.filter(Objects::nonNull);
		Class<?> result = classes.findFirst()
			.orElseThrow(() -> new ClassNotFoundException(name));
		return result;
	}

	@Override
	public URL getResource(String name) {
		Stream<URL> resources = elements.stream()
			.<URL>map(loader -> loader.getResource(name))
			.filter(Objects::nonNull);
		URL result = resources.findFirst()
			.orElse(null);
		return result;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Stream<URL> resources = elements.stream()
			.<Enumeration<URL>>map(loader -> {
				try {
					return loader.getResources(name);
				} catch (IOException e) {
					return Collections.emptyEnumeration();
				}
			})
			.map(enumeration -> Spliterators.spliteratorUnknownSize(enumeration.asIterator(), Spliterator.ORDERED))
			.flatMap(spliterator -> StreamSupport.stream(spliterator, false))
			.distinct();
		Enumeration<URL> result = new SpliteratorEnumeration<>(resources.spliterator());
		return result;
	}

	final static class SpliteratorEnumeration<T> implements Enumeration<T>, Consumer<T> {
		private final Spliterator<T> spliterator;
		private boolean hasNext = false;
		private T next;

		SpliteratorEnumeration(Spliterator<T> spliterator) {
			this.spliterator = Objects.requireNonNull(spliterator);
		}

		@Override
		public boolean hasMoreElements() {
			if (hasNext) {
				return true;
			}
			if (spliterator.tryAdvance(this)) {
				return hasNext = true;
			}
			return false;
		}

		@Override
		public T nextElement() {
			if (hasMoreElements()) {
				T t = next;
				hasNext = false;
				next = null;
				return t;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void accept(T t) {
			next = t;
		}
	}
}
