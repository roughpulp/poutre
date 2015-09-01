package com.roughpulp.poutre.http_client;

import com.google.common.util.concurrent.RateLimiter;
import com.roughpulp.poutre.pipeline.Result;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PoutreHttpAsyncClient implements AutoCloseable{

	private final CloseableHttpAsyncClient client;
	private final LiveStats stats;
	private final boolean dryRun;
	//
	private final ReentrantLock lock = new ReentrantLock(true);
	private final Condition cond = lock.newCondition();
	private final AtomicInteger concurrent = new AtomicInteger(0);

	public PoutreHttpAsyncClient(
			LiveStats stats,
			final HttpClientConfig conf) throws IOException {
        //FIXME: number of threads
		this.client = HttpClientFactory.newHttpAsyncClient(conf);
		this.client.start();
		this.stats = stats;
		this.dryRun = conf.dryRun;
	}

	@Override
	public void close() throws Exception {
		try {
			LOGGER.info("waiting for running queries ...");
			{
				final long maxSleepTimeMs = TimeUnit.MINUTES.toMillis(5);
				final long t0 = System.currentTimeMillis();
				lock.lock();
				try {
					long elapsedMs = System.currentTimeMillis() - t0;
					while (elapsedMs < maxSleepTimeMs && concurrent.get() > 0) {
						cond.await(maxSleepTimeMs - elapsedMs, TimeUnit.MILLISECONDS);
						elapsedMs = System.currentTimeMillis() - t0;
					}
				} catch (InterruptedException ex) {
					// that's okay
				} finally {
					lock.unlock();
				}
			}
			LOGGER.info("done waiting for running queries");
		} finally {
			client.close();
		}
	}

	public void request (final HttpUriRequest request) throws IOException, InterruptedException {
		if (dryRun) {
			LOGGER.info(request.getURI().toString());
			return;
		}
        final Result result = new Result();
		result.begin();
		stats.incQueriesCount();
		stats.incRunningCount();
		concurrent.incrementAndGet();

		final BasicAsyncRequestProducer requestProducer = new BasicAsyncRequestProducer(determineTarget(request), request) {
			@Override
			public void requestCompleted(final HttpContext context) {
			}

			@Override
			public void failed(final Exception ex) {
				handleFailure(request, result, ex);
			}
		};

		final FutureCallback<HttpResponse> callback = new FutureCallback<HttpResponse>(){
			@Override
			public void completed(HttpResponse resp) {
				try {
					result.end();
                    result.statusCode = resp.getStatusLine().getStatusCode();
					if (result.statusCode == 200) {
						stats.incOkCount();
						stats.pushQTime(result.time);
					} else {
						stats.incKoCount();
//						if (errorRecorder != null) {
//							errorRecorder.record(uri, resp);
//						}
					}
//					respHandler.handle(queryRes, resp);
//					pipeline.add(queryRes);
				} catch (Exception ex) {
					LOGGER.error("request completed exception", ex);
					throw new RuntimeException(ex);
				} finally {
					finishRequest();
				}
			}

			@Override
			public void failed(Exception ex) {
				handleFailure(request, result, ex);
			}

			@Override
			public void cancelled() { finishRequest(); }
		};

		client.execute(requestProducer, HttpAsyncMethods.createConsumer(), callback);
	}

	private HttpHost determineTarget(final HttpUriRequest request) throws ClientProtocolException {
		Args.notNull(request, "HTTP request");
		// A null target may be acceptable if there is a default target.
		// Otherwise, the null target is detected in the director.
		HttpHost target = null;

		final URI requestURI = request.getURI();
		if (requestURI.isAbsolute()) {
			target = URIUtils.extractHost(requestURI);
			if (target == null) {
				throw new ClientProtocolException(
						"URI does not specify a valid host name: " + requestURI);
			}
		}
		return target;
	}

	private void handleFailure(final HttpUriRequest request, final Result result, final Exception ex) {
		try {
			if (ex instanceof InterruptedException) {
				// thats okay
				return;
			} else {
				stats.incKoCount();
                result.setException(ex);
//				pipeline.add(queryRes);

//				if (errorRecorder != null) {
//					errorRecorder.record(uri, ex);
//				}

//				if (logErrorsRate.tryAcquire()) {
//					LOGGER.error("uri => " + ex.getMessage());
//				}
			}
		} catch (Exception ex2) {
			LOGGER.error("request failed exception", ex2);
			throw new RuntimeException(ex2);
		} finally {
			finishRequest();
		}
	}

	private void finishRequest() {
		stats.decRunningCount();
		final int left = concurrent.decrementAndGet();
		if (left == 0) {
			lock.lock();
			try {
				cond.signal();
			} finally {
				lock.unlock();
			}
		}
	}

    private final static RateLimiter logErrorsRate = RateLimiter.create(1.0/2.0);
    private final static Logger LOGGER = LoggerFactory.getLogger(PoutreHttpAsyncClient.class);
}
