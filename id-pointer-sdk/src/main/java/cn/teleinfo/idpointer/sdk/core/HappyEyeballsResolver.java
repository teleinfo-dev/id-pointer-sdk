/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class HappyEyeballsResolver implements Runnable {
    // Multithreaded requests for "Happy Eyeballs"
    private final HandleResolverInterface resolver;
    private final SiteInfo sites[];
    public final AbstractIdRequest req;
    private final ResponseMessageCallback callback;
    private final int primaries;
    private final SiteInfo preferredPrimary;
    private final int delayMillis;
    private final CountDownLatch waitLatch;
    private final CountDownLatch siblingPreferredPrimaryLatch;

    private volatile Thread hostThread = null;
    private volatile boolean interrupted;

    public AbstractIdResponse resp = null;
    public HandleException publicException = null;
    public HappyEyeballsResolver siblingResolver = null;

    public HappyEyeballsResolver(HandleResolverInterface resolver, SiteInfo sites[], AbstractIdRequest req, ResponseMessageCallback callback,
                                 int primaries, SiteInfo preferredPrimary, int delayMillis, boolean mustWaitForSiblingToProcessPreferredPrimary) {
        this.resolver = resolver;
        this.sites = sites;
        this.req = req;
        this.callback = callback;
        this.primaries = primaries;
        this.preferredPrimary = preferredPrimary;
        this.delayMillis = delayMillis;
        if (delayMillis > 0) waitLatch = new CountDownLatch(1);
        else waitLatch = null;
        if (mustWaitForSiblingToProcessPreferredPrimary) siblingPreferredPrimaryLatch = new CountDownLatch(1);
        else siblingPreferredPrimaryLatch = null;
    }

    public HappyEyeballsResolver() {
        this.resolver = null;
        this.sites = null;
        this.req = null;
        this.callback = null;
        this.primaries = 0;
        this.preferredPrimary = null;
        this.delayMillis = 0;
        this.waitLatch = null;
        this.siblingPreferredPrimaryLatch = null;
    }

    @Override
    public void run() {
        try {
            if (sites.length == 0) {
                return;
            }

            hostThread = Thread.currentThread();

            if (interrupted) return;

            if (siblingPreferredPrimaryLatch != null) {
                try {
                    siblingPreferredPrimaryLatch.await();
                } catch (InterruptedException e) {
                    // Interrupted by the other thread means we're done.
                    return;
                }
            }
            if (interrupted || req.completed.get()) return;

            if (delayMillis > 0) {
                try {
                    waitLatch.await(delayMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // Interrupted by the other thread means we're done.
                    return;
                }
            }
            if (interrupted || req.completed.get()) return;

            sendRequestAndSetResponseOrPublicException();

            if (resp != null && !interrupted) {
                if (siblingResolver != null) stopSibling();
            }
        } finally {
            if (siblingResolver != null) siblingResolver.stopWaiting();
            // Clear the interrupt flag from this thread if there is one.
            // This is needed because this runnable may be run in a thread that goes on to do other work.
            // We are assuming that the only possible interrupt comes from the sibling.
            synchronized (this) {
                hostThread = null;
            }
            Thread.interrupted();
            while (req.connectionLock.isHeldByCurrentThread()) req.connectionLock.unlock();
            if (interrupted) {
                resp = null;
                publicException = null;
                req.completed.set(false);
            }
        }
    }

    private void stopWaiting() {
        siblingPreferredPrimaryDone();
        if (waitLatch != null) waitLatch.countDown();
    }

    private void siblingPreferredPrimaryDone() {
        if (siblingPreferredPrimaryLatch != null) siblingPreferredPrimaryLatch.countDown();
    }

    private synchronized void stop() {
        interrupted = true;
        if (hostThread != null) {
            hostThread.interrupt();
        }
        Socket socket = req.socketRef.get();
        if (socket != null) {
            try { socket.close(); } catch(Exception e) {}
            req.socketRef.set(null);
        }
    }

    private void stopSibling() {
        req.connectionLock.lock();
        try {
            if (!interrupted) {
                req.completed.set(true);
                siblingResolver.stop();
            }
        } finally {
            req.connectionLock.unlock();
        }
    }

    private void sendRequestAndSetResponseOrPublicException() {
        sendRequestToPreferredPrimary();
        if (resp == null && preferredPrimary != null && siblingResolver != null) siblingResolver.siblingPreferredPrimaryDone();
        if (resp == null && !interrupted) sendRequestToSites();
    }

    private void sendRequestToPreferredPrimary() {
        if (preferredPrimary != null && siblingPreferredPrimaryLatch == null) {
            int[] preferredProtocols = resolver.getPreferredProtocols();
            for (int p = 0; p < preferredProtocols.length; p++) {
                sendRequestToSiteViaProtocol(preferredPrimary, p);
                if (resp != null) return;
            }
        }
    }

    private void sendRequestToSites() {
        int[] preferredProtocols = resolver.getPreferredProtocols();
        for (int p = 0; p < preferredProtocols.length; p++) {
            for (int i = 0; i < sites.length; i++) {
                SiteInfo site = sites[i];
                if (site.servers == null || site.servers.length == 0) {
                    continue;
                }
                if (!site.isPrimary && (req.isAdminRequest || req.authoritative)) {
                    continue;
                }
                if (site == preferredPrimary) continue; // Already tried it.
                if (interrupted) return;
                sendRequestToSiteViaProtocol(site, p);
                if (resp != null) {
                    adjustResponseTimesOfUntriedSites(i);
                    return;
                }
            }
        }
    }

    private void adjustResponseTimesOfUntriedSites(int indexOfSiteMostRecentlyUsed) {
        // adjust response times so that slower servers eventually get re-tried
        // a site twice as slow will be retried every 128 resolutions
        long usedSiteResponseTime = sites[indexOfSiteMostRecentlyUsed].responseTime;
        long adjustment = usedSiteResponseTime / 128;
        for (int i = indexOfSiteMostRecentlyUsed + 1; i < sites.length; i++) {
            if (sites[i] == preferredPrimary) continue;
            if (sites[i].servers.length == 0) continue;
            long thisSiteResponseTime = sites[i].responseTime;
            if (thisSiteResponseTime <= usedSiteResponseTime) continue;
            else if (thisSiteResponseTime <= usedSiteResponseTime + 256 || thisSiteResponseTime <= usedSiteResponseTime * 2) {
                if (adjustment == 0) continue;
                else resolver.getResponseTimeTbl().put(sites[i].servers[0].getAddressString(), sites[i].responseTime - adjustment);
            } else {
                // for really long entries, like timeouts, get to within 256 in about 400 queries
                long thisAdjustment = (thisSiteResponseTime - usedSiteResponseTime) / 64;
                if (thisAdjustment < adjustment) thisAdjustment = adjustment;
                resolver.getResponseTimeTbl().put(sites[i].servers[0].getAddressString(), sites[i].responseTime - thisAdjustment);
            }
        }
    }

    private void sendRequestToSiteViaProtocol(SiteInfo site, int p) {
        try {
            resp = resolver.sendRequestToSite(req, site, resolver.getPreferredProtocols()[p], callback);

            if (resp != null) {
                setPreferredPrimaryStatus(site);
                publicException = null;
            }
        } catch (HandleException e) {
            if (!interrupted) publicException = e;
        } finally {
            if (resp==null) while (req.connectionLock.isHeldByCurrentThread()) req.connectionLock.unlock();
        }
    }

    private void setPreferredPrimaryStatus(SiteInfo site) {
        if ((req.isAdminRequest || req.authoritative) && (preferredPrimary != null || (primaries > 1)) && (site.servers != null && site.servers.length > 0)) {
            long now = System.currentTimeMillis();
            String ip = site.servers[0].getAddressString();
            String ipPort = ip;
            if (site.servers[0].interfaces != null && site.servers[0].interfaces.length > 0) {
                ipPort = ip + ":" + site.servers[0].interfaces[0].port;
            }
            resolver.getPreferredPrimaryTbl().put(ipPort, now);
        }
    }

}
