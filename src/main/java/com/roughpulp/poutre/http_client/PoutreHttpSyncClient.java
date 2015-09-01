package com.roughpulp.poutre.http_client;

import com.google.common.util.concurrent.RateLimiter;
import com.roughpulp.poutre.pipeline.Result;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PoutreHttpSyncClient implements AutoCloseable{

	private final CloseableHttpClient client;
	private final LiveStats stats;
	private final ErrorRecorder errorRecorder;
	private final boolean dryRun;

	public PoutreHttpSyncClient(
			final HttpClientConfig conf,
            final LiveStats stats,
			final ErrorRecorder errorRecorder
            ) throws IOException {
		this.client = HttpClientFactory.newHttpSyncClient(conf);
		this.stats = stats;
		this.errorRecorder = errorRecorder;
		this.dryRun = conf.dryRun;
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

	public void request (final HttpUriRequest request) throws IOException, InterruptedException {
		if (dryRun) {
			LOGGER.info(request.toString());
			return;
		}
		final Result result = new Result();
//		requestResult.uri  = recordUris ? uri : "N/A";
		result.begin();
		CloseableHttpResponse resp = null;
		try {
			stats.incQueriesCount();
			stats.incRunningCount();
			resp = client.execute(request);
			result.end();
			result.statusCode = resp.getStatusLine().getStatusCode();
			if (result.statusCode == 200) {
				stats.incOkCount();
                stats.pushQTime(result.time);
			} else {
				stats.incKoCount();
				if (errorRecorder != null) {
					errorRecorder.record(request, resp);
				}
			}
//			respHandler.handle(requestResult, resp);
//			pipeline.add(requestResult);

		} catch (final Exception ex) {
			if (ex instanceof InterruptedException) {
				// that's okay
				return;
			} else {
				stats.incKoCount();
				result.setException(ex);
//				pipeline.add(requestResult);

				if (errorRecorder != null) {
					errorRecorder.record(request, ex);
				}

				if (logErrorsRate.tryAcquire()) {
					LOGGER.error("uri => " + ex.getMessage());
				}
			}

		} finally {
			stats.decRunningCount();
			if (resp != null) {
				try {
					resp.close();
				} catch (final IOException ex) {
					LOGGER.error("couldn't close response: ", ex);
				}
			}
		}
	}

	private final static RateLimiter logErrorsRate = RateLimiter.create(1.0/2.0);
	private final static Logger LOGGER = LoggerFactory.getLogger(PoutreHttpSyncClient.class);
}
