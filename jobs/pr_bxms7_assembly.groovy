def shell_script = '''wget http://git.app.eng.bos.redhat.com/git/integration-platform-tooling.git/plain/jssecacerts
export _KEYSTORE=`pwd`/jssecacerts
export MAVEN_OPTS="-Djavax.net.ssl.trustStore=${_KEYSTORE} -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.keyStore=${_KEYSTORE} -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.keyStoreType=jks -Xms512m -Xmx3096m -XX:MaxPermSize=1024m -Dgwt-plugin.localWorkers='3' -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit"

mvn  -Pbrms -DdependencyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.redhat-SNAPSHOT \\
\t-Denforce-skip=false -Dfull=true -DoverrideTransitive=false -Dproject.meta.skip=true \\
    -DpropertyManagement=org.jboss.brms.component.management:brms-dependency-management-all:7.0.0.redhat-SNAPSHOT \\
    -Drepo-reporting-removal=true -Dversion.override=7.0.0 -Dversion.suffix=redhat-SNAPSHOT -Dversion.suffix.snapshot=true \\
    -DversionOverride=true -DversionSuffixSnapshot=true -Dvictims.updates=offline -B -Dmaven.repo.local=/mnt/jboss-prod/m2/bxms-7-nightly -s /jboss-prod/m2/bxms-7-nightly-settings.xml clean package
'''
job('bxms7_assembly_codereview'){
    description("Monitor the code change in bxms-assembly")

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
                url("https://code.engineering.redhat.com/gerrit/ip")
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

           project("ip", "master")
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
        archiveArtifacts('brms-bpmsuite/*/target/*.zip')
    }
}

