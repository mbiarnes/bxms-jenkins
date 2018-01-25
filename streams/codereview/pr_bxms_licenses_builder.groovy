def shell_script = '''export MAVEN_OPTS="-Xms2g -Xmx16g -Dgwt-plugin.localWorkers='3' -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit"
export M3_HOME=/jboss-prod/tools/maven-3.3.9-prod
export PATH=$M3_HOME/bin:$PATH
build_date=\$(date --date="1 days ago" -u +'%Y%m%d')
mvn -Dversion.override=7.0.0.DR -Dversion.suffix=redhat-\${build_date} \\
    -DdependencyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.DR-redhat-\${build_date} \\
    -Dmaven.repo.local=/jboss-prod/m2/bxms-dev-repo clean install
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
   label('nightly-node')

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
