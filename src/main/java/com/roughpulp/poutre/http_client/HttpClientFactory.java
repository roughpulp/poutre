package com.roughpulp.poutre.http_client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class HttpClientFactory {

	public static CloseableHttpClient newHttpSyncClient (final HttpClientConfig conf) {
		final SocketConfig socketConfig = SocketConfig.custom()
				.setSoTimeout(conf.soTimeout)
				.setTcpNoDelay(conf.tcpNoDelay)
				.setSoLinger(conf.soLinger)
				.build();
		final HttpClientBuilder builder = HttpClientBuilder.create()
				.setRetryHandler(new HttpRequestRetryHandler() {
					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						return false;
					}
				})
				.setConnectionReuseStrategy(conf.connReuse ? new DefaultConnectionReuseStrategy() : new NoConnectionReuseStrategy())
				.setMaxConnPerRoute(1000 * 1000)
				.setMaxConnTotal(1000 * 1000)
				.setDefaultSocketConfig(socketConfig)
				.setUserAgent("poutre");

		//FIXME: proxy
		/*
		if (conf.proxy != null) {
			builder.setProxy(Config.toHttpHost(conf.proxy));
		}
		 */

		return builder.build();
	}

	public static CloseableHttpAsyncClient newHttpAsyncClient (final HttpClientConfig conf) throws IOReactorException {
		final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
				.setIoThreadCount(conf.threads)
				.setSoLinger(conf.soLinger)
				.setConnectTimeout(conf.soTimeout)
				.setSoTimeout(conf.soTimeout)
				.setTcpNoDelay(conf.tcpNoDelay)
				.build();
		final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
		final PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);
		connManager.setMaxTotal(1000 * 1000);
		connManager.setDefaultMaxPerRoute(1000 * 1000);
		final HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create()
				.setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("async-http-%d").build())
				.setMaxConnPerRoute(1000 * 1000)
				.setMaxConnTotal(1000 * 1000)
				.setConnectionReuseStrategy(conf.connReuse ? new DefaultConnectionReuseStrategy() : new NoConnectionReuseStrategy())
				.setUserAgent("poutre")
				.setConnectionManager(connManager);
		//FIXME: proxy
		/*
		if (conf.proxy != null) {
			builder.setProxy(Config.toHttpHost(conf.proxy));
		}
		*/
		return builder.build();
	}
}
