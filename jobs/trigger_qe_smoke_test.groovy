import org.jboss.bxms.jenkins.JobTemplate

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-trigger-qe-smoke-test") {

    // Sets a description for the job.
    description("This job is responsible for triggering QE smoke test.")

    // Adds build steps to the jobs.
    steps {

        // Sends JMS message.
        ciMessageBuilder {

            // JMS selector to choose messages that will fire the trigger.
            providerName("default")

            // Type of CI message to be sent.
            messageType("Custom")

            // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
            messageProperties("label=bxms-ci\n" +
                    "CI_TYPE=custom\n" +
                    "EVENT_TYPE=brms-64-qe-smoketest-trigger\n")

            // Content of CI message to be sent.
            messageContent('${brms_staging_properties_url}')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
