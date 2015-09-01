package com.roughpulp.commons.cursors;

import com.roughpulp.commons.iterators.AutoCloseableIterator;

import java.io.IOException;
import java.util.Iterator;

public class Cursors {

	public static <E> AutoCloseableIterator<E> toIterator (final Cursor<E> cursor) {
		return new AutoCloseableIterator<E> () {
			@Override
			public void close() throws Exception { cursor.close(); }

			@Override
			public boolean hasNext() {
				nextOnce();
				return next != null;
			}

			@Override
			public E next() {
				nextOnce();
				didNext = false;
				return next;
			}

			@Override
			public void remove() { throw new UnsupportedOperationException(); }

			private void nextOnce () {
				if (! didNext) {
					didNext = true;
					try {
						next = cursor.next();
					} catch (final Exception ex) {
						if (ex instanceof RuntimeException) {
							throw (RuntimeException)ex;
						} else {
							throw new RuntimeException(ex);
						}
					}
				}
			}

			private boolean didNext = false;
			private E next;
		};
	}

	public static <E> Cursor<E> fromIterator (final Iterator<E> iterator) {
		return new Cursor<E>() {
			@Override
			public E next() throws Exception {
				if (iterator.hasNext()) {
					return iterator.next();
				} else {
					return null;
				}
			}

			@Override
			public void close() throws Exception {
				if (iterator instanceof AutoCloseableIterator) {
					((AutoCloseableIterator)iterator).close();
				}
			}
		};
	}

	public static <E> Cursor<E> empty () {
		return new Cursor<E>() {
			@Override
			public E next() throws Exception { return null; }
			@Override
			public void close() throws IOException {}
		};
	}

	public static <E> Cursor<E> singleton (final E item) {
		return new Cursor<E>() {
			@Override
			public E next() throws Exception {
				final E ret = next;
				next = null;
				return ret;
			}

			@Override
			public void close() throws IOException {}

			private E next = item;
		};
	}

	public static <E> Cursor<E> forever (final E item) {
		return new Cursor<E>() {
			@Override
			public E next() throws Exception {
				return item;
			}

			@Override
			public void close() throws IOException {}
		};
	}

	public static <E> Cursor<E> forArray(final E... items) {
		return new Cursor<E>() {
			@Override
			public void close() throws IOException {}

			@Override
			public E next() throws Exception {
				if (ii >= items.length) {
					return null;
				} else {
					return items[ii++];
				}
			}
			int ii = 0;
		};
	}

	public static <E> Cursor<E> synchronize (final Cursor<E> cursor) {
		return new Cursor<E>() {

			@Override
			public void close() throws Exception {
				synchronized (this) {
					if (! closed) {
						closed = true;
						cursor.close();
					}
				}
			}

			@Override
			public E next() throws Exception {
				synchronized (this) {
					if (eos) {
						return null;
					} else {
						final E next = cursor.next();
						if (next == null) {
							eos = true;
							return null;
						} else {
							return next;
						}
					}
				}
			}

			private boolean eos = false;
			private boolean closed = false;
		};
	}

	public static void drain (final Cursor<?> cursor) throws Exception {
		while (cursor.next() != null);
	}
}
