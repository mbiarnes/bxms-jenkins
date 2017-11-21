import org.jboss.bxms.jenkins.JobTemplate
def shellScript = """# Disable bash tracking mode, too much noise.
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
jira_comment1= "Hi, All
\${product1_name} \${product1_version} \${product1_milestone} {color:#d04437}is now available{color}.
Download URL: [\${rcm_candidate_base}/\${product1_staging_path}/]
Handover: [\${rcm_candidate_base}/\${product1_staging_path}/\${release_code}-handover.html]

\${product2_name} \${product2_version} \${product2_milestone} {color:#d04437}is now available{color}.
Download URL: [\${rcm_candidate_base}/\${product2_staging_path}/]
Handover: [\${rcm_candidate_base}/\${product2_staging_path}/\${release_code}-handover.html]

"

ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "\${jira_comment1}" -f
ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "QE full test is triggered by CI message. Build URL:\${qe_fulltest_url}" -f
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-trigger-qe-handover-test") {

    // Sets a description for the job.
    description("This job is responsible for triggering QE handover test.")

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
            messageProperties("label=bxms-ci\n" +
                    "CI_TYPE=custom\n" +
                    "EVENT_TYPE=brms-64-qe-handover-trigger\n")

            // Content of CI message to be sent.
            messageContent('${product1_properties_url}')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)