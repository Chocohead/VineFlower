package org.jetbrains.java.decompiler.util.future;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MoreInputStream {
	public static byte[] readNBytes(InputStream in, int target) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int n, left = target;
		for (byte[] buffer = new byte[Math.min(target, 4096)]; left > 0 && (n = in.read(buffer, 0, Math.min(left, 4096))) != -1;) {
            out.write(buffer, 0, Math.min(n, left));
            left -= n;
        }

		assert out.size() <= target;
        return out.toByteArray();
	}

	public static byte[] readAllBytes(InputStream in) throws IOException {
		return in.readAllBytes();
	}

	public static long transferTo(InputStream in, OutputStream out) throws IOException {
		return in.transferTo(out);
	}
}