package com.roughpulp.poutre.jmx;

import com.google.common.util.concurrent.RateLimiter;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Knobs implements KnobsMBean {

    public interface QpsUpdateListener {
        void onUpdate (double qps);
    }

    public static Knobs create () throws Exception {
        final Knobs knobs = new Knobs();

        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = new ObjectName(Knobs.class.getName() + ":type=Knobs");
        mbeanServer.registerMBean(knobs, name);

        return knobs;
    }

    private Knobs () {}

    private final ReentrantLock lock = new ReentrantLock();
    private volatile double qps;
    private final ArrayList<QpsUpdateListener> qpsListeners = new ArrayList<QpsUpdateListener>();

    public RateLimiter createQpsLimiter (final double permitsPerSecond) {
        final RateLimiter qpsLimiter = RateLimiter.create(permitsPerSecond);
        addQpsUpdateListener(new QpsUpdateListener() {
            @Override
            public void onUpdate(double qps) { qpsLimiter.setRate(qps); }
        });
        return qpsLimiter;
    }

    void addQpsUpdateListener (QpsUpdateListener listener) {
        lock.lock();
        try {
            qpsListeners.add(listener);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setQps (double qps) {
        lock.lock();
        try {
            this.qps = qps;
            for (final QpsUpdateListener listener : qpsListeners) {
                listener.onUpdate(qps);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public double getQps () { return qps; }
}
