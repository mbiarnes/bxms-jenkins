import org.jboss.bxms.jenkins.JobTemplate

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-init-release") {

    // Sets a description for the job.
    description("This is the ${PRODUCT_NAME} release initialization job. This job is responsible for preparation of ${CI_PROPERTIES_FILE} file.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell("ip-tooling/utility/init-releaseci-properties.sh ${IP_CONFIG_FILE}")
    }
}

JobTemplate.addIpToolingScmConfiguration(jobDefinition)