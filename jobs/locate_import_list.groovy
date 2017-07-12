import org.jboss.bxms.jenkins.JobTemplate

String script = """
rm -rf ip-tooling
git clone  https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git ip-tooling
if ${IP_CONFIG_FILE} == 'brms-64.cfg';
then
    brew_tag = 'jb-bxms-6.4-build'
    nfs_repo_cache= '/mnt/jboss-prod/m3/bxms-6.4-milestone' 
elif ${IP_CONFIG_FILE} == 'brms.cfg'; 
    brew_tag = 'jb-bxms-7.0-maven-build'
    nfs_repo_cache= '/mnt/jboss-prod/m2/bxms-7.0-milestone' 
fi

ip-tooling/MEAD_check_artifact.sh $brew_tag $nfs_repo_cache 2>&1 | tee mead_check.log
cat mead_check.log
"""
// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-locate-import-list") {

    // Sets a description for the job.
    description("This job is responsible for finding brew missing jars .")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(script)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)