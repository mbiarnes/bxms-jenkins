// Creates or updates a free style job.
job("sample-init-release") {

    // Sets a description for the job.
    description("This is release initialization job. This job is responsible for preparation of brms-64-jenkins-ci.properties file.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }

    // Adds build triggers to the job.
    triggers {

        // Triggers build using remote build message.
        ciBuildTrigger {

            // The name of the Message Provider that was configured in the global settings.
            selector('CI_TYPE=\'brms-64-releaseci-brew-trigger\'')
            // JMS selector to choose messages that will fire the trigger.
            providerName("default")

        }
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
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell('integration-platform-tooling/utility/init-releaseci-properties.sh integration-platform-config/brms-64.cfg')
    }
}