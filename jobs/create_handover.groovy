import org.jboss.bxms.jenkins.JobTemplate

// Create handover script
def shellScript = null

    shellScript = """
ip-tooling/create_handover.py -a \${pvt_summary_adoc} -t \${release_prefix}-release/\${release_prefix}-handover.template -p ${CI_PROPERTIES_FILE} -o \${release_prefix}-release/\${release_prefix}-handover.adoc
asciidoctor \${release_prefix}-release/\${release_prefix}-handover.adoc

git config --global user.email "bxms-prod@redhat.com"
git config --global user.name "bxms-prod"
#Skip providing pvt test report for intpack and patch release
if [ \${release_type} != "intpack" ] || [ \$release_type != "patch" ] ;then
    cp \${brms_pvt_report_html} \${release_prefix}-release/\${release_prefix}-pvt-report-brms.html
    cp \${bpms_pvt_report_html} \${release_prefix}-release/\${release_prefix}-pvt-report-bpms.html
    git add \${release_prefix}-release/\${release_prefix}-pvt-report*.html
fi

#sed -i 's/releaseci_trigger=true/releaseci_trigger=false/g' \${release_prefix}.cfg
commit_msg="Prepare handover PR \${product_name} \${product_version} \${release_milestone}"

git commit -a -m "\${commit_msg}"
git push origin HEAD:refs/for/master 2>&1| tee b.log 

handover_pr=`grep "\${commit_msg}" b.log`
handover_pr=\${handover_pr#remote: }
handover_pr=\${handover_pr%% Prepare*}
#Update the handover pr link
sed -i '/^handover_pr=/d' ${CI_PROPERTIES_FILE} && echo "handover_pr=\$handover_pr" >>${CI_PROPERTIES_FILE}
"""
// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-create-handover") {

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

                if (PRODUCT_NAME == "intpack-fuse63-bxms64") {

                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-release/${release_prefix}-handover.html,${release_prefix}-release/${release_prefix}-pvt-report.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('${release_prefix}-release')

                        // Sets the destination folder.
                        remoteDirectory('${product_stage_folder}/${product_name}-${product_version}')
                    }

                } else {
                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-release/${release_prefix}-handover.html,${release_prefix}-release/${release_prefix}-pvt-report-brms.html,${release_prefix}-release/${release_prefix}-pvt-report-bpms.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('${release_prefix}-release')

                        // Sets the destination folder.
                        remoteDirectory('${brms_stage_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-release/${release_prefix}-handover.html,${release_prefix}-release/${release_prefix}-pvt-report-brms.html,${release_prefix}-release/${release_prefix}-pvt-report-bpms.html')

                        // Sets the first part of the file path that should not be created on the remote server.
                        removePrefix('${release_prefix}-release')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_stage_path}')
                    }
                }
            }
        }
        publishers {
            postBuildTask {
                task('JOB DONE', "ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a 'handover has been created' -f ")
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)