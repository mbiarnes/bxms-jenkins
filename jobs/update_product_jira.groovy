import org.jboss.bxms.jenkins.JobTemplate

// Update product JIRA script
String shellScript = '''echo "The product version is $product_version and the release milestone is $release_milestone."
python ./ip-tooling/release-ticketor.py --headless $product_version.GA $cutoff_date $product_version.$release_milestone 2>&1 | tee /tmp/release-ticketor-output
sed -i '/^resolve_issue_list=/d' ${HOME}/${release_prefix}-jenkins-ci.properties
echo "resolve_issue_list="`cat /tmp/release-ticketor-output | grep https://url.corp.redhat.com` >>${HOME}/${release_prefix}-jenkins-ci.properties
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-update-product-jira") {

    // Sets a description for the job.
    description("This job is responsible for updating the community JIRA tickets associated with this release.")

    // Allows a job to check out sources from multiple SCM providers.
    multiscm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch("master")
        }

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch("master")
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)