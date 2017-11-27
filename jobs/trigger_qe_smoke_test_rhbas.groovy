import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "QE smoketest is triggered by CI message. Build URL:\${qe_smoketest_job_url}" -f
"""
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-trigger-qe-smoke-test-rhbas") {

    // Sets a description for the job.
    description("This job is responsible for triggering QE smoke test.")

    // Adds build steps to the jobs.
    steps {
        shell(shellScript)

        // Sends JMS message.
        ciMessageBuilder {
            overrides {
                topic("default")
            }

            // JMS selector to choose messages that will fire the trigger.
            providerName("CI Publish")

            // Type of CI message to be sent.
            messageType("Custom")

            // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
            messageProperties("label=rhap-ci\n" +
                    "CI_TYPE=custom\n" +
                    "EVENT_TYPE=rhbas-70-brew-qe-trigger\n")

            // Content of CI message to be sent.
            messageContent('${product2_staging_properties_url}')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)