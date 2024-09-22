package com.chocohead.vineflower;

import java.util.Iterator;

import org.teavm.jso.core.JSArrayReader;

public class Utils {
	public static <T> Iterable<T> iterate(JSArrayReader<T> array) {
		return () -> new Iterator<>() {
			private int index;

			@Override
			public boolean hasNext() {
				return index < array.getLength();
			}

			@Override
			public T next() {
				return array.get(index++);
			}
		};
	}
}