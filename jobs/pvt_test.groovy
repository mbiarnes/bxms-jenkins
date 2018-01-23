import org.jboss.bxms.jenkins.JobTemplate

// PVT script.
String shellScript = """
echo -e "Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n"
case "\${PRODUCT_NAME}" in
    RHDM )
        prod_smoketest_cfg=\${product1_smoketest_cfg}
        prod_milestone_version=\${product1_milestone_version}
        prod_shipped_file_deliver_version=\${product1_shipped_file_deliver_version}
        prod_pvt_report_basename=\${product1_pvt_report_basename}
        ;;
    RHBAS )
        prod_smoketest_cfg=\${product2_smoketest_cfg}
        prod_milestone_version=\${product2_milestone_version}
        prod_shipped_file_deliver_version=\${product2_shipped_file_deliver_version}
        prod_pvt_report_basename=\${product2_pvt_report_basename}
        ;;
esac
git clone https://github.com/project-ncl/pvt.git
cd pvt
/jboss-prod/tools/maven-3.3.9-prod/bin/mvn -Dmaven.repo.local=\${dev_maven_repo} \
    surefire-report:report -B -Dproduct.config=\${prod_smoketest_cfg} -Dproduct.version=\${prod_milestone_version} \
    -Dproduct.target=\${prod_shipped_file_deliver_version} -Dreport.filepath=\${prod_pvt_report_basename} clean package

"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-pvt-test") {

    // Sets a description for the job.
    description("This job is responsible for executing product validation tests.")

    parameters {
        stringParam(parameterName = "PRODUCT_NAME", defaultValue = "RHDM",
                description = "Specify product name to switch between configurations.")
    }

    // Adds a Git SCM source.
    multiscm {

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

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }

    // Adds post-build actions to the job.
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
JobTemplate.addIpToolingScmConfiguration(jobDefinition,GERRIT_BRANCH , GERRIT_REFSPEC)
