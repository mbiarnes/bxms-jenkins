import org.jboss.bxms.jenkins.JobTemplate

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-promote-release") {

    // Sets a description for the job.
    description("This job is responsible for uploading release to candidate area.")

    // Adds post-build actions to the job.
    publishers {

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                // Adds a target server.
                verbose(true)

                // Adds a transfer set.
                transferSet {

                    // Specifies a command to execute on the remote server.
                    execCommand('kinit -k -t ~/host-host-8-172-124.host.centralci.eng.rdu2.redhat.com.keytab host/host-8-172-124.host.centralci.eng.rdu2.redhat.com@REDHAT.COM\n' +
                            '/mnt/redhat/scripts/rel-eng/utility/bus-clients/stage-mw-release ${brms_product_name}-${product_version}.${release_milestone}\n' +
                            '/mnt/redhat/scripts/rel-eng/utility/bus-clients/stage-mw-release ${bpms_product_name}-${product_version}.${release_milestone}')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)