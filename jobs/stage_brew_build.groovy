import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """
set -x
#kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

case "\${PRODUCT_NAME}" in 
    RHDM )
        prod_staging_properties_name=\${product1_staging_properties_name}
        prod_staging_properties_url=\${product1_staging_properties_url}
        prod_artifact_version=\${product1_artifact_version}
        prod_shipped_file_deliver_version=\${product1_shipped_file_deliver_version}
        prod_assembly_maven_repo_url=\${product1_assembly_maven_repo_url}
        prod_deliverable_template=\${product1_deliverable_template}
        prod_staging_path=\${product1_staging_path}
        prod_staging_folder=\${product1_staging_folder}
        prod_candidate_properties_name=\${product1_candidate_properties_name}
        ;;
    RHBAS )
        prod_staging_properties_name=\${product2_staging_properties_name}
        prod_staging_properties_url=\${product2_staging_properties_url}
        prod_artifact_version=\${product2_artifact_version}
        prod_shipped_file_deliver_version=\${product2_shipped_file_deliver_version}
        prod_assembly_maven_repo_url=\${product2_assembly_maven_repo_url}
        prod_deliverable_template=\${product2_deliverable_template}
        prod_staging_path=\${product2_staging_path}
        prod_staging_folder=\${product2_staging_folder}
        prod_candidate_properties_name=\${product2_candidate_properties_name}
        ;;
esac

echo "prod_staging_path=\$prod_staging_path" > /tmp/prod_staging_path

function appendProp(){
    if [ -z "\$1" ] || [ -z "\$2" ];then
        echo "Param  is not allow empty"
        exit 1
    fi
    sed -i "/^\$1/d" \${prod_staging_properties_name} && echo "\$1=\$2" >> \${prod_staging_properties_name}
}

if ! wget \${prod_staging_properties_url} -O \${prod_staging_properties_name} 2>/dev/null ;then
    echo " \${prod_staging_properties_url} isn't available yet"  
fi
ip-tooling/maven-to-stage.py --version=\${prod_artifact_version} --override-version \${prod_shipped_file_deliver_version} --maven-repo \${prod_assembly_maven_repo_url} \
  --deliverable \${prod_deliverable_template} --output \${PRODUCT_NAME} \
  --release-url=\${rcm_staging_base}/\${prod_staging_path} --output-deliverable-list \${prod_staging_properties_name}
cp ${IP_CONFIG_FILE} \${PRODUCT_NAME}
  
#append the other properties per qe's requirement
appendProp "build.config" \${rcm_staging_base}/\${prod_staging_path}/\${IP_CONFIG_FILE} 
appendProp "KIE_VERSION" \${kie_version} 
appendProp "\${PRODUCT_NAME}""_VERSION" \${prod_artifact_version}

sed -e "s=\${rcm_staging_base}/\${prod_staging_folder}=\${rcm_candidate_base}/\${PRODUCT_NAME}=g" \
    \${prod_staging_properties_name} > \${prod_candidate_properties_name}

"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-stage-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

    // Allows to parameterize the job.
    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        booleanParam(parameterName = "CLEAN_STAGING_ARTIFACTS", defaultValue = false, description = "WARNING, click this will force remove your artifacts in staging folder!")
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "",
                description = "Specify product name to switch between configurations.")
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
        // Inject environment variables for $prod_staging_path
        environmentVariables {
            propertiesFile("/tmp/prod_staging_path")
        }
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
                        remoteDirectory('${prod_staging_path}')
                        execCommand('if [ "${CLEAN_STAGING_ARTIFACTS}" = "true" ];then \n' +
                                        'rm -vrf  ~/staging/${prod_staging_path}/* \n' +
                                    'fi')
                    }
                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${PRODUCT_NAME}/*.*')
                        removePrefix('${PRODUCT_NAME}/')

                        // Sets the destination folder.
                        remoteDirectory('${prod_staging_path}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${IP_CONFIG_FILE}, `echo ${PRODUCT_NAME,,}`-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${prod_staging_path}')
                    }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)