import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
echo "Done"
"""
def report_string = '''{"SuccessfulJobs":{"BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-db":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-business-central-smoke-wls":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-dashbuilder-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-dashbuilder-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-integration-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-quickstarts-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-united-exec-servers-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-wb-rest-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-wb-rest-smoke-wls":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-bre-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-business-central-smoke-container":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-business-central-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-business-central-smoke-wls":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-quickstarts-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-wb-rest-smoke-was":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bxms-prod-6.4-blessed-inc-maven-repo-testsuite-smoke":"SUCCESS","BxMS/BxMS-prod-6.4/smoke-prod/bxms-prod-6.4-blessed-maven-repo-testsuite-smoke":"SUCCESS"},"UnsuccessfulJobs":{"BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-clustering-smoke":"FAILURE","BxMS/BxMS-prod-6.4/smoke-prod/bpms-prod-6.4-blessed-dashbuilder-smoke-db":"UNSTABLE","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-exec-server-smoke-container":"FAILURE","BxMS/BxMS-prod-6.4/smoke-prod/brms-prod-6.4-blessed-wb-rest-smoke-container":"FAILURE"},"Statistics":{"TotaBuildRuns":23,"SuccessfulBuilds":19,"UnsuccessfulBuilds":4}}'''
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-mock-qe-smoketest-report") {

    // Sets a description for the job.
    description("This job is responsible for mocking a CI message triggered returned the smoketest result from QE.")

    parameters {
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "rhdm",
                description = "Specify product name to switch between configurations.")
    }
    // Adds build steps to the jobs.
    steps {
        shell(shellScript)

        // Sends JMS message.
        ciMessageBuilder {
            overrides {
                topic('VirtualTopic.qe.ci.ba.$PRODUCT_NAME.70.brew.smoke.results')
            }

            // JMS selector to choose messages that will fire the trigger.
            providerName("Red Hat UMB")

            // Type of CI message to be sent.
            messageType("Custom")

            // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
            messageProperties('label=rhap-ci\n' +
                    'CI_TYPE=customer\n' +
                    'EVENT_TYPE=$PRODUCT_NAME-70-brew-qe-smoke-results\n')

            // Content of CI message to be sent.
            messageContent(report_string)
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
