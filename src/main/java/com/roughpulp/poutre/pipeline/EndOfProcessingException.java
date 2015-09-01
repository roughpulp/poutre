package com.roughpulp.poutre.pipeline;

import com.roughpulp.commons.cursors.Cursor;

public class EndOfProcessingException extends RuntimeException {

	public static <T> Cursor<T> endOfProcessing () {
		throw new EndOfProcessingException();
	}
}
