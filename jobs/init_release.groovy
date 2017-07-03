// Creates or updates a free style job.
job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-init-release") {

    // Sets a description for the job.
    description("This is the ${PRODUCT_NAME} release initialization job. This job is responsible for preparation of ${CI_PROPERTIES_FILE} file.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }

    // Allows a job to check out sources from multiple SCM providers.
    multiscm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch("master")
        }

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch("master")

            // Adds additional behaviors.
            extensions {

                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                relativeTargetDirectory('ip-tooling')
            }
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell("ip-tooling/utility/init-releaseci-properties.sh ${IP_CONFIG_FILE}")
    }
}