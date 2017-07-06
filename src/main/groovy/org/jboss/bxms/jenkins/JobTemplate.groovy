package org.jboss.bxms.jenkins


public class JobTemplate {
    public static void addCommonConfiguration(def job, def dslFactory) {
        job.with {

            // Label which specifies which nodes this job can run on.
            label("pvt-static")
        }
    }
}
