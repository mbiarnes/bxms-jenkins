import org.jboss.bxms.jenkins.JobTemplate
def shellScript = """# Disable bash tracking mode, too much noise.
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
jira_comment1= "Hi, All
BRMS & BPMSuite \${product_version} \${release_milestone} {color:#d04437}is now available{color}.

The BxMS \${product_version} \${release_milestone} Release is ready for QA. 

Candidate download URL:
Handover: [\${rcm_candidate_base}/\${bpms_staging_path}/\${release_prefix}-handover.html]

[\${rcm_candidate_base}/\${brms_staging_path}/]

[\${rcm_candidate_base}/\${bpms_staging_path}/]

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
            messageContent('${brms_properties_url}')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, RELEASE_CODE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)