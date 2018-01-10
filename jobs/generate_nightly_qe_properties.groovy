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
prod_properties_name=\${PRODUCT_NAME,,}-\${build_date}.properties
product_nightly_path=\${jenkins_cache_url}/\${jenkins_cache_repo}/org/kie/rhap/\${PRODUCT_NAME,,}

if [ ! -f \$prod_properties_name ]; then
    touch \$prod_properties_name
fi

case "\${PRODUCT_NAME}" in
    RHBAS )
        product_nightly_path=\${jenkins_cache_url}/\${jenkins_cache_repo}/org/kie/rhap/\${PRODUCT_NAME,,}
        product_version=\${product1_version}
        appendProp "\${PRODUCT_NAME,,}.business-central.standalone.latest.url"    "\${product_nightly_path}/\${product_version}.redhat-\${build_date}/\${product_version}.redhat-\${build_date}-business-central-standalone.zip"
        appendProp "\${PRODUCT_NAME,,}.business-central-eap7.latest.url"          "\${product_nightly_path}/\${product_version}.redhat-\${build_date}/\${product_version}.redhat-\${build_date}-business-central-eap7.zip"
    ;;
    RHDM )
        product_version=\${product2_version}
        appendProp "\${PRODUCT_NAME,,}.decision-central.standalone.latest.url"    "\${product_nightly_path}/\${product_version}.redhat-\${build_date}/\${product_version}.redhat-\${build_date}-decision-central-standalone.zip"
        appendProp "\${PRODUCT_NAME,,}.decision-central-eap7.latest.url"          "\${product_nightly_path}/\${product_version}.redhat-\${build_date}/\${product_version}.redhat-\${build_date}-decision-central-eap7.zip"
    ;;
esac
appendProp "\${PRODUCT_NAME,,}.kie-server.ee7.latest.url" "\${product_nightly_path}/\${product_version}.redhat-\${build_date}/\${product_version}.redhat-\${build_date}-kie-server-ee7.zip"
appendProp "\${PRODUCT_NAME,,}.addons.latest.url"         "\${product_nightly_path}/\${product_version}.redhat-\${build_date}/\${product_version}.redhat-\${build_date}-add-ons.zip"
appendProp "KIE_VERSION"                                    \${kie_version}
appendProp "\${PRODUCT_NAME}_VERSION"                     \${product_version}
#appendProp "\${PRODUCT_NAME,,}.maven.repo.latest.url"     "\${product_nightly_path}/\${product_version}.redhat-\${build_date}.pom"
#appendProp "\${PRODUCT_NAME,,}.sources.repo.latest.url"   "\${product_nightly_path}/\${product_version}.redhat-\${build_date}-scm-source.zip"
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
