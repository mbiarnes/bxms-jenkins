import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """# Disable bash tracking mode, too much noise.
#set +x
if [ ! -z \$CI_MESSAGE ];then
name=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['name']"` 1>/dev/null
version=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['version']"` 1>/dev/null
release=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['release']"` 1>/dev/null
task_id=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['task_id']"` 1>/dev/null
fi

maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"

#Uploading to rcm staging folder
if [ \${release_type} = 'intpack' ];then
    ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_version} --maven-repo \${maven_repo_url} \
      --deliverable \${release_prefix}-release/\${release_prefix}-deliverable.properties --output \${product_name}-\${product_version}
else
    ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_deliver_version} --maven-repo \${maven_repo_url} \
      --deliverable \${release_prefix}-release/\${release_prefix}-deliverable.properties --output \${brms_product_name} \
      --release-url=\${rcm_staging_base}/\${brms_staging_folder} --output-deliverable-list \${brms_staging_properties_name}
      
    ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_deliver_version} --maven-repo \${maven_repo_url} \
      --deliverable \${release_prefix}-release/bpmsuite-deliverable.properties --output \${bpms_product_name} \
      --release-url=\${rcm_staging_base}/\${bpms_staging_folder} --output-deliverable-list \${brms_staging_properties_name}
    
    #append the other properties per qe's requirement
    sed -i '/^build.config=/d\' \${brms_staging_properties_name} \
        && echo "build.config=\${rcm_staging_base}/\${brms_staging_folder}/\${IP_CONFIG_FILE}">>\${brms_staging_properties_name}
    sed -i '/^DROOLSJBPM_VERSION=/d\' \${brms_staging_properties_name} \
        && echo "DROOLSJBPM_VERSION=\${kie_version}">>\${brms_staging_properties_name}
    sed -i '/^BXMS_VERSION=/d\' \${brms_staging_properties_name} \
        && echo "BXMS_VERSION=\${product_artifact_version}">>\${brms_staging_properties_name}
    sed -e 's=\${rcm_staging_base}/\${brms_staging_folder}=\${rcm_candidate_base}/\${brms_product_name}=g' \
        -e 's=\${rcm_staging_base}/\${bpms_staging_folder}=\${rcm_candidate_base}/\${brms_product_name}=g' \
        \${brms_staging_properties_name} >> \${brms_candidate_properties_name}
fi
echo "JOB DONE"
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-stage-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

    // Allows to parameterize the job.
    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "name", defaultValue = null, description = "Brew Build Package name")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "version", defaultValue = null, description = "Brew Build version")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "release", defaultValue = null, description = "Brew Build release number")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "task_id", defaultValue = null, description = "Brew Build task id")

        // Defines a simple text parameter, where users can enter a string value.
        booleanParam(parameterName = "CLEAN_STAGING_ARTIFACTS", defaultValue = false, description = "WARNING, click this will force remove your artifacts in staging folder!")
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
    wrappers {
        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

    }
    // Adds post-build actions to the job.
    publishers {

        // Send artifacts to an SSH server (using SFTP) and/or execute commands over SSH.
        publishOverSsh {

            // Adds a target server.
            server('publish server') {

                if (PRODUCT_NAME == "intpack-fuse63-bxms64") {

                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${product_name}-${product_version}/*.*')

                        // Sets the destination folder.
                        remoteDirectory('${product_stage_folder}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}.cfg')

                        // Sets the destination folder.
                        remoteDirectory('${product_stage_folder}/${product_name}-${product_version}/')
                    }

                } else {
                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('')
                        // Sets the destination folder.
                        remoteDirectory('${brms_staging_path}')
                        execCommand('if [ "${CLEAN_STAGING_ARTIFACTS}" = "true" ];then \n' +
                                        'rm -vf  ~/staging/${brms_staging_path}/* ~/staging/${bpms_staging_path}/* \n' +
                                    'fi')
                    }
                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${brms_product_name}/*.*')
                        removePrefix('${brms_product_name}/')

                        // Sets the destination folder.
                        remoteDirectory('${brms_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${bpms_product_name}/*.*')
                        removePrefix('${bpms_product_name}/')


                        // Sets the destination folder.
                        remoteDirectory('${bpms_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${IP_CONFIG_FILE},${release_prefix}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${brms_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${IP_CONFIG_FILE},${release_prefix}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_staging_path}')
                    }

                }

            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)