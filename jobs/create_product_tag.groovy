import org.jboss.bxms.jenkins.JobTemplate

// Create product tag script
String shellScript = '''#kinit -k -t ${HOME}/host-host-8-172-124.host.centralci.eng.rdu2.redhat.com.keytab host/host-8-172-124.host.centralci.eng.rdu2.redhat.com@REDHAT.COM

git clone  https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git ip-tooling

#unset Jenkins WORKSPACE variable
unset WORKSPACE
RELEASE_TAG=BxMS-${product_version}.${release_milestone} LOCAL=1 MVN_LOCAL_REPO=/mnt/jboss-prod/m2/bxms-6.4-milestone CFG=./${release_prefix}.cfg REPO_GROUP=MEAD make POMMANIPEXT=brms-bom -f Makefile.BRMS bxms-maven-repo-root  2>&1| tee b.log 


sed -i '/^product_tag=/d' ${HOME}/${release_prefix}-jenkins-ci.properties && echo "product_tag=BxMS-${product_version}.${release_milestone}" >>${HOME}/${release_prefix}-jenkins-ci.properties
#sed -i '/^task_id=/d' ${HOME}/${release_prefix}-jenkins-ci.properties && echo "task_id=$brewchain_build_url" >>${HOME}/${release_prefix}-jenkins-ci.properties
echo "Product tag has been completed. Tag name: BxMS-${product_version}.${release_milestone}"
'''

// Creates or updates a free style job.
job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-create-product-tag") {

    // Sets a description for the job.
    description("This job is responsible for creating the product milestone tags for this release in the format of ProductVersion.Milestone.")

    scm {

        // Adds a Git SCM source.
        git {

            // Adds a remote.
            remote {

                // Sets the remote URL.
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }

            // Specify the branches to examine for changes and to build.
            branch('${ip_config_branch}')
        }
    }

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script (defaults to sh, but this is configurable) for building the project.
        shell(shellScript)
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)