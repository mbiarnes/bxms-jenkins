import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
set -x
#kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
function appendProp(){
    if [ -z "\$1" ] || [ -z "\$2" ];then
        echo "Param  is not allow empty"
        exit 1
    fi
    sed -i "/^\$1/d" \${product1_staging_properties_name} && echo "\$1=\$2" >> \${product1_staging_properties_name}
    sed -i "/^\$1/d" \${product2_staging_properties_name} && echo "\$1=\$2" >> \${product2_staging_properties_name}
}

if ! wget \${product1_staging_properties_url} -O \${product1_staging_properties_name} 2>/dev/null ;then
    echo " \${product1_staging_properties_url} isn't available yet"  
fi
ip-tooling/maven-to-stage.py --version=\${product1_artifact_version} --override-version \${product1_shipped_file_deliver_version} --maven-repo \${product_assembly_maven_repo_url} \
  --deliverable \${product1_deliverable_template} --output \${product1_product_name} \
  --release-url=\${rcm_staging_base}/\${product1_staging_path} --output-deliverable-list \${product1_staging_properties_name}
cp ${IP_CONFIG_FILE} \${product1_product_name}
  
ip-tooling/maven-to-stage.py --version=\${product2_artifact_version} --override-version \${product2_shipped_file_deliver_version} --maven-repo \${product_assembly_maven_repo_url} \
  --deliverable \${product2_deliverable_template} --output \${product2_product_name} \
  --release-url=\${rcm_staging_base}/\${product2_staging_path} --output-deliverable-list \${product2_staging_properties_name}
cp ${IP_CONFIG_FILE} \${product2_product_name}

#append the other properties per qe's requirement
appendProp "build.config" \${rcm_staging_base}/\${product1_staging_path}/${IP_CONFIG_FILE} 
appendProp "DROOLSJBPM_VERSION" \${kie_version} 
appendProp "BXMS_VERSION" \${product_artifact_version} 

sed -e "s=\${rcm_staging_base}/\${product1_staging_folder}=\${rcm_candidate_base}/\${product1_product_name}=g" \
    \${product1_staging_properties_name} > \${product1_candidate_properties_name}
sed -e "s=\${rcm_staging_base}/\${product2_staging_folder}=\${rcm_candidate_base}/\${product2_product_name}=g" \
    \${product2_staging_properties_name} > \${product2_candidate_properties_name}

"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-stage-brew-build") {

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

                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('')
                        // Sets the destination folder.
                        remoteDirectory('${product1_staging_path}')
                        execCommand('if [ "${CLEAN_STAGING_ARTIFACTS}" = "true" ];then \n' +
                                        'rm -vrf  ~/staging/${product1_staging_path}/* ~/staging/${product2_staging_path}/* \n' +
                                    'fi')
                    }
                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${product1_product_name}/*.*')
                        removePrefix('${product1_product_name}/')

                        // Sets the destination folder.
                        remoteDirectory('${product1_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${product2_product_name}/*.*')
                        removePrefix('${product2_product_name}/')


                        // Sets the destination folder.
                        remoteDirectory('${product2_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${IP_CONFIG_FILE},${release_code}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${product1_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${IP_CONFIG_FILE},${release_code}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${product2_staging_path}')
                    }

            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)