package com.roughpulp.poutre.pipeline.senders;

import com.roughpulp.poutre.http_client.ErrorRecorder;
import com.roughpulp.poutre.http_client.HttpClientConfig;
import com.roughpulp.poutre.pipeline.Processor;
import com.roughpulp.poutre.http_client.LiveStats;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public abstract class BaseSender implements Processor<HttpUriRequest, HttpUriRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseSender.class);

	protected final HttpClientConfig config;
	protected LiveStats liveStats;
    protected ErrorRecorder errorRecorder;

	public BaseSender(final HttpClientConfig config) {
		this.config = config;
	}

	@Override
	public final void open() throws Exception {
		liveStats = new LiveStats(config.liveStatsPeriodSec, TimeUnit.SECONDS);
        if (config.errors == null) {
            errorRecorder = null;
        } else {
            errorRecorder = new ErrorRecorder(new File(config.errors));
            errorRecorder.start();
        }
        openInner(liveStats, errorRecorder);
		liveStats.start();
	}

    protected abstract void openInner(LiveStats liveStats, ErrorRecorder errorRecorder) throws Exception;

	@Override
	public final void close() throws Exception {
        LOGGER.info("close begin ...");
        try {
            closeInner();
        } finally {
            try {
                if (liveStats != null) {
                    liveStats.close();
                    liveStats = null;
                }
            } finally {
                if (errorRecorder!= null) {
                    errorRecorder.close();
                    errorRecorder = null;
                }
            }
        }
        LOGGER.info("close done");
	}

    protected abstract void closeInner() throws Exception;
}
