package org.jetbrains.java.decompiler.util.future;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MoreInputStream {
	public static byte[] readNBytes(InputStream in, int target) throws IOException {
		return in.readNBytes(target);
	}

	public static byte[] readAllBytes(InputStream in) throws IOException {
		return in.readAllBytes();
	}

	public static long transferTo(InputStream in, OutputStream out) throws IOException {
		return in.transferTo(out);
	}
}