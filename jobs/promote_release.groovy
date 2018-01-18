import org.jboss.bxms.jenkins.JobTemplate

String shellScript = """
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
sed -i 's/release_status=/release_status=closed/g' ${CI_PROPERTIES_FILE}
sed -i '/^release_status=/d' ${CI_PROPERTIES_FILE} && echo "release_status=closed" >>${CI_PROPERTIES_FILE}
"""
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-promote-release") {

    // Sets a description for the job.
    description("This job is responsible for uploading release to candidate area.")
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
    if(JOB_NAME.matches("codereview/(.*)")){
        println "Detected in codereview:Disable promote-release"
        disabled()
    }
    triggers{
        gerrit{

            project("bxms-jenkins", "ant:**")
            events {
                changeMerged()
            }
            configure { triggers ->
                triggers   <<  {
                    'serverName' 'code.engineering.redhat.com'
                }
                triggers/'gerritProjects'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'/'filePaths'/'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' << {
                    'compareType' 'REG_EXP'
                    'pattern' 'stream/bxms/release-history/*-handover.adoc'
                }
            }
        }
    }
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

                    // Sets a timeout in milliseconds for the command to execute. Defaults to two minutes.
                    execTimeout(0)

                    // Specifies a command to execute on the remote server.
                    execCommand('kinit -k -t ~/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM\n' +
                            '/mnt/redhat/scripts/rel-eng/utility/bus-clients/stage-mw-release ${product1_name}-${product1_milestone_version}\n')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
