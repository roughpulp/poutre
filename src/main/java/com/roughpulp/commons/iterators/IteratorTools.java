package com.roughpulp.commons.iterators;

import java.util.Iterator;

public class IteratorTools {

	public static <E> Iterable<E> toIterable (final Iterator<E> iter) {
		return new Iterable<E>() {
			@Override
			public Iterator<E> iterator() { return iter; }
		};
	}
}
