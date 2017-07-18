import org.jboss.bxms.jenkins.JobTemplate

String script = """if [ '${IP_CONFIG_FILE}' == 'brms-64.cfg' ];
then
    ip-tooling/MEAD_check_artifact.sh jb-bxms-6.4-build /mnt/jboss-prod/m2/bxms-6.4-milestone 2>&1 | tee mead_check.log
elif [ '${IP_CONFIG_FILE}' == 'brms.cfg' ]; 
then
    ip-tooling/MEAD_check_artifact.sh jb-bxms-7.0-maven-build /mnt/jboss-prod/m2/bxms-7-milestone 2>&1 | tee mead_check.log
fi
cat mead_check.log
"""
// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-locate-import-list") {

    // Sets a description for the job.
    description("This job is responsible for finding brew missing jars.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(script)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)