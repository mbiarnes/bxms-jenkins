import org.jboss.bxms.jenkins.JobTemplate

// Create handover script
def shellScript = """
python ip-tooling/template_helper.py -i \${handover_template_basename}-\${product1_lowcase}.template -p ${CI_PROPERTIES_FILE} -o \${release_handover_basename}-\${product1_lowcase}.adoc
asciidoctor \${release_handover_basename}-\${product1_lowcase}.adoc

python ip-tooling/template_helper.py -i \${handover_template_basename}-\${product2_lowcase}.template -p ${CI_PROPERTIES_FILE} -o \${release_handover_basename}-\${product2_lowcase}.adoc
asciidoctor \${release_handover_basename}-\${product2_lowcase}.adoc

git config --global user.email "jb-ip-tooling-jenkins@redhat.com"
git config --global user.name "bxms-prod"

if [ -f \${product1_pvt_report_basename}.html ];then
    cp \${product1_pvt_report_basename}.html \${archive_pvt_report_basename}-\${product1_lowcase}.html
fi
if [ -f \${product2_pvt_report_basename}.html ];then
    cp \${product2_pvt_report_basename}.html \${archive_pvt_report_basename}-\${product2_lowcase}.html
fi
git add .

cd bxms-jenkins
#sed -i 's/releaseci_trigger=true/releaseci_trigger=false/g' ${CI_PROPERTIES_FILE}
commit_msg="Prepare handover PR \${product1_name} \${product1_version} \${product1_milestone}"

git commit -m "\${commit_msg}"
git push origin HEAD:refs/for/master 2>&1| tee b.log 

handover_pr=`grep "\${commit_msg}" b.log`
handover_pr=\${handover_pr#remote: }
handover_pr=\${handover_pr%% Prepare*}
handover_pr=`echo -e "\${handover_pr}" | tr -d '[:space:]'`
#Update the handover pr link
sed -i '/^handover_pr=/d' ${CI_PROPERTIES_FILE} && echo "handover_pr=\$handover_pr" >>${CI_PROPERTIES_FILE}

echo "JOB DONE"
"""
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-create-handover") {

    // Sets a description for the job.
    description("This job creates the handover report and pushes it to the staging area.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
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

                        // Sets the files to upload to a server.
                        sourceFiles('${archive_pvt_report_basename}-{product1_lowcase},${release_handover_basename}-{product1_lowcase}.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('release_stream_path}/release-history')

                        // Sets the destination folder.
                        remoteDirectory('${product1_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${archive_pvt_report_basename}-{product2_lowcase},${release_handover_basename}-{product2_lowcase}.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('${release_stream_path}/release-history')

                        // Sets the destination folder.
                        remoteDirectory('${product2_staging_path}')
                    }
                }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)