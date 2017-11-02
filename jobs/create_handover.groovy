import org.jboss.bxms.jenkins.JobTemplate

// Create handover script
def shellScript = """
python ip-tooling/template_helper.py -i \${handover_template} -p ${CI_PROPERTIES_FILE} -o \${qe_handover_basename}.adoc
asciidoctor \${qe_handover_basename}.adoc

git config --global user.email "jb-ip-tooling-jenkins@redhat.com"
git config --global user.name "bxms-prod"
#Skip providing pvt test report for intpack and patch release
if [ \${release_type} != "intpack" ] || [ \$release_type != "patch" ] ;then
    if [ -f \${brms_pvt_report_basename}.html ];then
        cp \${brms_pvt_report_basename}.html \${archive_pvt_report_basename}-brms.html
    fi
    if [ -f \${bpms_pvt_report_basename}.html ];then
        cp \${bpms_pvt_report_basename}.html \${archive_pvt_report_basename}-bpms.html
    fi
    git add .
fi

cd bxms-jenkins
#sed -i 's/releaseci_trigger=true/releaseci_trigger=false/g' ${CI_PROPERTIES_FILE}
commit_msg="Prepare handover PR \${product_name} \${product_version} \${release_milestone}"

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

                if (RELEASE_CODE == "intpack-fuse63-bxms64") {

                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_handover_basename}.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('${release_stream_path}/release-history')

                        // Sets the destination folder.
                        remoteDirectory('${product_stage_folder}/${product_name}-${product_version}')
                    }

                } else {
                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${archive_pvt_report_basename}-*,${release_handover_basename}.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('release_stream_path}/release-history')

                        // Sets the destination folder.
                        remoteDirectory('${brms_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${archive_pvt_report_basename}-*,${release_handover_basename}.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('${release_stream_path}/release-history')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_staging_path}')
                    }
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)