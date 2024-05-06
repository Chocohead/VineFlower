package org.jetbrains.java.decompiler.util.future;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MoreCollectors {
	public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
		return Collectors.toUnmodifiableList();
	}
}