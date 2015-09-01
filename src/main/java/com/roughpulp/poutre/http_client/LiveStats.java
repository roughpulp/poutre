package com.roughpulp.poutre.http_client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LiveStats implements AutoCloseable {

	private final static Logger LOGGER = LoggerFactory.getLogger("");

	private final long period;
	private final TimeUnit periodUnit;
	private final MetricRegistry metrics = new MetricRegistry();
	private final Counter queriesCount;
	private final Counter runningCount;
	private final Counter okCount;
	private final Counter koCount;
	private final Histogram qtimes;

	//
	private final Lock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();
	private boolean alive = false;
	private Thread daemon = null;
	//
	private long prevCount;
	private long prevTime;

	public LiveStats(final long period, final TimeUnit periodUnit) {
		this.period = period;
		this.periodUnit = periodUnit;
		queriesCount = metrics.counter("queries");
		runningCount = metrics.counter("running");
		okCount = metrics.counter("ok");
		koCount = metrics.counter("ko");
		qtimes = new Histogram(new SlidingTimeWindowReservoir(period, periodUnit));
//		qtimes = new Histogram(new ExponentiallyDecayingReservoir());
	}

	@Override
	public void close () {
		final Thread toJoin;
		lock.lock();
		try {
			if (daemon == null) {
				return;
			}
			alive = false;
			cond.signal();
			toJoin = daemon;
			daemon = null;
		} finally {
			lock.unlock();
		}
		try {
			toJoin.join();
		} catch (InterruptedException ex) {
			// that's okay
		}
	}

	public void incQueriesCount () { queriesCount.inc(); }

	public void incRunningCount () { runningCount.inc(); }

	public void decRunningCount () { runningCount.dec(); }

	public void incOkCount () { okCount.inc(); }

	public void incKoCount () { koCount.inc(); }

	public void pushQTime (final long qtime) { qtimes.update(qtime); }

	public void start () {
		lock.lock();
		try {
			if (daemon != null) {
				throw new IllegalStateException("already started");
			}
			alive = true;
			daemon = new Thread(new Runnable() {
				@Override
				public void run() {
					loop();
				}
			}, "liveStats");
			daemon.setDaemon(true);
			daemon.start();
		} finally {
			lock.unlock();
		}
	}

	private void loop()  {
		lock.lock();
		try {
			prevCount = 0;
			prevTime = System.currentTimeMillis();

			while (alive) {
				cond.await(period, periodUnit);
				if (alive) {
					log("");
				}
			}
			log("DONE ");

		} catch (final InterruptedException ex) {
			// thats okay
		} finally {
			lock.unlock();
		}
	}

	private void log (final String header) {
		final long now = System.currentTimeMillis();
		final long count = queriesCount.getCount();
		final long dcount = count - prevCount;
		final long dt = now - prevTime;
		prevCount = count;
		prevTime = now;
		final double qps = ((double) dcount) / ((double) dt) * 1000.0;

		final Snapshot qts = qtimes.getSnapshot();

		LOGGER.info(header +
				"total=" + count +
				", running=" + runningCount.getCount() +
				", ok=" + okCount.getCount() +
				", errors=" + koCount.getCount() +
				", qps=" + round(qps) + ", " +
				"qtimes mean=" + round(qts.getMean()) + " min=" + round(qts.getMin()) + " 50%=" + round(qts.getMedian()) + " 75%=" + round(qts.get75thPercentile()) + " 95%=" + round(qts.get95thPercentile()) + " 99%=" + round(qts.get99thPercentile()) + " max=" + round(qts.getMax())
		);
	}

	private static long round (final double val) {
		return Math.round(val);
	}

}
