package com.roughpulp.poutre.pipeline;

import com.roughpulp.commons.cursors.Cursor;

public interface Processor<IN, OUT> extends AutoCloseable {

    void open() throws Exception;

    Cursor<OUT> process(IN request) throws Exception;
}
