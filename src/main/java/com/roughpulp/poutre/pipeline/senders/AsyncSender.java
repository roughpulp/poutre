package com.roughpulp.poutre.pipeline.senders;

import com.roughpulp.commons.cursors.Cursor;
import com.roughpulp.commons.cursors.Cursors;
import com.roughpulp.poutre.http_client.HttpClientConfig;
import com.roughpulp.poutre.http_client.PoutreHttpAsyncClient;
import com.roughpulp.poutre.pipeline.Processor;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncSender implements Processor<HttpUriRequest, HttpUriRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSender.class);

    private final HttpClientConfig config;
	private PoutreHttpAsyncClient client;

	public AsyncSender(final HttpClientConfig config) {
        this.config = config;
	}

	@Override
	public void open() throws Exception {
	}

	@Override
	public void close() throws Exception {
		if (client != null) {
            client.close();
            client = null;
        }
	}

	@Override
	public Cursor<HttpUriRequest> process(final HttpUriRequest request) throws Exception {
		client.request(request);
		return Cursors.singleton(request);
	}
}
