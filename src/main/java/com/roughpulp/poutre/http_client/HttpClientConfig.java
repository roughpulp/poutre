package com.roughpulp.poutre.http_client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpClientConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfig.class);

	public static final Pattern HOST_PATTERN = Pattern.compile("^([^\\:]+)\\:([0-9]+)$");

	public static HttpHost toHttpHost(final String hostColonPort) {
		final Matcher mat = HOST_PATTERN.matcher(hostColonPort);
		if (!mat.find()) {
			throw new IllegalArgumentException("invalid parameter host:port '" + hostColonPort + "'");
		}
		final String hostname = mat.group(1);
		final int port = Integer.parseInt(mat.group(2));
		return new HttpHost(hostname, port);
	}

	public static HttpClientConfig read(final Map configMap) {
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final JsonElement json = gson.toJsonTree(configMap);
		final HttpClientConfig concreteConfig = gson.fromJson(json, HttpClientConfig.class);
		LOGGER.info("read " + HttpClientConfig.class.getSimpleName() + " : " + gson.toJson(concreteConfig));
		return concreteConfig;
	}

	/**
	 */
	public int soTimeout = 10 * 1000;
	/**
	 */
	public int soLinger = 10 * 1000;
	/**
	 */
	public boolean tcpNoDelay = true;
	/**
	 */
	public boolean connReuse = false;
	/**
	 */
	public String proxy = null;
    /**
     */
	public boolean dryRun = false;
    /**
     */
    public int threads = 16;
	/**
	 */
	public long liveStatsPeriodSec = 2;
    /**
     */
    public String errors;
}
