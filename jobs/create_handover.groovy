// Create handover script
String shellScript = '''rm -rf  integration-platform-config

git clone ssh://zheyzhan@code.engineering.redhat.com:22/integration-platform-config.git
cd integration-platform-config
git checkout ${ip_config_branch}

git clone ssh://zheyzhan@code.engineering.redhat.com:22/integration-platform-tooling.git
cd integration-platform-tooling
git checkout ${ip_tooling_branch}

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
commit_msg="Prepare handover PR ${product_name} ${product_version} ${release_milestone}  "


git commit -a -m "${commit_msg}"
git push origin HEAD:refs/for/${ip_config_branch} 2>&1| tee b.log 


handover_pr=`grep "${commit_msg}" b.log`
handover_pr=${handover_pr#remote: }
handover_pr=${handover_pr%% Prepare*}
sed -i '/^handover_pr=/d' ${HOME}/${release_prefix}-jenkins-ci.properties
echo "handover_pr=$handover_pr" >>${HOME}/${release_prefix}-jenkins-ci.properties
'''

// Creates or updates a free style job.
job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-create-handover") {

    // Sets a description for the job.
    description("This job is responsible for creating handover.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds environment variables to the build.
    environmentVariables {

        // Adds environment variables from a properties file.
        propertiesFile('${HOME}/brms-64-jenkins-ci.properties')

        // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
        keepBuildVariables(true)

        // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
        keepSystemVariables(true)
    }

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

        // Adds timestamps to the console log.
        timestamps()
    }


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
                    sourceFiles('integration-platform-config/brms-64-release/brms-64-handover.html,integration-platform-config/brms-64-release/brms-64-pvt-report-brms.html,integration-platform-config/brms-64-release/brms-64-pvt-report-bpms.html')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/brms-64-release')

                    // Sets the destination folder.
                    remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('integration-platform-config/brms-64.cfg')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/')

                    // Sets the destination folder.
                    remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('integration-platform-config/brms-64-release/brms-64-handover.html,integration-platform-config/brms-64-release/brms-64-pvt-report-brms.html,integration-platform-config/brms-64-release/brms-64-pvt-report-bpms.html')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/brms-64-release')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')
                }

                // Adds a transfer set.
                transferSet {

                    // Sets the files to upload to a server.
                    sourceFiles('integration-platform-config/brms-64.cfg')

                    // Sets the first part of the file path that should not be created on the remote server.
                    removePrefix('integration-platform-config/')

                    // Sets the destination folder.
                    remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')
                }
            }
        }
    }
}