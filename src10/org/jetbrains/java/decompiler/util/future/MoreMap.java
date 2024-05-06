package org.jetbrains.java.decompiler.util.future;

import java.util.Map;

public class MoreMap {
	public static <K, V> Map<K, V> of(K key, V value) {
		return Map.of(key, value);
	}

	public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7, K key8, V value8, K key9, V value9) {
		return Map.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6, key7, value7, key8, value8, key9, value9);
	}

	public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
		return Map.copyOf(map);
	}
}