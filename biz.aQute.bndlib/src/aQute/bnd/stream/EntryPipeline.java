package aQute.bnd.stream;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToLongBiFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

final class EntryPipeline<K, V> implements MapStream<K, V> {
	private final Stream<Entry<K, V>> stream;

	@SuppressWarnings("unchecked")
	EntryPipeline(Stream<? extends Entry<? extends K, ? extends V>> stream) {
		this.stream = requireNonNull((Stream<Entry<K, V>>) stream);
	}

	@Override
	public Stream<Entry<K, V>> entries() {
		return stream;
	}

	@Override
	public Stream<K> keys() {
		return entries().map(Entry::getKey);
	}

	@Override
	public Stream<V> values() {
		return entries().map(Entry::getValue);
	}

	@Override
	public Iterator<Entry<K, V>> iterator() {
		return entries().iterator();
	}

	@Override
	public Spliterator<Entry<K, V>> spliterator() {
		return entries().spliterator();
	}

	@Override
	public boolean isParallel() {
		return entries().isParallel();
	}

	@Override
	public MapStream<K, V> sequential() {
		Stream<Entry<K, V>> sequential = entries().sequential();
		return (entries() == sequential) ? this : new EntryPipeline<>(sequential);
	}

	@Override
	public MapStream<K, V> parallel() {
		Stream<Entry<K, V>> parallel = entries().parallel();
		return (entries() == parallel) ? this : new EntryPipeline<>(parallel);
	}

	@Override
	public MapStream<K, V> unordered() {
		Stream<Entry<K, V>> unordered = entries().unordered();
		return (entries() == unordered) ? this : new EntryPipeline<>(unordered);
	}

	@Override
	public MapStream<K, V> onClose(Runnable closeHandler) {
		Stream<Entry<K, V>> onClose = entries().onClose(closeHandler);
		return (entries() == onClose) ? this : new EntryPipeline<>(onClose);
	}

	@Override
	public void close() {
		entries().close();
	}

	@Override
	public MapStream<K, V> distinct() {
		return new EntryPipeline<>(entries().distinct());
	}

	@Override
	public MapStream<K, V> filter(BiPredicate<? super K, ? super V> filter) {
		requireNonNull(filter);
		return new EntryPipeline<>(entries().filter(e -> filter.test(e.getKey(), e.getValue())));
	}

	@Override
	public MapStream<K, V> filterKey(Predicate<? super K> filter) {
		requireNonNull(filter);
		return new EntryPipeline<>(entries().filter(e -> filter.test(e.getKey())));
	}

	@Override
	public MapStream<K, V> filterValue(Predicate<? super V> filter) {
		requireNonNull(filter);
		return new EntryPipeline<>(entries().filter(e -> filter.test(e.getValue())));
	}

	@Override
	public <R, S> MapStream<R, S> map(
		BiFunction<? super K, ? super V, ? extends Entry<? extends R, ? extends S>> mapper) {
		requireNonNull(mapper);
		return new EntryPipeline<>(entries().map(e -> mapper.apply(e.getKey(), e.getValue())));
	}

	@Override
	public <R> MapStream<R, V> mapKey(Function<? super K, ? extends R> mapper) {
		requireNonNull(mapper);
		return new EntryPipeline<>(entries().map(e -> MapStream.entry(mapper.apply(e.getKey()), e.getValue())));
	}

	@Override
	public <S> MapStream<K, S> mapValue(Function<? super V, ? extends S> mapper) {
		requireNonNull(mapper);
		return new EntryPipeline<>(entries().map(e -> MapStream.entry(e.getKey(), mapper.apply(e.getValue()))));
	}

	@Override
	public <R> Stream<R> mapToObj(BiFunction<? super K, ? super V, ? extends R> mapper) {
		requireNonNull(mapper);
		return entries().map(e -> mapper.apply(e.getKey(), e.getValue()));
	}

	@Override
	public IntStream mapToInt(ToIntBiFunction<? super K, ? super V> mapper) {
		requireNonNull(mapper);
		return entries().mapToInt(e -> mapper.applyAsInt(e.getKey(), e.getValue()));
	}

	@Override
	public LongStream mapToLong(ToLongBiFunction<? super K, ? super V> mapper) {
		requireNonNull(mapper);
		return entries().mapToLong(e -> mapper.applyAsLong(e.getKey(), e.getValue()));
	}

	@Override
	public DoubleStream mapToDouble(ToDoubleBiFunction<? super K, ? super V> mapper) {
		requireNonNull(mapper);
		return entries().mapToDouble(e -> mapper.applyAsDouble(e.getKey(), e.getValue()));
	}

	@Override
	public <R, S> MapStream<R, S> flatMap(
		BiFunction<? super K, ? super V, ? extends MapStream<? extends R, ? extends S>> mapper) {
		requireNonNull(mapper);
		return new EntryPipeline<>(entries().flatMap(e -> mapper.apply(e.getKey(), e.getValue())
			.entries()));
	}

	@Override
	public <R> Stream<R> flatMapToObj(BiFunction<? super K, ? super V, ? extends Stream<? extends R>> mapper) {
		requireNonNull(mapper);
		return entries().flatMap(e -> mapper.apply(e.getKey(), e.getValue()));
	}

	@Override
	public IntStream flatMapToInt(BiFunction<? super K, ? super V, ? extends IntStream> mapper) {
		requireNonNull(mapper);
		return entries().flatMapToInt(e -> mapper.apply(e.getKey(), e.getValue()));
	}

	@Override
	public LongStream flatMapToLong(BiFunction<? super K, ? super V, ? extends LongStream> mapper) {
		requireNonNull(mapper);
		return entries().flatMapToLong(e -> mapper.apply(e.getKey(), e.getValue()));
	}

	@Override
	public DoubleStream flatMapToDouble(BiFunction<? super K, ? super V, ? extends DoubleStream> mapper) {
		requireNonNull(mapper);
		return entries().flatMapToDouble(e -> mapper.apply(e.getKey(), e.getValue()));
	}

	@Override
	public MapStream<K, V> peek(BiConsumer<? super K, ? super V> peek) {
		requireNonNull(peek);
		return new EntryPipeline<>(entries().peek(e -> peek.accept(e.getKey(), e.getValue())));
	}

	@Override
	public MapStream<K, V> peekKey(Consumer<? super K> peek) {
		requireNonNull(peek);
		return new EntryPipeline<>(entries().peek(e -> peek.accept(e.getKey())));
	}

	@Override
	public MapStream<K, V> peekValue(Consumer<? super V> peek) {
		requireNonNull(peek);
		return new EntryPipeline<>(entries().peek(e -> peek.accept(e.getValue())));
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	// If K is not Comparable, a ClassCastException may be thrown when the
	// terminal operation is executed.
	private static <K, V> Comparator<Entry<K, V>> comparingByKey() {
		return (Comparator) Entry.comparingByKey();
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	// If V is not Comparable, a ClassCastException may be thrown when the
	// terminal operation is executed.
	private static <K, V> Comparator<Entry<K, V>> comparingByValue() {
		return (Comparator) Entry.comparingByValue();
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	// If K and V are not Comparable, a ClassCastException may be thrown
	// when the terminal operation is executed.
	private static <K, V> Comparator<Entry<K, V>> comparing() {
		return ((Comparator) Entry.comparingByKey()).thenComparing(Entry.comparingByValue());
	}

	@Override
	public MapStream<K, V> sorted() {
		return sorted(comparing());
	}

	@Override
	public MapStream<K, V> sorted(Comparator<? super Entry<K, V>> comparator) {
		return new EntryPipeline<>(entries().sorted(comparator));
	}

	@Override
	public MapStream<K, V> sortedByKey() {
		return sorted(comparingByKey());
	}

	@Override
	public MapStream<K, V> sortedByKey(Comparator<? super K> comparator) {
		return sorted(Entry.comparingByKey(comparator));
	}

	@Override
	public MapStream<K, V> sortedByValue() {
		return sorted(comparingByValue());
	}

	@Override
	public MapStream<K, V> sortedByValue(Comparator<? super V> comparator) {
		return sorted(Entry.comparingByValue(comparator));
	}

	@Override
	public MapStream<K, V> limit(long maxSize) {
		return new EntryPipeline<>(entries().limit(maxSize));
	}

	@Override
	public MapStream<K, V> skip(long n) {
		return new EntryPipeline<>(entries().skip(n));
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> consumer) {
		requireNonNull(consumer);
		entries().forEach(e -> consumer.accept(e.getKey(), e.getValue()));
	}

	@Override
	public void forEachOrdered(BiConsumer<? super K, ? super V> consumer) {
		requireNonNull(consumer);
		entries().forEachOrdered(e -> consumer.accept(e.getKey(), e.getValue()));
	}

	@Override
	public long count() {
		return entries().count();
	}

	@Override
	public boolean anyMatch(BiPredicate<? super K, ? super V> predicate) {
		requireNonNull(predicate);
		return entries().anyMatch(e -> predicate.test(e.getKey(), e.getValue()));
	}

	@Override
	public boolean allMatch(BiPredicate<? super K, ? super V> predicate) {
		requireNonNull(predicate);
		return entries().allMatch(e -> predicate.test(e.getKey(), e.getValue()));
	}

	@Override
	public boolean noneMatch(BiPredicate<? super K, ? super V> predicate) {
		requireNonNull(predicate);
		return entries().noneMatch(e -> predicate.test(e.getKey(), e.getValue()));
	}

	@Override
	public <R, A> R collect(Collector<? super Entry<? extends K, ? extends V>, A, R> collector) {
		return entries().collect(collector);
	}

	@Override
	public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super Entry<? extends K, ? extends V>> accumulator,
		BiConsumer<R, R> combiner) {
		return entries().collect(supplier, accumulator, combiner);
	}

	@Override
	public Optional<Entry<K, V>> max(Comparator<? super Entry<K, V>> comparator) {
		return entries().max(comparator);
	}

	@Override
	public Optional<Entry<K, V>> maxByKey(Comparator<? super K> comparator) {
		return max(Entry.comparingByKey(comparator));
	}

	@Override
	public Optional<Entry<K, V>> maxByValue(Comparator<? super V> comparator) {
		return max(Entry.comparingByValue(comparator));
	}

	@Override
	public Optional<Entry<K, V>> min(Comparator<? super Entry<K, V>> comparator) {
		return entries().min(comparator);
	}

	@Override
	public Optional<Entry<K, V>> minByKey(Comparator<? super K> comparator) {
		return min(Entry.comparingByKey(comparator));
	}

	@Override
	public Optional<Entry<K, V>> minByValue(Comparator<? super V> comparator) {
		return min(Entry.comparingByValue(comparator));
	}

	@Override
	public Optional<Entry<K, V>> findAny() {
		return entries().findAny();
	}

	@Override
	public Optional<Entry<K, V>> findFirst() {
		return entries().findFirst();
	}

	@Override
	public Entry<K, V>[] toArray() {
		@SuppressWarnings("unchecked")
		Entry<K, V>[] array = entries().toArray(Entry[]::new);
		return array;
	}
}
