package aQute.lib.persistentmap;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.lang.reflect.Type;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

/**
 * Implements a low performance but easy to use map that is backed on a
 * directory. All objects are stored as JSON objects and therefore should be
 * DTOs. Each key is a file name and the contents is the value encoded in JSON.
 * The PersistentMap will attempt to lock the directory. This is a
 * non-concurrent implementation so you must ensure it is only used in a single
 * thread. It cannot of course also not share the data directory.
 */
public class PersistentMap<V> extends AbstractMap<String, V> implements Closeable {

	final static JSONCodec				codec	= new JSONCodec();
	final File							dir;
	final File							data;
	final RandomAccessFile				lockFile;
	final Map<String, SoftReference<V>>	cache	= new HashMap<>();
	boolean								inited	= false;
	boolean								closed	= false;

	Type								type;

	public PersistentMap(File dir, Type type) throws Exception {
		this.dir = dir;
		this.type = type;
		IO.mkdirs(dir);
		if (!dir.isDirectory())
			throw new IllegalArgumentException("PersistentMap cannot create directory " + dir);

		if (!dir.canWrite())
			throw new IllegalArgumentException("PersistentMap cannot write directory " + dir);

		File f = new File(dir, "lock");
		lockFile = new RandomAccessFile(f, "rw");

		FileLock lock = lock();
		try {
			data = new File(dir, "data").getAbsoluteFile();
			IO.mkdirs(data);
			if (!dir.isDirectory())
				throw new IllegalArgumentException("PersistentMap cannot create data directory " + dir);

			if (!data.canWrite())
				throw new IllegalArgumentException("PersistentMap cannot write data directory " + data);
		} finally {
			unlock(lock);
		}

	}

	public PersistentMap(File dir, Class<V> type) throws Exception {
		this(dir, (Type) type);
	}

	public PersistentMap(File dir, Class<V> type, Map<String, V> map) throws Exception {
		this(dir, (Type) type);
		putAll(map);
	}

	public PersistentMap(File dir, Type type, Map<String, V> map) throws Exception {
		this(dir, type);
		putAll(map);
	}

	void init() {
		if (inited)
			return;

		if (closed)
			throw new IllegalStateException("PersistentMap " + dir + " is already closed");

		try {
			inited = true;
			FileLock lock = lock();
			try {
				for (File file : data.listFiles()) {
					cache.put(file.getName(), null);
				}
			} finally {
				unlock(lock);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<java.util.Map.Entry<String, V>> entrySet() {
		return new AbstractSet<Map.Entry<String, V>>() {

			@Override
			public int size() {
				init();
				return cache.size();
			}

			@Override
			public Iterator<java.util.Map.Entry<String, V>> iterator() {
				init();
				return new Iterator<Map.Entry<String, V>>() {
					Iterator<java.util.Map.Entry<String, SoftReference<V>>>	it	= cache.entrySet()
						.iterator();
					java.util.Map.Entry<String, SoftReference<V>>			entry;

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					@SuppressWarnings("unchecked")
					public java.util.Map.Entry<String, V> next() {
						try {
							entry = it.next();
							SoftReference<V> ref = entry.getValue();
							V value = null;
							if (ref != null)
								value = ref.get();

							if (value == null) {
								File file = new File(data, entry.getKey());
								value = (V) codec.dec()
									.from(file)
									.get(type);
								entry.setValue(new SoftReference<>(value));
							}

							final V v = value;

							return new Map.Entry<String, V>() {

								@Override
								public String getKey() {
									return entry.getKey();
								}

								@Override
								public V getValue() {
									return v;
								}

								@Override
								public V setValue(V value) {
									return put(entry.getKey(), value);
								}
							};
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}

					@Override
					public void remove() {
						PersistentMap.this.remove(entry.getKey());
					}
				};
			}
		};
	}

	@Override
	public V put(String key, V value) {
		init();
		try {
			V old = null;
			SoftReference<V> ref = cache.get(key);
			if (ref != null)
				old = ref.get();

			FileLock lock = lock();
			try {
				File file = new File(data, key);
				codec.enc()
					.to(file)
					.put(value);
				cache.put(key, new SoftReference<>(value));
				return old;
			} finally {
				unlock(lock);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private FileLock lock() throws IOException, InterruptedException {
		int count = 400;
		while (true)
			try {
				FileLock lock = lockFile.getChannel()
					.lock();
				if (!lock.isValid()) {
					System.err.println("Ouch, got invalid lock " + dir + " " + Thread.currentThread()
						.getName());
					return null;
				}
				return lock;
			} catch (OverlappingFileLockException e) {
				if (count-- > 0)
					TimeUnit.MILLISECONDS.sleep(5);
				else
					throw new RuntimeException("Could not obtain lock");
			}
	}

	private void unlock(FileLock lock) throws IOException {
		if (lock == null || !lock.isValid()) {
			System.err.println("Ouch, invalid lock was used " + dir + " " + Thread.currentThread()
				.getName());
			return;
		}
		lock.release();
	}

	public V remove(String key) {
		try {
			init();
			FileLock lock = lock();
			try {
				File file = new File(data, key);
				IO.deleteWithException(file);
				return cache.remove(key)
					.get();
			} finally {
				unlock(lock);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void clear() {
		init();
		try {
			FileLock lock = lock();
			try {
				IO.deleteWithException(data);
				cache.clear();
				IO.mkdirs(data);
			} finally {
				unlock(lock);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> keySet() {
		init();
		return cache.keySet();
	}

	@Override
	public void close() throws IOException {
		lockFile.close();
		closed = true;
		inited = false;
	}

	@Override
	public String toString() {
		return "PersistentMap[" + dir + "] " + super.toString();
	}

	public void clear(long whenOlder) {
		init();
		try {
			FileLock lock = lock();
			try {
				for (File f : data.listFiles()) {
					if (f.lastModified() < whenOlder)
						IO.deleteWithException(f);
				}
				cache.clear();
			} finally {
				unlock(lock);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
