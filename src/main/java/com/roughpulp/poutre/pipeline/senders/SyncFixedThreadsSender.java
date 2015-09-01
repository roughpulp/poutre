package com.roughpulp.poutre.pipeline.senders;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.roughpulp.commons.cursors.Cursors;
import com.roughpulp.poutre.http_client.ErrorRecorder;
import com.roughpulp.poutre.http_client.HttpClientConfig;
import com.roughpulp.poutre.http_client.PoutreHttpSyncClient;
import com.roughpulp.commons.cursors.Cursor;
import com.roughpulp.poutre.http_client.LiveStats;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.*;

public class SyncFixedThreadsSender extends BaseSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncFixedThreadsSender.class);

    private static final HttpUriRequest EOS = new HttpGet();

    private ArrayBlockingQueue<HttpUriRequest> queue;
    private ArrayList<Future<Void>> futures;
    private ThreadPoolExecutor exec;
    private PoutreHttpSyncClient client;

    public SyncFixedThreadsSender(final HttpClientConfig config) {
        super(config);
    }

    @Override
    protected void openInner(final LiveStats liveStats, final ErrorRecorder errorRecorder) throws Exception {
        queue = new ArrayBlockingQueue<>(config.threads);
        futures = new ArrayList<>(config.threads);
        client = new PoutreHttpSyncClient(config, liveStats, errorRecorder);
        exec = new ThreadPoolExecutor(
                config.threads, Integer.MAX_VALUE,
                1000 * 1000, TimeUnit.DAYS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("worker-%d")
                        .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread thread, Throwable th) {
                                LOGGER.error("uncaughtException: " + thread.getName() + ": " + th, th);
                            }
                        })
                        .build(),
                new ThreadPoolExecutor.AbortPolicy());
        queue.clear();
        for (int ii = 0; ii < config.threads; ++ii) {
            final Callable<Void> task = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    LOGGER.info("started");
                    try {
                        for (;;) {
                            final HttpUriRequest request = queue.take();
                            if (request == EOS) {
                                break;
                            } else {
                                client.request(request);
                            }
                        }
                    } catch (Exception ex) {
                        if (ex instanceof InterruptedException)  {
                            // that's okay
                        } else {
                            LOGGER.error("unexpected exception: " + ex, ex);
                            throw ex;
                        }
                    } finally {
                        LOGGER.info("stopped");
                    }
                    return null;
                }
            };
            final Future<Void> future = exec.submit(task);
            futures.add(future);
        }
    }

    @Override
    protected void closeInner() throws Exception {
        try {
            for (int ii = 0; ii < futures.size(); ++ii) {
                queue.put(EOS);
            }
            for (final Future<Void> future : futures) {
                try {
                    future.get();
                } catch (Exception ex) {
                    LOGGER.error("exception getting future :" + ex, ex);
                }
            }
        } finally {
            try {
                if (client != null) {
                    client.close();
                    client = null;
                }
            } finally {
                if (exec != null) {
                    exec.shutdownNow();
                    exec = null;
                }
            }
        }
    }

    @Override
    public Cursor<HttpUriRequest> process(HttpUriRequest request) throws Exception {
        queue.put(request);
        return Cursors.singleton(request);
    }
}
