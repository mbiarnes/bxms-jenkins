import org.jboss.bxms.jenkins.JobTemplate

// Init Brew build script.
def shellScript = """
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

UNBLOCK=1 BREWCHAIN=1 CFG=./${IP_CONFIG_FILE} POMMANIPEXT=brms-bom make -f  \${makefile} \${product_root_component} 2>&1| tee b.log 

brewchain_build_url=`grep 'build: Watching task ID:' b.log`
brewchain_build_url=\${brewchain_build_url##INFO*\\(}
brewchain_build_url=\${brewchain_build_url% *\\)}
brewchain_build_url=`echo -e "\${brewchain_build_url}" | tr -d '[:space:]'`

echo "Brewchain Build URL: \$brewchain_build_url"

sed -i '/^brewchain_build_url=/d' ${CI_PROPERTIES_FILE} && echo "brewchain_build_url=\'\$brewchain_build_url\'" >>${CI_PROPERTIES_FILE}
ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Brew chainbuild is trigger at:\${brewchain_build_url}" -f

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
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)