import org.jboss.bxms.jenkins.JobTemplate

String shellScript = """ip-tooling/MEAD_check_artifact.sh \$brew_tag /jboss-prod/m2/\${jenkins_cache_repo} 2>&1 | tee /tmp/mead_check.log
sed "/redhat-/d" /tmp/mead_check.log
echo "JOB DONE"
"""
// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-locate-import-list") {

    // Sets a description for the job.
    description("This job is responsible for finding brew missing jars.")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
    publishers {
        postBuildTask {
            //TODO
            task('JOB DONE', "echo 'send an email notification and trigger automation import'")
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, RELEASE_CODE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition)