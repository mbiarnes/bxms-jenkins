import org.jboss.bxms.jenkins.JobTemplate

// Init Brew build script.
def shellScript = """
#Enable keytab authentication.
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

BREWCHAIN=1 CFG=./${IP_CONFIG_FILE} POMMANIPEXT=brms-bom make -f  \${makefile} \${product_root_component} 2>&1| tee b.log 

brewchain_build_url=`grep 'build: Watching task ID:' b.log`
brewchain_build_url=\${brewchain_build_url##INFO*\\(}
brewchain_build_url=\${brewchain_build_url% *\\)}

echo "Brewchain Build URL: \$brewchain_build_url"

sed -i '/^brewchain_build_url=/d' ${CI_PROPERTIES_FILE} && echo "brewchain_build_url=\$brewchain_build_url" >>${CI_PROPERTIES_FILE}
echo "JOB DONE"
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-start-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for initialising the Brew chain build.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
    publishers {
        postBuildTask {
            task('JOB DONE', "ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -t \${release_estimation} \${release_estimation} -f")
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)