package org.jetbrains.java.decompiler.util.future;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MoreFiles {
	public static String readString(Path path) throws IOException {
		return Files.readString(path);
	}
}