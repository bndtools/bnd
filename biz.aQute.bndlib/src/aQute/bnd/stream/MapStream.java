package aQute.bnd.stream;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public interface MapStream<K, V> extends BaseStream<Entry<K, V>, MapStream<K, V>> {
	static <K, V> MapStream<K, V> of(Map<? extends K, ? extends V> map) {
		return of(map.entrySet());
	}

	static <K, V> MapStream<K, V> ofNullable(Map<? extends K, ? extends V> map) {
		return (map != null) ? of(map) : empty();
	}

	static <K, V> MapStream<K, V> of(Collection<? extends Entry<? extends K, ? extends V>> collection) {
		return of(collection.stream());
	}

	static <K, V> MapStream<K, V> ofNullable(Collection<? extends Entry<? extends K, ? extends V>> collection) {
		return (collection != null) ? of(collection) : empty();
	}

	static <K, V> MapStream<K, V> of(Stream<? extends Entry<? extends K, ? extends V>> stream) {
		return new EntryPipeline<>(stream);
	}

	static <K, V> MapStream<K, V> ofNullable(Stream<? extends Entry<? extends K, ? extends V>> stream) {
		return (stream != null) ? of(stream) : empty();
	}

	static <K, V> MapStream<K, V> concat(MapStream<? extends K, ? extends V> a, MapStream<? extends K, ? extends V> b) {
		return of(Stream.concat(a.entries(), b.entries()));
	}

	static <K, V> MapStream<K, V> empty() {
		return of(Stream.empty());
	}

	static <K, V> MapStream<K, V> of(K k1, V v1) {
		return of(Stream.of(entry(k1, v1)));
	}

	static <K, V> MapStream<K, V> of(K k1, V v1, K k2, V v2) {
		return ofEntries(entry(k1, v1), entry(k2, v2));
	}

	static <K, V> MapStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
		return ofEntries(entry(k1, v1), entry(k2, v2), entry(k3, v3));
	}

	static <K, V> MapStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		return ofEntries(entry(k1, v1), entry(k2, v2), entry(k3, v3), entry(k4, v4));
	}

	@SafeVarargs
	static <K, V> MapStream<K, V> ofEntries(Entry<? extends K, ? extends V>... entries) {
		return of(Arrays.stream(entries));
	}

	static <O, K, V> MapStream<K, V> ofEntries(Stream<? extends O> stream,
		Function<? super O, ? extends Entry<? extends K, ? extends V>> entryMapper) {
		return of(stream.map(entryMapper));
	}

	static <K, V> Entry<K, V> entry(K key, V value) {
		return new SimpleImmutableEntry<>(key, value);
	}

	Stream<Entry<K, V>> entries();

	Stream<K> keys();

	Stream<V> values();

	MapStream<K, V> distinct();

	MapStream<K, V> filter(BiPredicate<? super K, ? super V> filter);

	MapStream<K, V> filterKey(Predicate<? super K> filter);

	MapStream<K, V> filterValue(Predicate<? super V> filter);

	<R, S> MapStream<R, S> map(BiFunction<? super K, ? super V, ? extends Entry<? extends R, ? extends S>> mapper);

	<R> MapStream<R, V> mapKey(Function<? super K, ? extends R> mapper);

	<S> MapStream<K, S> mapValue(Function<? super V, ? extends S> mapper);

	<O> Stream<O> mapToObj(BiFunction<? super K, ? super V, ? extends O> mapper);

	IntStream mapToInt(ToIntBiFunction<? super K, ? super V> mapper);

	LongStream mapToLong(ToLongBiFunction<? super K, ? super V> mapper);

	DoubleStream mapToDouble(ToDoubleBiFunction<? super K, ? super V> mapper);

	<R, S> MapStream<R, S> flatMap(
		BiFunction<? super K, ? super V, ? extends MapStream<? extends R, ? extends S>> mapper);

	<O> Stream<O> flatMapToObj(BiFunction<? super K, ? super V, ? extends Stream<? extends O>> mapper);

	IntStream flatMapToInt(BiFunction<? super K, ? super V, ? extends IntStream> mapper);

	LongStream flatMapToLong(BiFunction<? super K, ? super V, ? extends LongStream> mapper);

	DoubleStream flatMapToDouble(BiFunction<? super K, ? super V, ? extends DoubleStream> mapper);

	MapStream<K, V> peek(BiConsumer<? super K, ? super V> peek);

	MapStream<K, V> peekKey(Consumer<? super K> peek);

	MapStream<K, V> peekValue(Consumer<? super V> peek);

	MapStream<K, V> sorted();

	MapStream<K, V> sorted(Comparator<? super Entry<K, V>> comparator);

	MapStream<K, V> sortedByKey();

	MapStream<K, V> sortedByKey(Comparator<? super K> comparator);

	MapStream<K, V> sortedByValue();

	MapStream<K, V> sortedByValue(Comparator<? super V> comparator);

	MapStream<K, V> limit(long maxSize);

	MapStream<K, V> skip(long n);

	long count();

	void forEach(BiConsumer<? super K, ? super V> consumer);

	void forEachOrdered(BiConsumer<? super K, ? super V> consumer);

	boolean anyMatch(BiPredicate<? super K, ? super V> predicate);

	boolean allMatch(BiPredicate<? super K, ? super V> predicate);

	boolean noneMatch(BiPredicate<? super K, ? super V> predicate);

	<R> R collect(Supplier<R> supplier, BiConsumer<R, ? super Entry<? extends K, ? extends V>> accumulator,
		BiConsumer<R, R> combiner);

	<R, A> R collect(Collector<? super Entry<? extends K, ? extends V>, A, R> collector);

	static <K, V> Collector<? super Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
		return Collectors.toMap(Entry::getKey, Entry::getValue);
	}

	static <K, V> Collector<? super Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap(
		BinaryOperator<V> mergeFunction) {
		return Collectors.toMap(Entry::getKey, Entry::getValue, mergeFunction);
	}

	static <K, V, M extends Map<K, V>> Collector<? super Entry<? extends K, ? extends V>, ?, M> toMap(
		BinaryOperator<V> mergeFunction, Supplier<M> mapSupplier) {
		return Collectors.toMap(Entry::getKey, Entry::getValue, mergeFunction, mapSupplier);
	}

	Optional<Entry<K, V>> max(Comparator<? super Entry<K, V>> comparator);

	Optional<Entry<K, V>> maxByKey(Comparator<? super K> comparator);

	Optional<Entry<K, V>> maxByValue(Comparator<? super V> comparator);

	Optional<Entry<K, V>> min(Comparator<? super Entry<K, V>> comparator);

	Optional<Entry<K, V>> minByKey(Comparator<? super K> comparator);

	Optional<Entry<K, V>> minByValue(Comparator<? super V> comparator);

	Optional<Entry<K, V>> findAny();

	Optional<Entry<K, V>> findFirst();

	Entry<K, V>[] toArray();
}
