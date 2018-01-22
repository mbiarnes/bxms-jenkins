import org.jboss.bxms.jenkins.JobTemplate
String shellScript = """
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
remote_product_cfg_sha=\$(git log -1 --pretty="%H" ${IP_CONFIG_FILE})
cfg=${IP_CONFIG_FILE}
release_code=\${cfg%%.cfg}

function appendProp() {
    echo "Inject Properties:\$2"
    if [ -z "\$1" ] || [ -z "\$2" ];then
        echo "Properties value is empty"
        exit 1
    fi
    sed -i "/^\$1/d" ${CI_PROPERTIES_FILE} && echo "\$1=\$2" >> ${CI_PROPERTIES_FILE}
}
if [ "\${CLEAN_CONFIG}" = "true" ];then
    rm -vf /jboss-prod/config/\${release_code}-*.*
    rm -vf \${CI_PROPERTIES_FILE}
fi

# Prepare properties for nightly build
if [ "${RELEASE_CODE}" == "bxms-nightly" ]; then
    build_date=\$(date -u +'%Y%m%d')
    sed -i "s#-SNAPSHOT#-\${build_date}#g" ${IP_CONFIG_FILE}
fi

#If build new versions, then remove the jenkins properties files
if [ -f ${CI_PROPERTIES_FILE} ];then
    new_version="`grep 'product1_version=' ${IP_CONFIG_FILE}`"
    new_milestone="`grep 'product1_milestone=' ${IP_CONFIG_FILE}`"
    old_version="`grep 'product1_version=' ${CI_PROPERTIES_FILE}`"
    old_milestone="`grep 'product1_milestone=' ${CI_PROPERTIES_FILE}`"

    export `grep "product_cfg_sha" ${CI_PROPERTIES_FILE}`

    echo "local:\$product_cfg_sha, remote: \$remote_product_cfg_sha"
    if [ "\${new_version}\${new_milestone}" != "\${old_version}\${old_milestone}" ] || \
       [ "\${product_cfg_sha}" != "\${remote_product_cfg_sha}" ]
    then
        rm -vf ${CI_PROPERTIES_FILE}
    fi
fi
if [ ! -f ${CI_PROPERTIES_FILE} ];then
    #Loading env from cfg file
    python ip-tooling/jenkins_ci_property_loader.py -m bxms-jenkins/streams/${RELEASE_CODE}/config/properties-mapping.template -i ${IP_CONFIG_FILE} -o ${CI_PROPERTIES_FILE}
    appendProp "product_cfg_sha" \$remote_product_cfg_sha
    appendProp "ci_properties_file" ${CI_PROPERTIES_FILE}
    appendProp "build_cfg" ${IP_CONFIG_FILE}
fi
source ${CI_PROPERTIES_FILE}
product1_shipped_file_deliver_version=\${product1_milestone_version}
product2_shipped_file_deliver_version=\${product2_milestone_version}
#Uploading to rcm staging folder
if [ "\${milestone:0:2}" == "CR" ];then
    product1_shipped_file_deliver_version=\${product1_version}\${availability}
    product2_shipped_file_deliver_version=\${product2_version}\${availability}
fi
appendProp "product1_shipped_file_deliver_version" \$product1_shipped_file_deliver_version
appendProp "product2_shipped_file_deliver_version" \$product2_shipped_file_deliver_version

#Use kerbose to create the release JIRA
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -t \${release_estimation} \${release_estimation} -f |tee /tmp/jira.log
jira_id=`tail -n 2 /tmp/jira.log |head -n 1`
jira_id=\${jira_id/Selected Result:/}
echo "https://projects.engineering.redhat.com/browse/\$jira_id"
appendProp "release_jira_id" \$jira_id
if [ "${RELEASE_CODE}" == "bxms-nightly" ]; then
    appendProp "build_date" "\${build_date}"
fi
"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-init-release") {

    // Sets a description for the job.
    description("This is the ${RELEASE_CODE} release initialization job. This job is responsible for preparation of ${CI_PROPERTIES_FILE} file.")
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

JobTemplate.addIpToolingScmConfiguration(jobDefinition,GERRIT_BRANCH , GERRIT_REFSPEC)
