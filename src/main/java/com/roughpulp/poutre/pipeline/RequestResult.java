package com.roughpulp.poutre.pipeline;

public class RequestResult {
	
	public void begin () {
		t0 = System.currentTimeMillis();
	}
	
	public void end () {
		time = System.currentTimeMillis() - t0;
	}
	
	public void setException (final Exception ex) {
		exceptionClass = ex.getClass().getName();
		exceptionMsg = ex.getMessage();
	}

	public long t0 = -1;
	public long time = -1;
	public int statusCode = -1;
	public String uri;
	public String exceptionClass = null;
	public String exceptionMsg = null;
}
