import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
echo "Done"
"""
def report_string = '''{
  "BxMS/BxMS-prod-test/smoke-prod/bpms-prod-test-blessed-business-central-smoke-container":"SUCCESS",
  "BxMS/BxMS-prod-test/smoke-prod/bpms-prod-test-blessed-business-central-smoke-db":"FAILURE",
  "BxMS/BxMS-prod-test/smoke-prod/bpms-prod-test-blessed-business-central-smoke-db1":"FAILURE",
  "BxMS/BxMS-prod-test/smoke-prod/bpms-prod-test-blessed-integration-smoke-container":"UNSTABLE",
  "BxMS/BxMS-prod-test/smoke-prod/bpms-prod-test-blessed-integration-smoke-container1":"UNSTABLE",
  "Total build runs":"5",
  "Successful builds":"1",
  "Other build results":"4"
}'''
// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-mock-qe-smoketest-report") {

    // Sets a description for the job.
    description("This job is responsible for mocking a CI message triggered returned the smoketest result from QE.")

    // Adds build steps to the jobs.
    steps {
        shell(shellScript)

        // Sends JMS message.
        ciMessageBuilder {

            // JMS selector to choose messages that will fire the trigger.
            providerName("default")

            // Type of CI message to be sent.
            messageType("Custom")

            // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
            messageProperties("label=bxms-ci\n" +
                    "CI_TYPE=custom\n" +
                    "EVENT_TYPE=\${release_prefix}-qe-smoketest-report\n")

            // Content of CI message to be sent.
            messageContent(report_string)
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)