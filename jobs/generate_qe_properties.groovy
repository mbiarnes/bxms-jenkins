import org.jboss.bxms.jenkins.JobTemplate

def shellScript = """

set -x
#kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

function appendProp(){
    if [ -z "\$1" ] || [ -z "\$2" ];then
        echo "Param  is not allow empty"
        exit 1
    fi
    sed -i "/^\$1/d" \${prod_properties_name} && echo "\$1=\$2" >> \${prod_properties_name}
}

if [ "\${release_code}" == "bxms-nightly" ]; then
    prod_properties_name=\${PRODUCT_NAME, ,}-\${build_date}.properties
    case "\${PRODUCT_NAME}" in
        RHDM )
            product_version="\${product1_version}"
            ;;
        RHBAS )
            product_version="\${product2_version}"
            ;;
    esac

    product_url_prefix="\${jenkins_cache_url}/\${jenkins_cache_repo}/org/kie/rhap/\${PRODUCT_NAME,,}/\${product_version}.redhat-\${build_date}"
    product_filename_common_prefix="\${product_version}.redhat-\${build_date}"
    echo "properties_staging_path=\${PRODUCT_NAME,,}/\${PRODUCT_NAME}-\${
    }.NIGHTLY" > /tmp/prod_staging_path
    echo "prod_properties_name=\${prod_properties_name}" >> /tmp/prod_staging_path
else
    case "\${PRODUCT_NAME}" in
        RHDM )
            prod_properties_name=\${product1_staging_properties_name}
            prod_staging_properties_url=\${product1_staging_properties_url}
            product_version=\${product1_shipped_file_deliver_version}

            prod_deliverable_template=\${product1_deliverable_template}
            prod_staging_path=\${product1_staging_path}
            prod_candidate_properties_name=\${product1_candidate_properties_name}
            prod_public_version_properties_name="RHDM_PUBLIC_VERSION"
            prod_public_version_properties_value=\${product1_milestone_version}
            prod_sources_name=\${product1_sources_name}
            ;;
        RHBAS )
            prod_properties_name=\${product2_staging_properties_name}
            prod_staging_properties_url=\${product2_staging_properties_url}
            product_version=\${product2_shipped_file_deliver_version}

            prod_deliverable_template=\${product2_deliverable_template}
            prod_staging_path=\${product2_staging_path}
            prod_candidate_properties_name=\${product2_candidate_properties_name}
            prod_public_version_properties_name="RHBA_PUBLIC_VERSION"
            prod_public_version_properties_value=\${product2_milestone_version}
            prod_sources_name=\${product2_sources_name}
            ;;
    esac

    product_url_prefix="\${rcm_staging_base}/\${prod_staging_path}"
    product_filename_common_prefix="\${PRODUCT_NAME,,}-\${product_version}"

    echo "properties_staging_path=\${prod_staging_path}" > /tmp/prod_staging_path
    echo "prod_properties_name=\${prod_properties_name}" >> /tmp/prod_staging_path
    echo "prod_candidate_properties_name=\${prod_candidate_properties_name}" >> /tmp/prod_staging_path
fi

if [ ! -f \$prod_properties_name ]; then
    touch \$prod_properties_name
fi

case "\${PRODUCT_NAME}" in
    RHDM )
        appendProp "\${PRODUCT_NAME,,}.decision-central.standalone.latest.url"    "\$product_url_prefix/\${product_filename_common_prefix}-decision-central-standalone.jar"
        appendProp "\${PRODUCT_NAME,,}.decision-central-eap7.latest.url"          "\$product_url_prefix/\${product_filename_common_prefix}-decision-central-eap7.zip"
        ;;
    RHBAS )
        appendProp "\${PRODUCT_NAME,,}.business-central.standalone.latest.url"    "\$product_url_prefix/\${product_filename_common_prefix}-business-central-standalone.jar"
        appendProp "\${PRODUCT_NAME,,}.business-central-eap7.latest.url"          "\$product_url_prefix/\${product_filename_common_prefix}-business-central-eap7.zip"
esac

appendProp "\${PRODUCT_NAME,,}.kie-server.ee7.latest.url" "\${product_url_prefix}/\${product_filename_common_prefix}-kie-server-ee7.zip"
appendProp "\${PRODUCT_NAME,,}.addons.latest.url"         "\${product_url_prefix}/\${product_filename_common_prefix}-add-ons.zip"
appendProp "\${PRODUCT_NAME}_VERSION"   \${product_version}
appendProp "KIE_VERSION"                \${kie_version}
appendProp "APPFORMER_VERSION"          \${appformer_version}
appendProp "ERRAI_VERSION"              \${errai_version}
appendProp "MVEL_VERSION"               \${mvel_version}

#Additional properties for brew release
if [ "\${release_code}" != "bxms-nightly" ]; then
    #append the other properties per qe's requirement
    appendProp "build.config" \${product_url_prefix}/${IP_CONFIG_FILE}
    appendProp \$prod_public_version_properties_name \${prod_public_version_properties_value}
    appendProp "\${PRODUCT_NAME,,}.maven.repo.latest.url"     "\$product_url_prefix/\${product_filename_common_prefix}-maven-repository.zip"
    appendProp "\${PRODUCT_NAME,,}.sources.repo.latest.url"   "\$product_url_prefix/\${prod_sources_name}.zip"

    sed -e "s=\${rcm_staging_base}/\${PRODUCT_NAME,,}=\${rcm_candidate_base}/\${PRODUCT_NAME}=g" \
    \${prod_properties_name} > \${prod_candidate_properties_name}
fi
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-generate-qe-properties") {

    // Sets a description for the job.
    description("This job is responsible for staging the Brew release deliverable to the RCM staging area.")

    // Allows to parameterize the job.
    parameters {
        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "RHDM",
                description = "Specify product name to switch between configurations.")
    }

    // Adds build steps to the jobs.
    steps {
        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
        // Inject environment variables for staging paths
        environmentVariables {
            propertiesFile("/tmp/prod_staging_path")
        }
    }
    wrappers {
        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

    }

    publishers {
        publishOverSsh {
            server('publish server') {
                // Adds a target server.
                verbose(true)
                // Adds a transfer set.
                transferSet {
                    // Sets the files to upload to a server.
                    sourceFiles('${IP_CONFIG_FILE}, ${prod_properties_name}, ${prod_candidate_properties_name}')

                    // Sets the destination folder.
                    remoteDirectory('${properties_staging_path}')
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)
