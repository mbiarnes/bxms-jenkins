import java.util.logging.Logger

job('sample-init-release') {

    Logger logger = Logger.getLogger("")

    description('This is release initialization job. This job is responsible for preparation of ${HOME}/brms-64-jenkins-ci.properties file')

    label("pvt-static")

    wrappers {
        preBuildCleanup()
    }

    logger.info('Add SCM block')
    scm {
        git {
            remote {
                name('origin')
                url('https://code.engineering.redhat.com/gerrit/integration-platform-config.git/')
            }
            branch('master')
        }
    }

    logger.warn("Add steps")
    steps {
        shell('echo "Hello!"')
    }
}