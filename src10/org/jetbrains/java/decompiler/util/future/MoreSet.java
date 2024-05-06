package org.jetbrains.java.decompiler.util.future;

import java.util.Collection;
import java.util.Set;

public class MoreSet {
	public static <T> Set<T> copyOf(Collection<? extends T> things) {
		return Set.copyOf(things);
	}
}