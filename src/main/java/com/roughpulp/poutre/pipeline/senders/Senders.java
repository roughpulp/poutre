package com.roughpulp.poutre.pipeline.senders;

import com.roughpulp.poutre.http_client.HttpClientConfig;

import java.util.Map;

public class Senders {

	public static SyncFixedThreadsSender syncFixedThreads(final Map config) {
		return syncFixedThreads(HttpClientConfig.read(config));
	}

	public static SyncFixedThreadsSender syncFixedThreads(final HttpClientConfig config) {
		return new SyncFixedThreadsSender(config);
	}

	// -----------

	public static SyncVariableThreadsSender syncVariableThreads(final Map config) {
		return syncVariableThreads(HttpClientConfig.read(config));
	}

	public static SyncVariableThreadsSender syncVariableThreads(final HttpClientConfig config) {
		return new SyncVariableThreadsSender(config);
	}

	// -----------

	public static AsyncSender async(final Map config) {
		return async(HttpClientConfig.read(config));
	}

	public static AsyncSender async(final HttpClientConfig config) {
		return new AsyncSender(config);
	}
}
