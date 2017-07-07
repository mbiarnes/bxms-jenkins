import org.jboss.bxms.jenkins.JobTemplate

// Init Brew build script.
def shellScript = """# Reset the build_counter and brew_build_url.
export `grep "build_counter" \${HOME}/\${release_prefix}-jenkins-ci.properties`
export `grep "ip_config_sha" \${HOME}/\${release_prefix}-jenkins-ci.properties`

latest_ip_config_sha=`grep "ip.config.sha=" \${release_prefix}.cfg`
latest_ip_config_sha=\${latest_ip_config_sha##*ip.config.sha=}
latest_ip_config_sha=\${latest_ip_config_sha%%,*}
echo "latest_ip_config_sha=\$latest_ip_config_sha"
echo "\$ip_config_sha"
if [ "\$ip_config_sha" != "\$latest_ip_config_sha" ];then
  build_counter=0
fi
echo "current build counter:\$build_counter"
if [ \$build_counter -gt 3 ]
then
echo "The same Brew build has been tried 3 times， Please investigate the root cause."
exit 1
fi

#Enable keytab authentication.
kinit -k -t \${HOME}/host-host-8-172-124.host.centralci.eng.rdu2.redhat.com.keytab host/host-8-172-124.host.centralci.eng.rdu2.redhat.com@REDHAT.COM

git clone  https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git ip-tooling

UNBLOCK=1 BREWCHAIN=1 CFG=./\${release_prefix}.cfg POMMANIPEXT=brms-bom make -f  ${IP_MAKEFILE} ${PRODUCT_ROOT_COMPNENT}  2>&1| tee b.log 

brewchain_build_url=`grep 'build: Watching task ID:' b.log`
brewchain_build_url=\${brewchain_build_url##INFO*\\(}
brewchain_build_url=\${brewchain_build_url% *\\)}
#task_id=\${brewchain_build_url##*taskID=}

echo "Brewchain Build URL: \$brewchain_build_url"

build_counter=\$((\$build_counter+1))
sed -i '/^build_counter=/d' \${HOME}/\${release_prefix}-jenkins-ci.properties && echo "build_counter=\$build_counter" >>\${HOME}/\${release_prefix}-jenkins-ci.properties
#sed -i '/^brew_build_url=/d' \${HOME}/\${release_prefix}-jenkins-ci.properties && echo "brew_build_url=\$brew_build_url" >>\${HOME}/\${release_prefix}-jenkins-ci.properties


sed -i '/^brewchain_build_url=/d' \${HOME}/\${release_prefix}-jenkins-ci.properties && echo "brewchain_build_url=\$brewchain_build_url" >>\${HOME}/\${release_prefix}-jenkins-ci.properties
#sed -i '/^task_id=/d' \${HOME}/\${release_prefix}-jenkins-ci.properties && echo "task_id=\$brewchain_build_url" >>\${HOME}/\${release_prefix}-jenkins-ci.properties
echo "Congratulation Brew build is triggered!"
"""

// Creates or updates a free style job.
job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-start-brew-build") {

    // Sets a description for the job.
    description("This job is responsible for initialising the Brew chain build.")

    // Allows a job to check out sources from an SCM provider.
    scm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch('${ip_config_branch}')
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)