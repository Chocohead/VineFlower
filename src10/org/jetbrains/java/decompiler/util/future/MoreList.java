package org.jetbrains.java.decompiler.util.future;

import java.util.Collection;
import java.util.List;

public class MoreList {
	public static <T> List<T> of() {
		return List.of();
	}

	public static <T> List<T> of(T thing) {
		return List.of(thing);
	}

	public static <T> List<T> copyOf(Collection<? extends T> things) {
		return List.copyOf(things);
	}
}