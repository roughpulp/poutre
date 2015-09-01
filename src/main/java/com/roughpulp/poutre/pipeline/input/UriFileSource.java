package com.roughpulp.poutre.pipeline.input;

import com.google.common.base.Charsets;
import com.roughpulp.commons.cursors.Cursor;
import com.roughpulp.poutre.pipeline.Processor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class UriFileSource implements Processor<Void, String> {

	private final File sourceFile;
	private final boolean loop;

	public UriFileSource(File sourceFile, boolean loop) {
		this.sourceFile = sourceFile;
		this.loop = loop;
	}

	@Override
	public void open() throws Exception {}

	@Override
	public void close() throws Exception {}

	@Override
	public Cursor<String> process(Void request) throws Exception {
		return new Cursor<String>() {
			@Override
			public void close() throws IOException {
				if (reader != null) {
					reader.close();
					reader = null;
				}
			}

			@Override
			public String next() throws Exception {
				String line = readLine();
				if (loop) {
					while (line == null) {
						reader.close();
						reader = openSourceFile(sourceFile);
						line = readLine();
					}
					return line;
				} else {
					close();
					return null;
				}
			}

			private String readLine() throws IOException {
				for (;;) {
					String line = reader.readLine();
					if (line == null) {
						return null;
					} else {
						line = line.trim();
						if (! line.isEmpty()) {
							return line;
						}
					}
				}
			}

			private BufferedReader reader = openSourceFile(sourceFile);
		};
	}

	private static BufferedReader openSourceFile(final File sourceFile) throws FileNotFoundException {
		return new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(sourceFile), 4 * 1024), Charsets.UTF_8));
	}
}
