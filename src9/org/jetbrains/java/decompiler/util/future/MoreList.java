package org.jetbrains.java.decompiler.util.future;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MoreList {
	public static <T> List<T> of() {
		return List.of();
	}

	public static <T> List<T> of(T thing) {
		return List.of(thing);
	}

	public static <T> List<T> of(T thing1, T thing2) {
		return List.of(thing1, thing2);
	}

	@SafeVarargs
	public static <T> List<T> of(T... things) {
		return List.of(things);
	}

	public static <T> List<T> copyOf(Collection<? extends T> things) {
		List<T> out = new ArrayList<T>(things);
		for (T thing : out) {
			Objects.requireNonNull(thing);
		}
		return Collections.unmodifiableList(out);
	}
}