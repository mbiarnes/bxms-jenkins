import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
set -x
#kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
function appendProp(){
    if [ -z "\$1" ] || [ -z "\$2" ];then
        echo "Param  is not allow empty"
        exit 1
    fi
    sed -i "/^\$1/d" \${brms_staging_properties_name} && echo "\$1=\$2" >> \${brms_staging_properties_name}
}

#Uploading to rcm staging folder
if [ \${release_type} = 'intpack' ];then
    ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_version} --maven-repo \${product_assembly_maven_repo_url} \
      --deliverable \${release_prefix}-release/\${release_prefix}-deliverable.properties --output \${product_name}-\${product_version}
else
    ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_deliver_version} --maven-repo \${product_assembly_maven_repo_url} \
      --deliverable \${release_prefix}-release/brms-deliverable.properties --output \${brms_product_name} \
      --release-url=\${rcm_staging_base}/\${brms_staging_path} --output-deliverable-list \${brms_staging_properties_name}
      
    ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_deliver_version} --maven-repo \${product_assembly_maven_repo_url} \
      --deliverable \${release_prefix}-release/bpmsuite-deliverable.properties --output \${bpms_product_name} \
      --release-url=\${rcm_staging_base}/\${bpms_staging_path} --output-deliverable-list \${brms_staging_properties_name}
    
    #append the other properties per qe's requirement
    appendProp "build.config" \${rcm_staging_base}/\${brms_staging_path}/${IP_CONFIG_FILE} 
    appendProp "DROOLSJBPM_VERSION" \${kie_version} 
    appendProp "BXMS_VERSION" \${product_artifact_version} 
    
    sed -e 's=\${rcm_staging_base}/\${brms_staging_folder}=\${rcm_candidate_base}/\${brms_product_name}=g' \
        -e 's=\${rcm_staging_base}/\${bpms_staging_folder}=\${rcm_candidate_base}/\${bpms_product_name}=g' \
        \${brms_staging_properties_name} >> \${brms_candidate_properties_name}
fi
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-stage-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

    // Allows to parameterize the job.
    parameters {

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
                                        'rm -vrf  ~/staging/${brms_staging_path}/* ~/staging/${bpms_staging_path}/* \n' +
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