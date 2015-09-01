package com.roughpulp.commons.cursors;

public interface Cursor<T> extends AutoCloseable{
    T next () throws Exception;
}
