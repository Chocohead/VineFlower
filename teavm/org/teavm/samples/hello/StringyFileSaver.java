package org.teavm.samples.hello;

import java.util.Map;
import java.util.jar.Manifest;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

public class StringyFileSaver implements IResultSaver {
	private final Map<String, String> results;

	public StringyFileSaver(Map<String, String> results) {
		this.results = results;
	}

	@Override
	public void createArchive(String path, String archiveName, Manifest manifest) {
	}

	@Override
	public void saveFolder(String path) {
	}

	@Override
	public void saveDirEntry(String path, String archiveName, String entryName) {
	}

	@Override
	public void copyFile(String source, String path, String entryName) {
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
		this.results.put(entryName, content);
	}

	@Override
	public void copyEntry(String source, String path, String archiveName, String entry) {
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
		this.results.put(entryName, content);
	}

	@Override
	public void closeArchive(String path, String archiveName) {
	}
}