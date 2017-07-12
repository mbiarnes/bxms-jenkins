import org.jboss.bxms.jenkins.JobTemplate

// QE handover test trigger script
String shellScript = '''#Publish a CI Message to trigger the QE smoketest
#echo "Send CI message CI_TYPE='${release_prefix}-qe-smoketest-trigger"
deliverable_list=${rcm_candidate_base}/${brms_product_name}/${brms_product_name}-${product_version}.${release_milestone}/${release_prefix}-deliverable-list.properties
echo ${deliverable_list}
#utility/publish.py --user ci-ops-central-jenkins --password tQrYdOHhBqOMJi/k --type ${release_prefix}-qe-smoketest-trigger  --header label:bxms-ci --header deliverable_list:${deliverable_list} --body deliverable_list:${deliverable_list}
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-trigger-qe-handover-test") {

    // Sets a description for the job.
    description("This job is responsible for triggering QE handover test.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
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
                    "EVENT_TYPE=brms-64-qe-handover-trigger\n")

            // Content of CI message to be sent.
            messageContent('${rcm_candidate_base}/${brms_product_name}/${brms_product_name}-${product_version}.${release_milestone}/${release_prefix}-deliverable -list.properties')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
