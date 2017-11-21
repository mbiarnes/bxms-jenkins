import org.jboss.bxms.jenkins.JobTemplate

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-brew-repo-regen") {

    // Sets a description for the job.
    description("This job is responsible for finding brew missing jars.")

    // Adds build steps to the jobs.
    steps {
        ciGenerateBrewRepoBuilder {
            // Tag name to generate repo for.
            tag("jb-bxms-7.0-maven-build")
            // BrewHUB url for Brew operations.
            brewhuburl("https://brewhub.engineering.redhat.com/brewhub")
            // Principal name to use for authentication.
            principal("CI/ci-user.ci-bus.lab.eng.rdu2.redhat.com@REDHAT.COM")
            // Path name for keytab used in authentication.
            keytab("\${JENKINS_HOME}/plugins/redhat-ci-plugin/ci-user.keytab")
            // Do not fail the build step if the tagging operation fails.
            ignore(false)
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)