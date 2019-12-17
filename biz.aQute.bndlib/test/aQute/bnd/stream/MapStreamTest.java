package aQute.bnd.stream;

import static aQute.bnd.stream.MapStream.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapStreamTest {

	Map<String, String>		testMap;
	Entry<String, String>[]	testEntries;

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		testMap = new HashMap<>();
		testMap.put("key1", "value1");
		testMap.put("key2", "value2");
		testMap.put("key3", "value3");
		testMap.put("key4", "value4");
		testMap.put("key5", "value5");
		testEntries = testMap.entrySet()
			.toArray(new Entry[0]);
	}

	@AfterEach
	public void tearDown() throws Exception {}

	@Test
	public void ofMap() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void ofMapNullable_NotNull() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofNullable(testMap);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void ofMapNullable_Null() {
		Map<String, String> nullMap = null;
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofNullable(nullMap);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()).isEmpty();
		assertThat(supplier.get()
			.values()).isEmpty();
		assertThat(supplier.get()
			.entries()).isEmpty();
	}

	@Test
	public void ofCollection() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap.entrySet());
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void ofCollectionNullable_NotNull() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofNullable(testMap.entrySet());
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void ofCollectionNullable_Null() {
		Collection<Entry<String, String>> nullCollection = null;
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofNullable(nullCollection);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()).isEmpty();
		assertThat(supplier.get()
			.values()).isEmpty();
		assertThat(supplier.get()
			.entries()).isEmpty();
	}

	@Test
	public void ofStream() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap.entrySet()
			.stream());
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void ofStreamNullable_NotNull() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofNullable(testMap.entrySet()
			.stream());
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void ofStreamNullable_Null() {
		Stream<Entry<String, String>> nullStream = null;
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofNullable(nullStream);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()).isEmpty();
		assertThat(supplier.get()
			.values()).isEmpty();
		assertThat(supplier.get()
			.entries()).isEmpty();
	}

	@Test
	public void ofEntries_stream() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofEntries(testMap.keySet()
			.stream(), k -> entry(k.concat("key"), k.concat("value")));
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1key", "key2key", "key3key", "key4key", "key5key");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("key1value", "key2value", "key3value", "key4value", "key5value");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1key", "key1value"), entry("key2key", "key2value"),
				entry("key3key", "key3value"), entry("key4key", "key4value"), entry("key5key", "key5value"));
	}

	@Test
	public void of1() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of("key", "value");
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(1);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(1);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(1);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(1);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key", "value"));
	}

	@Test
	public void of2() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of("key1", "value1", "key2", "value2");
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "value1"), entry("key2", "value2"));
	}

	@Test
	public void of3() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of("key1", "value1", "key2", "value2", "key3",
			"value3");
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "value1"), entry("key2", "value2"),
				entry("key3", "value3"));
	}

	@Test
	public void of4() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of("key1", "value1", "key2", "value2", "key3",
			"value3", "key4", "value4");
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(4);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(4);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(4);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(4);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "value1"), entry("key2", "value2"),
				entry("key3", "value3"), entry("key4", "value4"));
	}

	@Test
	public void ofEntries_array() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.ofEntries(testEntries);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testEntries.length);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testEntries.length);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testEntries.length);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testEntries.length);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void iterator() {
		Supplier<Iterator<Entry<String, String>>> supplier = () -> MapStream.of(testMap)
			.iterator();
		assertThat(supplier.get()).toIterable()
			.containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void spliterator() {
		Supplier<Spliterator<Entry<String, String>>> supplier = () -> MapStream.of(testMap)
			.spliterator();
		assertThat(StreamSupport.stream(supplier.get(), false)).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void empty() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.empty();
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(0);
		assertThat(supplier.get()
			.keys()).isEmpty();
		assertThat(supplier.get()
			.values()).isEmpty();
		assertThat(supplier.get()
			.entries()).isEmpty();
	}

	@Test
	public void concat() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.concat(MapStream.of(testMap),
			MapStream.of("key6", "value6", "key0", "value0"));
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() + 2);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size() + 2);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size() + 2);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size() + 2);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key0", "key1", "key2", "key3", "key4", "key5", "key6");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value0", "value1", "value2", "value3", "value4", "value5", "value6");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key0", "value0"), entry("key1", "value1"),
				entry("key2", "value2"), entry("key3", "value3"), entry("key4", "value4"), entry("key5", "value5"),
				entry("key6", "value6"));
	}

	@Test
	public void collect() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.collect((Supplier<Map<String, String>>) HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
				Map::putAll)).containsExactlyInAnyOrderEntriesOf(testMap);
		assertThat(supplier.get()
			.collect((Supplier<List<Entry<?, ?>>>) ArrayList::new, List::add, List::addAll))
				.containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void collectToMap() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.collect(MapStream.toMap())).containsExactlyInAnyOrderEntriesOf(testMap);
	}

	@Test
	public void collectToMapMerging() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.concat(MapStream.of(testMap),
			MapStream.of(testMap));
		assertThat(supplier.get()
			.collect(MapStream.toMap((String v1, String v2) -> v1.concat(v2)))).containsOnly(
				entry("key1", "value1value1"), entry("key2", "value2value2"), entry("key3", "value3value3"),
				entry("key4", "value4value4"), entry("key5", "value5value5"));
	}

	@Test
	public void collectToMapSupplier() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.concat(MapStream.of(testMap),
			MapStream.of(testMap));
		LinkedHashMap<String, String> result = supplier.get()
			.collect(MapStream.toMap((v1, v2) -> v1.concat(v2), LinkedHashMap::new));
		assertThat(result).isInstanceOf(LinkedHashMap.class)
			.containsOnly(entry("key1", "value1value1"), entry("key2", "value2value2"), entry("key3", "value3value3"),
				entry("key4", "value4value4"), entry("key5", "value5value5"));
	}

	@Test
	public void distinct() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream
			.concat(MapStream.of(testMap), MapStream.of(testMap))
			.distinct();
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void filter() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.filter((k, v) -> !k.equals("key2"));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "value1"), entry("key3", "value3"),
				entry("key4", "value4"), entry("key5", "value5"));
		assertThat(supplier.get()
			.collect(MapStream.toMap())).hasSize(testMap.size() - 1)
				.containsOnly(entry("key1", "value1"), entry("key3", "value3"), entry("key4", "value4"),
					entry("key5", "value5"));
	}

	@Test
	public void filterKey() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.filterKey(k -> !k.equals("key2"));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "value1"), entry("key3", "value3"),
				entry("key4", "value4"), entry("key5", "value5"));
		assertThat(supplier.get()
			.collect(MapStream.toMap())).hasSize(testMap.size() - 1)
				.containsOnly(entry("key1", "value1"), entry("key3", "value3"), entry("key4", "value4"),
					entry("key5", "value5"));
	}

	@Test
	public void filterValue() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.filterValue(v -> !v.equals("value2"));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size() - 1);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "value1"), entry("key3", "value3"),
				entry("key4", "value4"), entry("key5", "value5"));
		assertThat(supplier.get()
			.collect(MapStream.toMap())).hasSize(testMap.size() - 1)
				.containsOnly(entry("key1", "value1"), entry("key3", "value3"), entry("key4", "value4"),
					entry("key5", "value5"));
	}

	@Test
	public void map() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.map((k, v) -> entry(k.concat("0"), v.concat(k)));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key10", "key20", "key30", "key40", "key50");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1key1", "value2key2", "value3key3", "value4key4", "value5key5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key10", "value1key1"), entry("key20", "value2key2"),
				entry("key30", "value3key3"), entry("key40", "value4key4"), entry("key50", "value5key5"));
		assertThat(supplier.get()
			.collect(MapStream.toMap())).containsOnly(entry("key10", "value1key1"), entry("key20", "value2key2"),
				entry("key30", "value3key3"), entry("key40", "value4key4"), entry("key50", "value5key5"));
	}

	@Test
	public void mapKey() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.mapKey(k -> k.concat("0"));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key10", "key20", "key30", "key40", "key50");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key10", "value1"), entry("key20", "value2"),
				entry("key30", "value3"), entry("key40", "value4"), entry("key50", "value5"));
		assertThat(supplier.get()
			.collect(MapStream.toMap())).containsOnly(entry("key10", "value1"), entry("key20", "value2"),
				entry("key30", "value3"), entry("key40", "value4"), entry("key50", "value5"));
	}

	@Test
	public void mapValue() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.mapValue(v -> "0".concat(v));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("0value1", "0value2", "0value3", "0value4", "0value5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "0value1"), entry("key2", "0value2"),
				entry("key3", "0value3"), entry("key4", "0value4"), entry("key5", "0value5"));
		assertThat(supplier.get()
			.collect(MapStream.toMap())).containsOnly(entry("key1", "0value1"), entry("key2", "0value2"),
				entry("key3", "0value3"), entry("key4", "0value4"), entry("key5", "0value5"));
	}

	@Test
	public void mapToObj() {
		Supplier<Stream<String>> supplier = () -> MapStream.of(testMap)
			.mapToObj((k, v) -> k.concat(v));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()).containsExactlyInAnyOrder("key1value1", "key2value2", "key3value3", "key4value4",
			"key5value5");
	}

	@Test
	public void mapToInt() {
		Supplier<IntStream> supplier = () -> MapStream.of(testMap)
			.mapToInt((k, v) -> k.length());
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.sum()).isEqualTo(testMap.size() * 4);
	}

	@Test
	public void mapToLong() {
		Supplier<LongStream> supplier = () -> MapStream.of(testMap)
			.mapToLong((k, v) -> k.length() + v.length());
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.sum()).isEqualTo(testMap.size() * (4 + 6));
	}

	@Test
	public void mapToDouble() {
		Supplier<DoubleStream> supplier = () -> MapStream.of(testMap)
			.mapToDouble((k, v) -> 2.2d * k.length());
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.sum()).isEqualTo(8.8d * testMap.size());
	}

	@Test
	public void flatMap() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.flatMap((k, v) -> MapStream.of(k, v, k.concat("0"), v.concat(k)));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() * 2);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size() * 2);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size() * 2);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size() * 2);
		assertThat(supplier.get()
			.keys()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5", "key10", "key20", "key30",
				"key40", "key50");
		assertThat(supplier.get()
			.values()).containsExactlyInAnyOrder("value1", "value2", "value3", "value4", "value5", "value1key1",
				"value2key2", "value3key3", "value4key4", "value5key5");
		assertThat(supplier.get()
			.entries()).containsExactlyInAnyOrder(entry("key1", "value1"), entry("key2", "value2"),
				entry("key3", "value3"), entry("key4", "value4"), entry("key5", "value5"), entry("key10", "value1key1"),
				entry("key20", "value2key2"), entry("key30", "value3key3"), entry("key40", "value4key4"),
				entry("key50", "value5key5"));
		assertThat(supplier.get()
			.collect(MapStream.toMap())).hasSize(testMap.size() * 2)
				.containsOnly(entry("key1", "value1"), entry("key2", "value2"), entry("key3", "value3"),
					entry("key4", "value4"), entry("key5", "value5"), entry("key10", "value1key1"),
					entry("key20", "value2key2"), entry("key30", "value3key3"), entry("key40", "value4key4"),
					entry("key50", "value5key5"));
	}

	@Test
	public void flatMapToObj() {
		Supplier<Stream<String>> supplier = () -> MapStream.of(testMap)
			.flatMapToObj((k, v) -> Stream.of(k, v, k.concat(v)));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() * 3);
		assertThat(supplier.get()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5", "value1", "value2",
			"value3", "value4", "value5", "key1value1", "key2value2", "key3value3", "key4value4", "key5value5");
	}

	@Test
	public void flatMapToInt() {
		Supplier<IntStream> supplier = () -> MapStream.of(testMap)
			.flatMapToInt((k, v) -> IntStream.of(1, k.length()));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() * 2);
		assertThat(supplier.get()
			.sum()).isEqualTo(testMap.size() * 5);
	}

	@Test
	public void flatMapToLong() {
		Supplier<LongStream> supplier = () -> MapStream.of(testMap)
			.flatMapToLong((k, v) -> LongStream.of(v.length(), k.length()));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() * 2);
		assertThat(supplier.get()
			.sum()).isEqualTo(testMap.size() * 10);
	}

	@Test
	public void flatMapToDouble() {
		Supplier<DoubleStream> supplier = () -> MapStream.of(testMap)
			.flatMapToDouble((k, v) -> DoubleStream.of(2.1d, k.length()));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size() * 2);
		assertThat(supplier.get()
			.sum()).isEqualTo(6.1d * testMap.size());
	}

	@Test
	public void peek() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		Map<String, String> peeked = new HashMap<>();
		assertThat(supplier.get()
			.peek(peeked::put)
			.collect(MapStream.toMap())).containsExactlyInAnyOrderEntriesOf(testMap);
		assertThat(peeked).containsExactlyInAnyOrderEntriesOf(testMap);
	}

	@Test
	public void peekKey() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		List<String> peeked = new ArrayList<>();
		assertThat(supplier.get()
			.peekKey(peeked::add)
			.collect(MapStream.toMap())).containsExactlyInAnyOrderEntriesOf(testMap);
		assertThat(peeked).containsExactlyInAnyOrderElementsOf(testMap.keySet());
	}

	@Test
	public void peekValue() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		List<String> peeked = new ArrayList<>();
		assertThat(supplier.get()
			.peekValue(peeked::add)
			.collect(MapStream.toMap())).containsExactlyInAnyOrderEntriesOf(testMap);
		assertThat(peeked).containsExactlyInAnyOrderElementsOf(testMap.values());
	}

	@Test
	public void sorted() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream
			.ofEntries(entry("key1", "value1"), entry("key1", "value0"), entry("key2", "value6"),
				entry("key3", "value3"), entry("key4", "value4"), entry("key5", "value5"), entry("key2", "value2"))
			.sorted();
		assertThat(supplier.get()
			.count()).isEqualTo(7);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(7);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(7);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(7);
		assertThat(supplier.get()
			.keys()).containsExactly("key1", "key1", "key2", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactly("value0", "value1", "value2", "value6", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key1", "value0"), entry("key1", "value1"), entry("key2", "value2"),
				entry("key2", "value6"), entry("key3", "value3"), entry("key4", "value4"), entry("key5", "value5"));
	}

	@Test
	public void sortedComparator() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sorted(Entry.comparingByKey(Comparator.reverseOrder()));
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactly("key5", "key4", "key3", "key2", "key1");
		assertThat(supplier.get()
			.values()).containsExactly("value5", "value4", "value3", "value2", "value1");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key5", "value5"), entry("key4", "value4"), entry("key3", "value3"),
				entry("key2", "value2"), entry("key1", "value1"));
	}

	@Test
	public void sortedByKey() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByKey();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactly("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactly("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key1", "value1"), entry("key2", "value2"), entry("key3", "value3"),
				entry("key4", "value4"), entry("key5", "value5"));
	}

	@Test
	public void sortedByKeyComparator() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByKey(Comparator.reverseOrder());
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactly("key5", "key4", "key3", "key2", "key1");
		assertThat(supplier.get()
			.values()).containsExactly("value5", "value4", "value3", "value2", "value1");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key5", "value5"), entry("key4", "value4"), entry("key3", "value3"),
				entry("key2", "value2"), entry("key1", "value1"));
	}

	@Test
	public void sortedByValue() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByValue();
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactly("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactly("value1", "value2", "value3", "value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key1", "value1"), entry("key2", "value2"), entry("key3", "value3"),
				entry("key4", "value4"), entry("key5", "value5"));
	}

	@Test
	public void sortedByValueComparator() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByValue(Comparator.reverseOrder());
		assertThat(supplier.get()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(testMap.size());
		assertThat(supplier.get()
			.keys()).containsExactly("key5", "key4", "key3", "key2", "key1");
		assertThat(supplier.get()
			.values()).containsExactly("value5", "value4", "value3", "value2", "value1");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key5", "value5"), entry("key4", "value4"), entry("key3", "value3"),
				entry("key2", "value2"), entry("key1", "value1"));
	}

	@Test
	public void limit() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByKey()
			.limit(3);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(3);
		assertThat(supplier.get()
			.keys()).containsExactly("key1", "key2", "key3");
		assertThat(supplier.get()
			.values()).containsExactly("value1", "value2", "value3");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key1", "value1"), entry("key2", "value2"), entry("key3", "value3"));
	}

	@Test
	public void skip() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByKey()
			.skip(3);
		assertThat(supplier.get()).isNotNull();
		assertThat(supplier.get()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.entries()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.keys()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.values()
			.count()).isEqualTo(2);
		assertThat(supplier.get()
			.keys()).containsExactly("key4", "key5");
		assertThat(supplier.get()
			.values()).containsExactly("value4", "value5");
		assertThat(supplier.get()
			.entries()).containsExactly(entry("key4", "value4"), entry("key5", "value5"));
	}

	@Test
	public void forEach() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		Map<String, String> forEach = new HashMap<>();
		supplier.get()
			.forEach(forEach::put);
		assertThat(forEach).containsExactlyInAnyOrderEntriesOf(testMap);
	}

	@Test
	public void forEachOrdered() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		Map<String, String> forEachOrdered = new LinkedHashMap<>();
		supplier.get()
			.sortedByKey(Comparator.reverseOrder())
			.forEachOrdered(forEachOrdered::put);
		assertThat(forEachOrdered).containsExactly(entry("key5", "value5"), entry("key4", "value4"),
			entry("key3", "value3"), entry("key2", "value2"), entry("key1", "value1"));
	}

	@Test
	public void allMatch() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.allMatch((k, v) -> k.startsWith("key") && v.startsWith("value"))).isTrue();
		assertThat(supplier.get()
			.allMatch((k, v) -> k.startsWith("key1") && v.startsWith("value"))).isFalse();
	}

	@Test
	public void anyMatch() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.anyMatch((k, v) -> k.startsWith("key") && v.startsWith("value"))).isTrue();
		assertThat(supplier.get()
			.anyMatch((k, v) -> k.startsWith("key1") && v.startsWith("value"))).isTrue();
		assertThat(supplier.get()
			.anyMatch((k, v) -> k.startsWith("1key") && v.startsWith("value"))).isFalse();
	}

	@Test
	public void noneMatch() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.noneMatch((k, v) -> k.startsWith("key") && v.startsWith("value"))).isFalse();
		assertThat(supplier.get()
			.noneMatch((k, v) -> k.startsWith("key1") && v.startsWith("value"))).isFalse();
		assertThat(supplier.get()
			.noneMatch((k, v) -> k.startsWith("1key") && v.startsWith("value"))).isTrue();
	}

	@Test
	public void max() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.max(Entry.comparingByKey(Comparator.naturalOrder()))).contains(entry("key5", "value5"));
		assertThat(supplier.get()
			.max(Entry.comparingByKey(Comparator.reverseOrder()))).contains(entry("key1", "value1"));
		assertThat(MapStream.<String, String> empty()
			.max(Entry.comparingByKey(Comparator.naturalOrder()))).isEmpty();
	}

	@Test
	public void maxByKey() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.maxByKey(Comparator.naturalOrder())).contains(entry("key5", "value5"));
		assertThat(supplier.get()
			.maxByKey(Comparator.reverseOrder())).contains(entry("key1", "value1"));
		assertThat(MapStream.<String, String> empty()
			.maxByKey(Comparator.naturalOrder())).isEmpty();
	}

	@Test
	public void maxByValue() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.maxByValue(Comparator.naturalOrder())).contains(entry("key5", "value5"));
		assertThat(supplier.get()
			.maxByValue(Comparator.reverseOrder())).contains(entry("key1", "value1"));
		assertThat(MapStream.<String, String> empty()
			.maxByValue(Comparator.naturalOrder())).isEmpty();
	}

	@Test
	public void min() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.min(Entry.comparingByValue(Comparator.naturalOrder()))).contains(entry("key1", "value1"));
		assertThat(supplier.get()
			.min(Entry.comparingByValue(Comparator.reverseOrder()))).contains(entry("key5", "value5"));
		assertThat(MapStream.<String, String> empty()
			.min(Entry.comparingByValue(Comparator.naturalOrder()))).isEmpty();
	}

	@Test
	public void minByKey() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.minByKey(Comparator.naturalOrder())).contains(entry("key1", "value1"));
		assertThat(supplier.get()
			.minByKey(Comparator.reverseOrder())).contains(entry("key5", "value5"));
		assertThat(MapStream.<String, String> empty()
			.minByKey(Comparator.naturalOrder())).isEmpty();
	}

	@Test
	public void minByValue() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.minByValue(Comparator.naturalOrder())).contains(entry("key1", "value1"));
		assertThat(supplier.get()
			.minByValue(Comparator.reverseOrder())).contains(entry("key5", "value5"));
		assertThat(MapStream.<String, String> empty()
			.minByValue(Comparator.naturalOrder())).isEmpty();
	}

	@Test
	public void findAny() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.findAny()).isNotEmpty()
				.map(Entry::getKey)
				.get()
				.asString()
				.isIn(testMap.keySet());
		assertThat(MapStream.<String, String> empty()
			.findAny()).isEmpty();
	}

	@Test
	public void findFirst() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByKey(Comparator.reverseOrder());
		assertThat(supplier.get()
			.findFirst()).isNotEmpty()
				.contains(entry("key5", "value5"));
		assertThat(MapStream.<String, String> empty()
			.findFirst()).isEmpty();
	}

	@Test
	public void toArray() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.toArray()).containsExactlyInAnyOrder(testEntries);
	}

	@Test
	public void onClose() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		AtomicBoolean closed = new AtomicBoolean(false);
		try (MapStream<String, String> stream = supplier.get()) {
			stream.onClose(() -> closed.set(true));
		}
		assertThat(closed).isTrue();
	}

	@Test
	public void sequential() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.sequential()
			.isParallel()).isFalse();
	}

	@Test
	public void parallel() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap);
		assertThat(supplier.get()
			.parallel()
			.isParallel()).isTrue();
	}

	@Test
	public void unordered() {
		Supplier<MapStream<String, String>> supplier = () -> MapStream.of(testMap)
			.sortedByKey();
		assertThat(supplier.get()
			.spliterator()
			.hasCharacteristics(Spliterator.ORDERED)).isTrue();
		assertThat(supplier.get()
			.unordered()
			.spliterator()
			.hasCharacteristics(Spliterator.ORDERED)).isFalse();
	}

}
