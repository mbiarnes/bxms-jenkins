// Creates or updates a free style job.
job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-create-handover-jira") {

    // Sets a description for the job.
    description("This job is responsible for create/update jira issue after creation of handover.")

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
        shell('echo "Do Jira ticket.')
    }
}