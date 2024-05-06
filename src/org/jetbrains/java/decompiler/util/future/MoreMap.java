package org.jetbrains.java.decompiler.util.future;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class MoreMap {
	public static <K, V> Map<K, V> of() {
		return Collections.emptyMap();
	}

	public static <K, V> Map<K, V> of(K key, V value) {
		return Collections.unmodifiableMap(Collections.singletonMap(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value")));
	}

	public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2) {
		Map<K, V> out = new HashMap<>(2, 1);
		out.put(Objects.requireNonNull(key1), Objects.requireNonNull(value1));
		out.put(Objects.requireNonNull(key2), Objects.requireNonNull(value2));
		return Collections.unmodifiableMap(out);
	}

	public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7, K key8, V value8, K key9, V value9) {
		Map<K, V> out = new HashMap<>(9, 1);
		out.put(Objects.requireNonNull(key1), Objects.requireNonNull(value1));
		out.put(Objects.requireNonNull(key2), Objects.requireNonNull(value2));
		out.put(Objects.requireNonNull(key3), Objects.requireNonNull(value3));
		out.put(Objects.requireNonNull(key4), Objects.requireNonNull(value4));
		out.put(Objects.requireNonNull(key5), Objects.requireNonNull(value5));
		out.put(Objects.requireNonNull(key6), Objects.requireNonNull(value6));
		out.put(Objects.requireNonNull(key7), Objects.requireNonNull(value7));
		out.put(Objects.requireNonNull(key8), Objects.requireNonNull(value8));
		out.put(Objects.requireNonNull(key9), Objects.requireNonNull(value9));
		return Collections.unmodifiableMap(out);
	}

	public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7, K key8, V value8, K key9, V value9, K key10, V value10) {
		Map<K, V> out = new HashMap<>(10, 1);
		out.put(Objects.requireNonNull(key1), Objects.requireNonNull(value1));
		out.put(Objects.requireNonNull(key2), Objects.requireNonNull(value2));
		out.put(Objects.requireNonNull(key3), Objects.requireNonNull(value3));
		out.put(Objects.requireNonNull(key4), Objects.requireNonNull(value4));
		out.put(Objects.requireNonNull(key5), Objects.requireNonNull(value5));
		out.put(Objects.requireNonNull(key6), Objects.requireNonNull(value6));
		out.put(Objects.requireNonNull(key7), Objects.requireNonNull(value7));
		out.put(Objects.requireNonNull(key8), Objects.requireNonNull(value8));
		out.put(Objects.requireNonNull(key9), Objects.requireNonNull(value9));
		out.put(Objects.requireNonNull(key10), Objects.requireNonNull(value10));
		return Collections.unmodifiableMap(out);
	}

	public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
		Map<K, V> out = new HashMap<>(map);
		if (out.containsKey(null) || out.containsValue(null)) throw new NullPointerException();
		return Collections.unmodifiableMap(out);
	}

	@SafeVarargs
	public static <K, V> Map<K, V> ofEntries(Entry<K, V>... entries) {
		switch (entries.length) {
		case 0:
			return of();
		case 1:
			return of(entries[0].getKey(), entries[0].getValue());
		default: {
			Map<K, V> out = new HashMap<>(entries.length << 1, 1);
			for (Entry<K, V> entry : entries) {
				out.put(Objects.requireNonNull(entry.getKey()), Objects.requireNonNull(entry.getValue()));
			}
			return out;
		}
		}
	}

	public static <K, V> Entry<K, V> entry(K key, V value) {
		return new SimpleImmutableEntry<>(Objects.requireNonNull(key), Objects.requireNonNull(value));
	}
}