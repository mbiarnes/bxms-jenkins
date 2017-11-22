folder("bxms-test-release-pipline")
job('bxms-test-release-pipline/bxms_jenkins_teststream_masterbranch_codereview'){
   description("Monitor the code change in bxms-assembly")

    parameters {
        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "GERRIT_REFSPEC", defaultValue = "refs/heads/master", description = "Parameter passed by Gerrit code review trigger")
        stringParam(parameterName = "GERRIT_BRANCH", defaultValue = "master", description = "Parameter passed by Gerrit code review trigger")
    }

    // Adds environment variables to the build.
    environmentVariables {
        // Adds environment variables from a properties file.
        env ("RELEASE_CODE", "bxms-test")
        env ("CI_PROPERTIES_FILE", "/jboss-prod/config/bxms-test-ci.properties")
        env ("IP_CONFIG_FILE", "bxms-test.cfg")
    }

    scm {
        // Adds a Git SCM source.
        git {
            // Adds a remote.
            remote {
                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
                name("origin")
                refspec("+refs/heads/*:refs/remotes/origin/* \$GERRIT_REFSPEC")
            }

            // Specify the branches to examine for changes and to build.
            branch("\$GERRIT_BRANCH")
            extensions {
                choosingStrategy {
                    gerritTrigger()
                }
            }
        }
    }
    triggers{
        gerrit{
            project("bxms-jenkins", "ant:**")
            events {
                patchsetCreated()
            }
            //Using configuration block to handle unsupported field
            configure { triggers ->
                triggers   <<  {
                    'serverName' 'code.engineering.redhat.com'
                }
            }
        }
    }
   label('service-node')
   
   // build steps 
   steps{
        // shell script to check latest version of PME and update accordingly
       dsl {
           external ('streams/bxms-test/dsl/*')
           additionalClasspath 'src/main/groovy'
           //For SEED_JOB strategy, PR will create job in codereview/ folder instead of JENKINS_ROOT
           lookupStrategy 'SEED_JOB'
           // Specifies the action to be taken for job that have been removed from DSL scripts.
           removeAction('DELETE')
           // Specifies the action to be taken for views that have been removed from DSL scripts.
           removeViewAction('DELETE')
       }
    }
   // clear workspace 
    wrappers {
        preBuildCleanup()
    }
}

