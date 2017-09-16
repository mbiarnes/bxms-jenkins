import org.jboss.bxms.jenkins.JobTemplate

String shellScript = """
remote_product_cfg_sha=\$(git log -1 --pretty="%H" ${IP_CONFIG_FILE})
cfg=${IP_CONFIG_FILE}
release_prefix=\${cfg%%.cfg}

if [ "\${CLEAN_CONFIG}" = "true" ];then
    rm -vf /jboss-prod/config/\${release_prefix}-*.*
fi
#If build new versions, then remove the jenkins properties files
if [ -f ${CI_PROPERTIES_FILE} ];then
    new_version="`grep 'product_version=' ${IP_CONFIG_FILE}`"
    new_milestone="`grep 'release_milestone=' ${IP_CONFIG_FILE}`"
    old_version="`grep 'product_version=' ${IP_CONFIG_FILE}`"
    old_milestone="`grep 'release_milestone=' ${IP_CONFIG_FILE}`"
    
    export `grep "product_cfg_sha" ${CI_PROPERTIES_FILE}`
    
    echo "local:\$product_cfg_sha, remote: \$remote_product_cfg_sha"
    if [ "\${new_version}\${new_milestone}" != "\${old_version}\${old_milestone}" ] || \
       [ "\${product_cfg_sha}" != "\${remote_product_cfg_sha}" ]       
    then
        rm -vf \$${CI_PROPERTIES_FILE}
    fi
fi
if [ ! -f ${CI_PROPERTIES_FILE} ];then
    #Loading env from cfg file
    python ip-tooling/jenkins_ci_property_loader.py -i ${IP_CONFIG_FILE} -o ${CI_PROPERTIES_FILE}
    sed -i '/^product_cfg_sha=/d' ${CI_PROPERTIES_FILE} && echo "product_cfg_sha=\${remote_product_cfg_sha}" >> ${CI_PROPERTIES_FILE}    
fi
source ${CI_PROPERTIES_FILE}
#Use kerbose to create the release JIRA
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -t \${release_estimation} \${release_estimation} -f

"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-init-release") {

    // Sets a description for the job.
    description("This is the ${PRODUCT_NAME} release initialization job. This job is responsible for preparation of ${CI_PROPERTIES_FILE} file.")
    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        booleanParam(parameterName = "CLEAN_CONFIG", defaultValue = false, description = "WARNING, click this will force remove your release pipeline properties!")
    }
    // Label which specifies which nodes this job can run on.
    label("release-pipeline")

    // Adds pre/post actions to the job.

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
    // Adds post-build actions to the job.
}

JobTemplate.addIpToolingScmConfiguration(jobDefinition)
