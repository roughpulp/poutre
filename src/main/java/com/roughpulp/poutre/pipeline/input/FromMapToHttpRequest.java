package com.roughpulp.poutre.pipeline.input;

import com.roughpulp.commons.cursors.Cursor;
import com.roughpulp.commons.cursors.Cursors;
import com.roughpulp.poutre.pipeline.Processor;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import java.util.Locale;
import java.util.Map;

public class FromMapToHttpRequest implements Processor<Map,HttpUriRequest> {

	@Override
	public void open() throws Exception {}

	@Override
	public void close() throws Exception {}

	@Override
	public Cursor<HttpUriRequest> process(Map requestMap) throws Exception {

		final String method;
		{
			final Object uMethod = requestMap.get("method");
			if (uMethod == null) {
				method = "GET";
			} else {
				method = uMethod.toString().toUpperCase(Locale.ROOT);
			}
		}
		final String uri;
		{
			final Object uUri = requestMap.get("uri");
			if (uUri == null) {
				throw new IllegalArgumentException("missing uri");
			} else {
				uri = uUri.toString();
			}
		}

		HttpEntityEnclosingRequestBase entityRequest = null;
		HttpUriRequest uriRequest = null;
		switch (method) {
			case "OPTIONS":
				uriRequest = new HttpOptions(uri);
				break;
			case "GET":
				uriRequest = new HttpGet(uri);
				break;
			case "HEAD":
				uriRequest = new HttpHead(uri);
				break;
			case "POST": {
				final HttpPost post = new HttpPost(uri);
				uriRequest = post;
				entityRequest = post;
				break;
			}
			case "PUT": {
				final HttpPut post = new HttpPut(uri);
				uriRequest = post;
				entityRequest = post;
				break;
			}
			case "DELETE":
				uriRequest = new HttpDelete(uri);
				break;
			case "TRACE":
				uriRequest = new HttpTrace(uri);
				break;
			default:
				throw new IllegalArgumentException("unexpected HTTP method: " + method);
		}

		// populate the headers:
		{
			final Object uHeaders = requestMap.get("headers");
			if (! (uHeaders instanceof Map)) {
				throw new IllegalArgumentException("headers not a map");
			}
			final Map<Object, Object> headers = (Map<Object, Object>) uHeaders;
			for (final Map.Entry<Object, Object> entry : headers.entrySet()) {
				final String header = entry.getKey().toString();
				final Object uValues = entry.getValue();
				if (uValues instanceof Iterable) {
					final Iterable values = (Iterable) uValues;
					for (final Object value : values) {
						uriRequest.addHeader(header, value.toString());
					}

				} else if (uValues instanceof String) {
					uriRequest.addHeader(header, uValues.toString());

				} else {
					throw new IllegalArgumentException("unexpected header values type. header: " + header + ", values type: " + uValues.getClass());
				}
			}
		}

		// populate entity
		if (entityRequest != null) {
			final Object uEntity = requestMap.get("entity");
			if (uEntity != null) {
				if (uEntity instanceof String) {
					entityRequest.setEntity(new StringEntity(uEntity.toString()));
				} else {
					throw new IllegalArgumentException("unexpected entity type: " + uEntity.getClass());
				}
			}
		}

		return Cursors.singleton(uriRequest);
	}

}
