This is a copy of the test-osbs pipeline that was used to develop the
Jenkins job (osbs-download) for running rhba image builds on Jenkins.
It's archived here in case the pipeline accidentally is deleted :)

The job is so-named because it is the final version that downloads
both build-overrides.sh and build-osbs.sh from jboss-kie-modules
based on the branch being built.

To restore it, theoretically, take this tarball and unpack it on the jenkins
master under /var/lib/jenkins/jobs. You may have to do something in the
Jenkins console to make it reread the configuration from disk, or restart
the Jenkins master.
