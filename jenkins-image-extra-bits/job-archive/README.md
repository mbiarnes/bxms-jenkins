5/3/2019

This is a copy of the test-osbs pipeline that was used to develop the
Jenkins jobs (osbs-build-7.3.x, osbs-build-master, and osbs-build-indexing)
for running rhba image builds on Jenkins. It's archived here in case the
pipeline accidentally is deleted :)

To restore it, theoretically, take this tarball and unpack it on the jenkins
master under /var/lib/jenkins/jobs. You may have to do something in the
Jenkins console to make it reread the configuration from disk, or restart
the Jenkins master.