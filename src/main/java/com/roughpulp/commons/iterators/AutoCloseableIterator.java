package com.roughpulp.commons.iterators;

import java.util.Iterator;

public interface AutoCloseableIterator<E> extends Iterator<E>, AutoCloseable {
}
