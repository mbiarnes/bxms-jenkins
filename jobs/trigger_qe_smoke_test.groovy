import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
case "\${PRODUCT_NAME}" in
    RHDM )
        prod_staging_properties_url=\${product1_staging_properties_url}
        ;;
    RHBAS )
        prod_staging_properties_url=\${product2_staging_properties_url}
        ;;
esac

echo "prod_staging_properties_url=\${prod_staging_properties_url}" > /tmp/prod_staging_properties_url

ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "QE smoketest is triggered by CI message. Build URL:\${qe_smoketest_job_url}" -f
"""
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-trigger-qe-smoke-test-rhdm") {

    // Sets a description for the job.
    description("This job is responsible for triggering QE smoke test.")

    parameters {
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "RHDM",
                description = "Specify product name to switch between configurations.")
    }

    // Adds build steps to the jobs.
    steps {
        shell(shellScript)

        environmentVariables {
            propertiesFile("/tmp/prod_staging_properties_url")
        }

        // Sends JMS message.
        ciMessageBuilder {
            overrides {
                topic('VirtualTopic.qe.ci.ba.${PRODUCT_NAME}.70.brew.trigger')
            }

            // JMS selector to choose messages that will fire the trigger.
            providerName("Red Hat UMB")

            // Type of CI message to be sent.
            messageType("Custom")

            // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
            messageProperties('label=rhap-ci\n' +
                    'CI_TYPE=custom\n' +
                    'EVENT_TYPE=$PRODUCT_NAME-70-brew-qe-trigger\n')
            // Content of CI message to be sent.
            messageContent('${prod_staging_properties_url}')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
