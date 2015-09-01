package com.roughpulp.poutre.http_client;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.GZIPOutputStream;

/**
 */
public class ErrorRecorder implements AutoCloseable{

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorRecorder.class);

	private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1024);
	private final Writer out;
	private final static String EOS = "EOS";
	private Thread daemon;

	public ErrorRecorder(final File dst) throws IOException {
		out = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(dst), 4 * 1024)), Charsets.UTF_8);
	}

	@Override
	public void close() throws Exception {
		if (daemon != null) {
			queue.put(EOS);
			daemon.join();
			daemon = null;
		}
	}

	public void start () {
		daemon = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					drain();
				} catch (InterruptedException ex) {
					//that's okay
				} catch (Exception ex) {
					LOGGER.error("error recorder exception", ex);
				}
			}
		}, "error-recorder");
		daemon.start();
	}

	public void record(final HttpUriRequest request, final HttpResponse response) throws InterruptedException, IOException {
		final StringWriter sb = new StringWriter();
		sb.append("-------------\n").append(request.toString()).append("\n");
		sb.append(response.getStatusLine().toString()).append("\n");
		final HttpEntity entity = response.getEntity();
		if (entity != null) {
			final InputStream in = entity.getContent();
			try {
				IOUtils.copy(in, sb, Charsets.UTF_8);
				sb.append("\n");
			} finally {
				in.close();
			}
		}
		// do not block rather drop the entry
		queue.offer(sb.toString());
	}

	public void record(final HttpUriRequest request, Exception ex) throws InterruptedException, IOException {
		final StringBuilder sb = new StringBuilder();
		sb.append("-------------\n").append(request.toString()).append("\n");
		sb.append(ex.getMessage()).append("\n");
		// do not block rather drop the entry
		queue.offer(sb.toString());
	}

	private void drain() throws InterruptedException, IOException {
		for (;;) {
			final String error = queue.take();
			if (error == EOS) {
				out.close();
				return;
			} else {
				out.write(error);
			}
		}
	}
}
