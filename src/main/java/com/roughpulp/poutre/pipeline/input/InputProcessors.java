package com.roughpulp.poutre.pipeline.input;

import com.google.common.util.concurrent.RateLimiter;
import com.roughpulp.commons.cursors.Cursor;
import com.roughpulp.commons.cursors.Cursors;
import com.roughpulp.poutre.pipeline.EndOfProcessingException;
import com.roughpulp.poutre.pipeline.Processor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InputProcessors {

	public static <T> Processor<Void, T> constant(final T value) {
		return new Processor<Void, T>() {

			@Override
			public void open() throws Exception {}

			@Override
			public void close() throws Exception {}

			@Override
			public Cursor<T> process(Void request) throws Exception {
				return Cursors.forever(value);
			}
		};
	}

	public static <T> Processor<T, T> timeLimitSec(final long limit) {
		return timeLimit(limit, TimeUnit.SECONDS);
	}

	public static <T> Processor<T, T> timeLimitMin(final long limit) {
		return timeLimit(limit, TimeUnit.MINUTES);
	}

	public static <T> Processor<T, T> timeLimit(final long limit, final TimeUnit unit) {
		final long limitNs = unit.toNanos(limit);
		return new Processor<T, T>() {

			private long t0Ns;

			@Override
			public void open() throws Exception {
				t0Ns = System.nanoTime();
			}

			@Override
			public void close() throws Exception {}

			@Override
			public Cursor<T> process(T request) throws Exception {
				final long dtNs = System.nanoTime() - t0Ns;
				if (dtNs > limitNs || dtNs < 0) {
					return EndOfProcessingException.endOfProcessing();
				} else {
					return Cursors.singleton(request);
				}
			}
		};
	}

	public static <T> Processor<T, T> countLimit(final long limit) {
		return new Processor<T, T>() {

			private long count;

			@Override
			public void open() throws Exception {
				count = 0;
			}

			@Override
			public void close() throws Exception {}

			@Override
			public Cursor<T> process(T request) throws Exception {
				if (count >= limit) {
					return EndOfProcessingException.endOfProcessing();
				} else {
					count += 1;
					return Cursors.singleton(request);
				}
			}
		};
	}

	public static <T> Processor<T, T> rateLimit(final double rate) {
		final RateLimiter rateLimiter = RateLimiter.create(rate);
		return new Processor<T, T>() {

			@Override
			public void open() throws Exception {}

			@Override
			public void close() throws Exception {}

			@Override
			public Cursor<T> process(T request) throws Exception {
				rateLimiter.acquire();
				return Cursors.singleton(request);
			}
		};
	}

	public static Processor<Void, String> uriFileSource (final File sourceFile, final boolean loop) {
		return new UriFileSource(sourceFile, loop);
	}

	public static Processor<Object, HttpUriRequest> fromStringToHttpRequest() {
		return new Processor<Object, HttpUriRequest>() {
			@Override
			public void open() throws Exception {}

			@Override
			public void close() throws Exception {}

			@Override
			public Cursor<HttpUriRequest> process(Object uri) throws Exception {
				final HttpUriRequest request = new HttpGet(uri.toString());
				return Cursors.singleton(request);
			}
		};
	}

	public static Processor<Map, HttpUriRequest> fromMapToHttpRequest() {
		return new FromMapToHttpRequest();
	}
}
