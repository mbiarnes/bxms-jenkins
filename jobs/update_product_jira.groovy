// Update product jira script
String shellScript = '''echo "The product version is $product_version and the release milestone is $release_milestone."
python ./integration-platform-tooling/release-ticketor.py --headless $product_version.GA $cutoff_date $product_version.$release_milestone 2>&1 | tee /tmp/release-ticketor-output
sed -i '/^resolve_issue_list=/d' ${HOME}/brms-64-jenkins-ci.properties
echo "resolve_issue_list="`cat /tmp/release-ticketor-output | grep https://url.corp.redhat.com` >>${HOME}/brms-64-jenkins-ci.properties
'''

// Creates or updates a free style job.
job("sample-update-product-jira") {

    // Sets a description for the job.
    description("This job is responsible for update product jira.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds environment variables to the build.
    environmentVariables {

        // Adds environment variables from a properties file.
        propertiesFile('${HOME}/brms-64-jenkins-ci.properties')

        // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
        keepBuildVariables(true)

        // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
        keepSystemVariables(true)
    }

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

        // Adds timestamps to the console log.
        timestamps()

        // Add a timeout to the build job. Defaults to a absolute timeout with a maximum build time of 3 minutes.
        timeout {

            // Aborts the build based on a fixed time-out
            absolute(minutes = 3)

            // Aborts the build.
            abortBuild()

            // Marked the build as failed
            failBuild()
        }
    }

    // Allows to parameterize the job.
    parameters {

        // Defines a simple boolean parameter.
        booleanParam(parameterName = "CLEAN_CARTOGRAPHER_CACHE", defaultValue = false,
                description = "Tick if you want to wipe local Cartographer cache to send over new requests.")

        // Defines a simple boolean parameter.
        booleanParam(parameterName = "DELETE_CARTOGRAPHER_WORKSPACE", defaultValue = false,
                description = "Tick if you want to wipe remote Cartographer workspace containing any resolved dependency graph.")

        // Defines a simple boolean parameter.
        booleanParam(parameterName = "GEN_REPORT", defaultValue = true,
                description = "Tick if you want to generate report for the newly created repository.")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "task_id", defaultValue = null,
                description = "Brew task id")
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
        shell(shellScript)
    }
}