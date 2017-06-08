// PVT script.
String shellScript = '''cd pvt
/mnt/maven-3.2.3-prod/bin/mvn clean site surefire-report:report -B -Dproduct.config=${brms_smoketest_cfg}  -Dproduct.version=${product_version}.${release_milestone}

cd generic/

sed -i '/^brms_pvt_summary_adoc=/d' ${HOME}/${release_prefix}-jenkins-ci.properties && echo "brms_pvt_summary_adoc=`pwd`/`find . -name 'pvt_handover_summary*.adoc'`">>${HOME}/${release_prefix}-jenkins-ci.properties
sed -i '/^brms_pvt_report_html=/d' ${HOME}/${release_prefix}-jenkins-ci.properties && echo "brms_pvt_report_html=`pwd`/`find . -name 'pvt_report*.html'`">>${HOME}/${release_prefix}-jenkins-ci.properties
'''

// Creates or updates a free style job.
job("sample-pvt-test-brms") {

    // Sets a description for the job.
    description("This job is responsible for executing product validation tests.")

    // Label which specifies which nodes this job can run on.
    label("pvt-static")

    // Adds pre/post actions to the job.
    wrappers {

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()
    }

    // Adds environment variables to the build.
    environmentVariables {

        // Adds environment variables from a properties file.
        propertiesFile('${HOME}/brms-64-jenkins-ci.properties')

        // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
        keepBuildVariables(true)

        // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
        keepSystemVariables(true)
    }

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