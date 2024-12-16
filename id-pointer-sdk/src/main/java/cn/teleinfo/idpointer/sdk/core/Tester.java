/**********************************************************************\
 © COPYRIGHT 2019 Corporation for National Research Initiatives (CNRI);
                        All rights reserved.

        The HANDLE.NET software is made available subject to the
      Handle.Net Public License Agreement, which may be obtained at
          http://hdl.handle.net/20.1000/112 or hdl:20.1000/112
\**********************************************************************/

package cn.teleinfo.idpointer.sdk.core;


import cn.teleinfo.idpointer.sdk.core.sample.SiteInfoConverter;

import java.io.File;
import java.io.FileInputStream;

public class Tester {

    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Usage:  java Tester <handle>" + " <#queries/thread> <#threads> <site_info_file>");
            return;
        }

        String handle = argv[0];
        int queries = (argv.length > 1) ? Integer.parseInt(argv[1]) : 1000;
        int threads = (argv.length > 2) ? Integer.parseInt(argv[2]) : 20;
        File siteInfoFile = (argv.length > 3) ? new File(argv[3]) : null;

        HandleResolver resolver = new HandleResolver();
        Tester tester = new Tester(resolver);

        System.err.println("Site info file: " + siteInfoFile);
        SiteInfo sites[];
        if (siteInfoFile != null) {
            byte buf[] = new byte[(int) siteInfoFile.length()];
            int n = 0;
            int r = 0;
            FileInputStream fin = new FileInputStream(siteInfoFile);
            try {
                while (n < buf.length && (r = fin.read(buf, n, buf.length - n)) >= 0)
                    n += r;
            } finally {
                fin.close();
            }
            SiteInfo site = new SiteInfo();
            if (Util.looksLikeBinary(buf)) {
                Encoder.decodeSiteInfoRecord(buf, 0, site);
            } else {
                site = SiteInfoConverter.convertToSiteInfo(new String(buf, "UTF-8"));
            }
            sites = new SiteInfo[] { site };
            resolver.setCache(null);
            resolver.setCertifiedCache(null);
        } else {
            sites = resolver.findLocalSites(new ResolutionIdRequest(Util.encodeString(handle), null, null, null));
        }
        resolver.getConfiguration().setCacheSites(sites);
        resolver.getConfiguration().setResolutionMethod(Configuration.RM_WITH_CACHE);

        numberOfTestThreads = threads;
        tester.doTests(handle, queries, threads, sites);
    }

    private final HandleResolver resolver;

    public Tester(HandleResolver resolver) {
        this.resolver = resolver;
    }

    public void doTests(String handle, int queries, int threads, SiteInfo sites[]) throws Exception {
        //resolver.traceMessages = true;

        ResolutionIdRequest req = new ResolutionIdRequest(Util.encodeString(handle), null, null, null);
        req.ignoreRestrictedValues = true;
        //req.certify = true;

        AbstractIdResponse response = resolver.processRequest(req);
        if (response instanceof ResolutionIdResponse) {
            System.err.println(" number of values: " + ((ResolutionIdResponse) response).values.length);
        } else {
            System.err.println(" aborting tests");
            return;
        }

        java.util.Vector<SubTester> testers = new java.util.Vector<>();
        java.util.Vector<Thread> threadV = new java.util.Vector<>();

        System.err.println("\nQuerying " + handle + " with " + threads + " threads, each performing " + queries + " queries");

        resolver.traceMessages = false;

        for (int i = 0; i < threads; i++) {
            SubTester t = new SubTester(resolver, handle, queries, i, threads, sites);
            testers.addElement(t);
            Thread th = new Thread(t);
            threadV.addElement(th);
            testerCount++;
        }

        startTime = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            threadV.elementAt(i).start();
        }
    }

    private static int testerCount = 0;
    static int numberOfTestThreads = 0;
    private long startTime;
    private long endTime;
    private int successes = 0;
    private int failures = 0;

    class SubTester implements Runnable {
        private final ResolutionIdRequest req;
        @SuppressWarnings("hiding")
        private final HandleResolver resolver;
        private final SiteInfo sites[];
        private final int threadNum;
        private final int queries;
        private final int numThreads;

        public SubTester(HandleResolver resolver, String handle, int queries, int threadNum, int numThreads, SiteInfo[] sites) throws Exception {
            this.resolver = resolver;
            this.queries = queries;
            this.threadNum = threadNum;
            this.numThreads = numThreads;
            req = new ResolutionIdRequest(Util.encodeString(handle), null, null, null);
            this.sites = sites; //resolver.findLocalSites(req);
        }

        @Override
        public void run() {
            for (int i = 0; i < queries; i++) {
                try {
                    AbstractIdResponse resp = resolver.sendRequestToService(req, sites);
                    //AbstractResponse resp = resolver.processRequest(req);
                    if (resp.getClass() == ResolutionIdResponse.class) {
                        successes++;
                    } else {
                        System.err.println("Error: " + threadNum + ':' + i + ": got unexpected response: " + resp);
                        failures++;
                    }
                } catch (Exception e) {
                    failures++;
                    System.err.println("Error: " + threadNum + ':' + i + ": " + e);
                }
            }
            boolean showResults = testerCount == numberOfTestThreads;
            testerCount--;
            if (showResults) {
                endTime = System.currentTimeMillis();
                long time = endTime - startTime;
                // do the clean-up...
                System.err.println(
                    "  milliseconds: " + time + "\n" + "  seconds: " + (time / 1000) + "\n" + "  req/second: " + (((numThreads * queries) / (double) time) * 1000) + "\n" + "  successes: " + successes + "\n" + "  failures: " + failures);
            }
        }
    }

    private static HandleValue valuesToCreate[] = { new HandleValue(1, Common.STD_TYPE_URL, Util.encodeString("http://handle.net/")), new HandleValue(2, Common.STD_TYPE_EMAIL, Util.encodeString("hdladmin@cnri.reston.va.us")),
            new HandleValue(3, Common.STD_TYPE_HSADMIN, Encoder.encodeAdminRecord(new AdminRecord(Util.encodeString("200/0"), 300, true, true, true, true, true, true, true, true, true, true, true, true))), };

    class CreateTester implements Runnable {
        private final CreateHandleIdRequest req;
        @SuppressWarnings("hiding")
        private final HandleResolver resolver;
        private final SiteInfo sites[];
        private final int threadNum;
        private final int queries;
        private final int numThreads;

        public CreateTester(HandleResolver resolver, AuthenticationInfo auth, String handle, int queries, int threadNum, int numThreads) throws Exception {
            this.resolver = resolver;
            this.queries = queries;
            this.threadNum = threadNum;
            this.numThreads = numThreads;
            req = new CreateHandleIdRequest(Util.encodeString(handle), valuesToCreate, auth);
            this.sites = resolver.findLocalSites(req);
        }

        @Override
        public void run() {
            for (int i = 0; i < queries; i++) {
                try {
                    AbstractIdResponse resp = resolver.sendRequestToService(req, sites);
                    //AbstractResponse resp = resolver.processRequest(req);
                    if (resp.getClass() == ResolutionIdResponse.class) {
                        successes++;
                    } else {
                        System.err.println("Error: " + threadNum + ':' + i + ": got unexpected response: " + resp);
                        failures++;
                    }
                } catch (Exception e) {
                    failures++;
                    System.err.println("Error: " + threadNum + ':' + i + ": " + e);
                }
            }
            testerCount--;
            if (testerCount == 0) {
                endTime = System.currentTimeMillis();
                long time = endTime - startTime;
                // do the clean-up...
                System.err.println(
                    "  milliseconds: " + time + "\n" + "  seconds: " + (time / 1000) + "\n" + "  req/second: " + (((numThreads * queries) / (double) time) * 1000) + "\n" + "  successes: " + successes + "\n" + "  failures: " + failures);
            }
        }
    }

}
