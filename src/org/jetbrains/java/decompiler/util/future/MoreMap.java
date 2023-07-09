package org.jetbrains.java.decompiler.util.future;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class MoreMap {
	public static <K, V> Map<K, V> of(K key, V value) {
		return Collections.unmodifiableMap(Collections.singletonMap(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value")));
	}
}