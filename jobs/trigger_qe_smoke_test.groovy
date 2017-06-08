// QE smoke test trigger script
String shellScript = '''#Publish a CI Message to trigger the QE smoketest
#echo "Send CI message CI_TYPE='${release_prefix}-qe-smoketest-trigger"
deliverable_list=${rcm_stage_base}/${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/${release_prefix}-deliverable-list-staging.properties
echo ${deliverable_list}
#utility/publish.py --user ci-ops-central-jenkins --password tQrYdOHhBqOMJi/k --type ${release_prefix}-qe-smoketest-trigger  --header label:bxms-ci --header deliverable_list:${deliverable_list} --body deliverable_list:${deliverable_list}
'''

// Creates or updates a free style job.
job("sample-trigger-qe-smoke-test") {

    // Sets a description for the job.
    description("This job is responsible for triggering QE smoke test.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }

    // Adds environment variables to the build.
    environmentVariables {

        // Adds environment variables from a properties file.
        propertiesFile('${HOME}/brms-64-jenkins-ci.properties')

        // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
        keepBuildVariables(true)

        // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
        keepSystemVariables(true)
    }

    scm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch('${ip_config_branch}')
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)

        // Sends JMS message.
        ciMessageBuilder {

            // JMS selector to choose messages that will fire the trigger.
            providerName("default")

            // Type of CI message to be sent.
            messageType("custom")

            // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
            messageProperties("label=bxms-ci\n" +
                    "CI_TYPE=custom\n" +
                    "EVENT_TYPE=brms-64-qe-smoketest-trigger\n")

            // Content of CI message to be sent.
            messageContent('${rcm_stage_base}/${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/${release_prefix}-deliverable-list-staging.properties')
        }
    }
}
