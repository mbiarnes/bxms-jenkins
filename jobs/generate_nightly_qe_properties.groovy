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
        ;;
esac

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
ip-tooling/maven-artifact-handler.py --version=\${prod_artifact_version} --override-version \${prod_shipped_file_deliver_version} --maven-repo \${jenkins_cache_url}/\${jenkins_cache_repo} \
  --deliverable \${prod_deliverable_template} \
  --release-url=\${rcm_staging_base}/\${prod_staging_path} --output-deliverable-list \${prod_staging_properties_name}
cp ${IP_CONFIG_FILE} \${PRODUCT_NAME}
  
#append the other properties per qe's requirement
appendProp "KIE_VERSION" \${kie_version} 
appendProp "\${PRODUCT_NAME}""_VERSION" \${prod_artifact_version}

"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-generate-nightly-qe-properties") {

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
    }
    wrappers {
        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)