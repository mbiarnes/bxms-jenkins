import org.jboss.bxms.jenkins.JobTemplate

// Create product tag script
String shellScript = """
#unset Jenkins WORKSPACE variable
unset WORKSPACE
RELEASE_TAG=\${product_name}-\${product_version}.\${release_milestone} LOCAL=1 CFG=./${IP_CONFIG_FILE} \
    REPO_GROUP=MEAD make POMMANIPEXT=brms-bom -f \${makefile} \${product_root_component} 2>&1| tee b.log 

sed -i '/^product_tag=/d' ${CI_PROPERTIES_FILE} && echo "product_tag=BxMS-\${product_version}.\${release_milestone}" >> ${CI_PROPERTIES_FILE}
echo "Product tag has been completed. Tag name: BxMS-\${product_version}.\${release_milestone}"
#todo need to verify if all tag is created succesfully
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-create-product-tag") {

    // Sets a description for the job.
    description("This job is responsible for creating the product milestone tags for this release in the format of ProductVersion.Milestone.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)