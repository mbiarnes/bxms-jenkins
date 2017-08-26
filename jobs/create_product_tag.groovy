import org.jboss.bxms.jenkins.JobTemplate

// Create product tag script
String shellScript = """#unset Jenkins WORKSPACE variable
unset WORKSPACE
RELEASE_TAG=\${product_name}-\${product_version}.\${release_milestone} LOCAL=1 CFG=./\${release_prefix}.cfg REPO_GROUP=MEAD make POMMANIPEXT=brms-bom -f ${IP_MAKEFILE} ${PRODUCT_ROOT_COMPNENT} 2>&1| tee b.log 

sed -i '/^product_tag=/d' \${HOME}/\${release_prefix}-jenkins-ci.properties && echo "product_tag=BxMS-\${product_version}.\${release_milestone}" >> \${HOME}/\${release_prefix}-jenkins-ci.properties
echo "Product tag has been completed. Tag name: BxMS-\${product_version}.\${release_milestone}"
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-create-product-tag") {

    // Sets a description for the job.
    description("This job is responsible for creating the product milestone tags for this release in the format of ProductVersion.Milestone.")

    // Allows to parameterize the job.
    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "ip_config_branch", defaultValue = "master", description = "IP Config branch, commit id or tag to clone")
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition, '${ip_config_branch}')