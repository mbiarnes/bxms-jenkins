import org.jboss.bxms.jenkins.JobTemplate

// Init Brew build script.
def shellScript = """
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM

UNBLOCK=1 BREWCHAIN=1 CFG=./${IP_CONFIG_FILE} POMMANIPEXT=brms-bom make -f  \${makefile} \${product_root_component} 2>&1| tee b.log 

brewchain_build_url=`grep 'build: Watching task ID:' b.log`
brewchain_build_url=`python -c "import sys,re
print re.match('^.*(https.*\\d+).*\$', '\$brewchain_build_url').group(1)
"`

echo "Brewchain Build URL: \$brewchain_build_url"

sed -i '/^brewchain_build_url=/d' ${CI_PROPERTIES_FILE} && echo "brewchain_build_url=\$brewchain_build_url" >>${CI_PROPERTIES_FILE}
sed -i '/^brew_status=/d' ${CI_PROPERTIES_FILE} && echo "brew_status=running" >>${CI_PROPERTIES_FILE}
ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a "Brew chainbuild is trigger at:\${brewchain_build_url}" -f

"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-start-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for initialising the Brew chain build.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, RELEASE_CODE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)