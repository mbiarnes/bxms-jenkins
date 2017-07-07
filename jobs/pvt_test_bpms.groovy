import org.jboss.bxms.jenkins.JobTemplate

// PVT script.
String shellScript = '''cd pvt
/mnt/maven-3.2.3-prod/bin/mvn clean site surefire-report:report -B -Dproduct.config=${bpms_smoketest_cfg}  -Dproduct.version=${product_version}.${release_milestone} -Dproduct.target=${product_version}.GA

cd generic/

sed -i '/^bpms_pvt_summary_adoc=/d' ${HOME}/${release_prefix}-jenkins-ci.properties && echo "bpms_pvt_summary_adoc=`pwd`/`find . -name 'pvt_handover_summary*.adoc'`">>${HOME}/${release_prefix}-jenkins-ci.properties
sed -i '/^bpms_pvt_report_html=/d' ${HOME}/${release_prefix}-jenkins-ci.properties && echo "bpms_pvt_report_html=`pwd`/`find . -name 'pvt_report*.html'`">>${HOME}/${release_prefix}-jenkins-ci.properties
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-pvt-test-bpms") {

    // Sets a description for the job.
    description("This job is responsible for executing product validation tests.")

    scm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://github.com/project-ncl/pvt")
            }

            // Adds additional behaviors.
            extensions {

                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                relativeTargetDirectory("pvt")
            }
            // Specify the branches to examine for changes and to build.
            branch('*/master')
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    // Adds post-build actions to the job.
    publishers {

        //Archives artifacts with each build.
        archiveArtifacts('pvt/generic/*.html,pvt/generic/*.adoc')
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)