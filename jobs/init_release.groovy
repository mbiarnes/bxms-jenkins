job('sample-init-release') {

    description('This is release initialization job.')

    environmentVariables {
            propertiesFile('${HOME}/brms-64-jenkins-ci.properties')
            keepBuildVariables(true)
    }


    label('pvt-static')

    wrappers {
        preBuildCleanup()
    }

    scm {
        git {
            remote {
                name('origin')
                url('https://code.engineering.redhat.com/gerrit/integration-platform-config.git/')
            }
            branch('master')
        }
    }

    steps {
        shell('echo "Hello!"')
    }
}