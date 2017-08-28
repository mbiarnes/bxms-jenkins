import org.jboss.bxms.jenkins.JobTemplate

// PVT script.
String shellScript = """cd pvt
/jboss-prod/tools/maven-3.3.9-prod/bin/mvn -Dmaven.repo.local=/jboss-prod/m2/bxms-dev-repo \
    surefire-report:report -B -Dproduct.config=\${bpms_smoketest_cfg} -Dproduct.version=\${product_deliver_version} \
    -Dproduct.target=\${product_deliver_version} clean package

cd generic/
#TODO update the pvt report to share folder
sed -i '/^bpms_pvt_summary_adoc=/d' ${CI_PROPERTIES_FILE} \
    && echo "bpms_pvt_summary_adoc=`pwd`/`find . -name 'pvt_handover_summary*.adoc'`">> ${CI_PROPERTIES_FILE}
sed -i '/^bpms_pvt_report_html=/d' ${CI_PROPERTIES_FILE} \
    && echo "bpms_pvt_report_html=`pwd`/`find . -name 'pvt_report*.html'`">>${CI_PROPERTIES_FILE}
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-pvt-test-bpms") {

    // Sets a description for the job.
    description("This job is responsible for executing product validation tests.")

    multiscm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling")
            }

            // Specify the branches to examine for changes and to build.
            branch("master")
            // Adds additional behaviors.
            extensions {

                // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                relativeTargetDirectory('ip-tooling')
            }
        }
        git {
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
    wrappers {
        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
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
        postBuildTask {
            task('BUILD SUCCESS', "ip-tooling/jira_helper.py -c ${IP_CONFIG_FILE} -a '${PRODUCT_NAME} PVT test in completed: \${BUILD_URL}' -f")
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)