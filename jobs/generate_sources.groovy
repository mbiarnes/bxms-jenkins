import org.jboss.bxms.jenkins.JobTemplate

shellScript = """
unset WORKSPACE
make CFG=brms.cfg SOURCES=1 SRCDIR=src -f Makefile.BRMS kie-wb-distributions kie-docs droolsjbpm-integration
make CFG=common.cfg SOURCES=1 SRCDIR=src -f Makefile.COMMON mvel-2.3.0 xmlpull-1.1.4
make CFG=ip-bom.cfg SOURCES=1 SRCDIR=src -f Makefile.IPBOM jboss-integration-platform-bom

zip -r sources.zip
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-generate-sources") {
    // Sets a description for the job.
    description("This job is responsible for generating product sources.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    // Adds post-build actions to the job.
    publishers {
        //Archives artifacts with each build.
        archiveArtifacts('workspace/sources.zip')
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
