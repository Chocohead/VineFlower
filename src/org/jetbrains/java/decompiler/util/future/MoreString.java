package org.jetbrains.java.decompiler.util.future;

public class MoreString {
	public static boolean isBlank(String string) {
		return string.codePoints().allMatch(Character::isWhitespace);
	}

	public static String repeat(String string, int count) {
		if (count <= 1) {
			switch (count) {
			case 1:
				return string;
			case 0:
				return "";
			default:
				throw new IllegalArgumentException("count " + count + " < 0");
			}
		}

		int len = string.length();
		long longSize = (long) len * count;
		int size = (int) longSize;
		if (size != longSize) {
			throw new ArrayIndexOutOfBoundsException("Required array size too large: " + longSize);
		}

		char[] out = new char[size];
		string.getChars(0, len, out, 0);
		int n;
		for (n = len; n < size - n; n <<= 1) {
			System.arraycopy(out, 0, out, n, n);
		}
		System.arraycopy(out, 0, out, n, size - n);
		return new String(out);
	}
}