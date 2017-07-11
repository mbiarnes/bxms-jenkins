import org.jboss.bxms.jenkins.JobTemplate

// Create handover script
String shellScript = '''rm -rf  integration-platform-config

#chmod 655 create_handover.py
python create_handover.py -a ${bpms_pvt_summary_adoc} -t ../${release_prefix}-release/${release_prefix}-handover.template -p ${HOME}/${release_prefix}-jenkins-ci.properties -o ../${release_prefix}-release/${release_prefix}-handover.adoc
asciidoctor ../${release_prefix}-release/${release_prefix}-handover.adoc

cd ../
cp ${brms_pvt_report_html} ${release_prefix}-release/${release_prefix}-pvt-report-brms.html
cp ${bpms_pvt_report_html} ${release_prefix}-release/${release_prefix}-pvt-report-bpms.html

git config --global user.email "bxms-releaseci@redhat.com"
git config --global user.name "bxms-releaseci"
git add ${release_prefix}-release/${release_prefix}-pvt-report-*.html
sed -i 's/releaseci_trigger=true/releaseci_trigger=false/g' ${release_prefix}.cfg
commit_msg="Prepare handover PR ${product_name} ${product_version} ${release_milestone}"


git commit -a -m "${commit_msg}"
git push origin HEAD:refs/for/${ip_config_branch} 2>&1| tee b.log 


handover_pr=`grep "${commit_msg}" b.log`
handover_pr=${handover_pr#remote: }
handover_pr=${handover_pr%% Prepare*}
sed -i '/^handover_pr=/d' ${HOME}/${release_prefix}-jenkins-ci.properties
echo "handover_pr=$handover_pr" >>${HOME}/${release_prefix}-jenkins-ci.properties
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-create-handover") {

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
                    sourceFiles('integration-platform-config/${release_prefix}-release/${release_prefix}-handover.html,integration-platform-config/${release_prefix}-release/${release_prefix}-pvt-report-brms.html,integration-platform-config/${release_prefix}-release/${release_prefix}-pvt-report-bpms.html')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/${release-prefix}-release')

                    // Sets the destination folder.
                    remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('integration-platform-config/${release-prefix}.cfg')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/')

                    // Sets the destination folder.
                    remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('integration-platform-config/${release-prefix}-release/${release-prefix}-handover.html,integration-platform-config/${release-prefix}-release/${release-prefix}-pvt-report-brms.html,integration-platform-config/${release-prefix}-release/${release-prefix}-pvt-report-bpms.html')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/${release-prefix}-release')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('integration-platform-config/${release-prefix}.cfg')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)