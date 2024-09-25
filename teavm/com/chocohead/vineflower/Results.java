package com.chocohead.vineflower;

import java.util.Map;
import java.util.TreeMap;

public record Results(String name, Map<String, String> classes, Map<String, Results> children) {
	public Results(String name) {
		this(name, new TreeMap<>(), new TreeMap<>());
	}
}