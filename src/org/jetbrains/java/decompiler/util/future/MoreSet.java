package org.jetbrains.java.decompiler.util.future;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MoreSet {
	public static <T> Set<T> copyOf(Collection<? extends T> things) {
		Set<T> out = new HashSet<>(things);
		if (out.contains(null)) throw new NullPointerException();
		return Collections.unmodifiableSet(out);
	}
}