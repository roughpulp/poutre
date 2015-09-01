package com.roughpulp.poutre.pipeline.senders;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.roughpulp.commons.cursors.Cursor;
import com.roughpulp.commons.cursors.Cursors;
import com.roughpulp.poutre.http_client.ErrorRecorder;
import com.roughpulp.poutre.http_client.HttpClientConfig;
import com.roughpulp.poutre.http_client.LiveStats;
import com.roughpulp.poutre.http_client.PoutreHttpSyncClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SyncVariableThreadsSender extends BaseSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncVariableThreadsSender.class);

    private ThreadPoolExecutor exec;
    private PoutreHttpSyncClient client;

    public SyncVariableThreadsSender(final HttpClientConfig config) {
        super(config);
    }

    @Override
    protected void openInner(final LiveStats liveStats, final ErrorRecorder errorRecorder) throws Exception {
        client = new PoutreHttpSyncClient(config, liveStats, errorRecorder);
        exec = new ThreadPoolExecutor(
                config.threads, Integer.MAX_VALUE,
                1000 * 1000, TimeUnit.DAYS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy());
        exec.prestartAllCoreThreads();
    }

    @Override
    public void closeInner() throws Exception {
        try {
            if (exec != null) {
                exec.shutdownNow();
                exec = null;
            }
        } finally {
            if (client != null) {
                client.close();
                client = null;
            }
        }
    }

    @Override
    public Cursor<HttpUriRequest> process(final HttpUriRequest request) throws Exception {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    client.request(request);
                } catch (final Exception ex) {
                    if (ex instanceof InterruptedException) {
                        // that's okay
                    } else {
                        LOGGER.error("exception processing " + request + " : " + ex, ex);
                    }
                }
            }
        };
        exec.submit(task);
        return Cursors.singleton(request);
    }
}
