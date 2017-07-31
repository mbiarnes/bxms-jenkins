import org.jboss.bxms.jenkins.JobTemplate


// sed command
def command = null

// temp workaround for bpms
def mavenToStageBpmsCommand = null

// Prepare properties command
if (PRODUCT_NAME == "intpack-fuse63-bxms64") {

    command = 'sed -e \'s=rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-brms=download.devel.redhat.com/devel/candidates/INTPACK=g\' ${release_prefix}-deliverable-list-staging.properties >> ${release_prefix}-deliverable-list.properties'

    mavenToStageBpmsCommand = ''
} else {

    command = 'sed -e \'s=rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-brms=download.devel.redhat.com/devel/candidates/BRMS=g\' -e \'s=rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging/jboss-bpmsuite=download.devel.redhat.com/devel/candidates/BPMS=g\' ${release_prefix}-deliverable-list-staging.properties >> ${release_prefix}-deliverable-list.properties\n' +
            '\n' +
            '\n' +
            'sed -i \'/^ip_brms_brew_task_url=/d\' ${HOME}/${release_prefix}-jenkins-ci.properties && echo "ip_brms_brew_task_url=https://brewweb.engineering.redhat.com/brew/taskinfo?taskID=${task_id}">>${HOME}/${release_prefix}-jenkins-ci.properties\n' +
            '\n' +
            'sed -i \'/^build.config=/d\' ${HOME}/${release_prefix}-deliverable-list-staging.properties && echo "build.config=${rcm_stage_base}/jboss-bpmsuite/${bpms_product_name}-${product_version}.${release_milestone}/${release_prefix}.cfg">>${HOME}/${release_prefix}-deliverable-list-staging.properties\n' +
            'sed -i \'/^DROOLSJBPM_VERSION=/d\' ${HOME}/${release_prefix}-deliverable-list-staging.properties && echo "DROOLSJBPM_VERSION=${kie_version}">>${HOME}/${release_prefix}-deliverable-list-staging.properties\n' +
            'sed -i \'/^BXMS_VERSION=/d\' ${HOME}/${release_prefix}-deliverable-list-staging.properties && echo "BXMS_VERSION=${product_artifact_version}">>${HOME}/${release_prefix}-deliverable-list-staging.properties\n' +
            '\n' +
            '\n' +
            'cp ${HOME}/${release_prefix}-deliverable-list-staging.properties ${release_prefix}-deliverable-list-staging.properties'

    mavenToStageBpmsCommand = 'ip-tooling/maven-to-stage.py --version=${product_artifact_version} --override-version ${product_version} \\\n' +
            "   --deliverable \${release_prefix}-release/${BPMS_DELIVERABLE_LIST_FILE} --maven-repo \${maven_repo_url} \\\n" +
            '   --output ${bpms_product_name}-${product_version}.${release_milestone}\\\n' +
            '   --release-url=${rcm_stage_base}/jboss-bpmsuite/${bpms_product_name}-${product_version}.${release_milestone} \\\n' +
            '   --output-deliverable-list ${HOME}/${release_prefix}-deliverable-list-staging.properties'
}

// Staging script.
def shellScript = """# Disable bash tracking mode, too much noise.
#set +x
if [ ! -z \$CI_MESSAGE ];then
name=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['name']"` 1>/dev/null
version=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['version']"` 1>/dev/null
release=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['release']"` 1>/dev/null
task_id=`echo \$CI_MESSAGE| python -c "import sys, json; print json.load(sys.stdin)['build']['task_id']"` 1>/dev/null

fi
maven_repo_url="http://download.eng.bos.redhat.com/brewroot/packages/\${name}/\${version}/\${release}/maven/"

echo \$maven_repo_url

#Uploading to rcm staging folder
ip-tooling/maven-to-stage.py --version=\${product_artifact_version} --override-version \${product_version} \\
   --deliverable \${release_prefix}-release/\${release_prefix}-deliverable.properties --maven-repo \${maven_repo_url} \\
   --output \${brms_product_name}-\${product_version}.\${release_milestone} \\
   --release-url=\${rcm_stage_base}/jboss-brms/\${brms_product_name}-\${product_version}.\${release_milestone} --output-deliverable-list \${HOME}/\${release_prefix}-deliverable-list-staging.properties
   
${mavenToStageBpmsCommand}

cp \${HOME}/\${release_prefix}-deliverable-list.properties \${release_prefix}-deliverable-list-staging.properties

${command}
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-stage-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for staging the Brew release deliverables to the RCM staging area.")

    // Allows to parameterize the job.
    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "name", defaultValue = null, description = "Brew task name")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "version", defaultValue = null, description = "Brew task version")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "release", defaultValue = null, description = "Brew task release")

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "task_id", defaultValue = null, description = "Brew task id")
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
                        sourceFiles('${release_prefix}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${product_stage_folder}/${product_name}-${product_version}/')
                    }

                } else {

                    // Adds a target server.
                    verbose(true)

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${brms_product_name}-${product_version}.${release_milestone}/*.*')

                        // Sets the destination folder.
                        remoteDirectory('${brms_stage_folder}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${bpms_product_name}-${product_version}.${release_milestone}/*.*')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_stage_folder}')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/')
                    }

                    // Adds a transfer set.
                    transferSet {

                        // Sets the files to upload to a server.
                        sourceFiles('${release_prefix}-deliverable-list*.properties')

                        // Sets the destination folder.
                        remoteDirectory('${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/')
                    }
                }
            }
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)