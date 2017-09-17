import org.jboss.bxms.jenkins.JobTemplate

// Update product JIRA script
def shellScript = """echo "The product version is \$product_version and the release milestone is \$release_milestone."
kinit -k -t \${HOME}/bxms-release.keytab bxms-release/prod-ci@REDHAT.COM
python ip-tooling/release-ticketor.py --user mw-prod-ci --password ds54sdfs54df \
    --headless \$product_version.GA \$cutoff_date \$product_version.\$release_milestone 2>&1 | tee /tmp/release-ticketor-output

sed -i '/^resolve_issue_list=/d' ${CI_PROPERTIES_FILE} \
    && echo "resolve_issue_list="`cat /tmp/release-ticketor-output | grep https://url.corp.redhat.com` >> ${CI_PROPERTIES_FILE}
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-update-product-jira") {

    // Sets a description for the job.
    description("This job is responsible for updating the community JIRA tickets associated with this release.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)