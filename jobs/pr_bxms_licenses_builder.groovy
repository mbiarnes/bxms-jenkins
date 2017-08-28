def shell_script = '''export MAVEN_OPTS="-Xms512m -Xmx8096m -Dgwt-plugin.localWorkers='3' -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit"
/jboss-prod/tools/maven-3.3.9-prod/bin/mvn -Dmaven.repo.local=/jboss-prod/m2/bxms-dev-repo clean install
'''
job('bxms_licenses_builder_codereview'){
    description("Monitor the code change in bxms-licenses-builder")

    parameters {

        // Defines a simple text parameter, where users can enter a string value.
        stringParam(parameterName = "GERRIT_REFSPEC", defaultValue = "refs/heads/master", description = "Parameter passed by Gerrit code review trigger")

        stringParam(parameterName = "GERRIT_BRANCH", defaultValue = "master", description = "Parameter passed by Gerrit code review trigger")

    }

    scm {
        // Adds a Git SCM source.
        git {
            // Adds a remote.
            remote {
                // Sets the remote URL.
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-licenses-builder")
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

           project("bxms-licenses-builder", "ant:**")
           events {
               patchsetCreated()
           }
           configure { triggers ->
               triggers   <<  {
                   'serverName' 'code.engineering.redhat.com'
               }
           }
       }
   }
   label('bxms-nightly')
   
   // build steps 
   steps{
       shell(shell_script)
    }
   // clear workspace 
    wrappers {
        preBuildCleanup()
    }
    // Adds post-build actions to the job.
    publishers {
        //Archives artifacts with each build.
        archiveArtifacts('target/*.zip')
    }
}

