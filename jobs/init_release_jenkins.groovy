import org.jboss.bxms.jenkins.JobTemplate

// Shell script
String script = """
ip-tooling/utility/init-releaseci-properties.sh ${IP_CONFIG_FILE}

sed -i '/^maven_repo_url=/d' \${HOME}/\${release_prefix}-jenkins-ci.properties && echo "maven_repo_url=http://10.8.172.174/m2/bxms-7-milestone" >>\${HOME}/\${release_prefix}-jenkins-ci.properties

cat \${HOME}/brms-jenkins-ci.properties

echo "\${product_name} \${product_version} \${release_milestone} Release"

echo "temporary: scp brms-jenkins-ci.properties to bxms nightly servers"

scp -i \${HOME}/.ssh/jenkins_rsa \${HOME}/brms-jenkins-ci.properties 10.8.173.97:~/
scp -i \${HOME}/.ssh/jenkins_rsa \${HOME}/brms-jenkins-ci.properties 10.8.172.252:~/
scp -i \${HOME}/.ssh/jenkins_rsa \${HOME}/brms-jenkins-ci.properties 10.8.172.182:~/
scp -i \${HOME}/.ssh/jenkins_rsa \${HOME}/brms-jenkins-ci.properties 10.8.172.25:~/
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-init-release-jenkins") {

    // Sets a description for the job.
    description("This is the ${PRODUCT_NAME} release initialization job. This job is responsible for preparation of ${CI_PROPERTIES_FILE} file.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(script)
    }
}

JobTemplate.addIpToolingScmConfiguration(jobDefinition)