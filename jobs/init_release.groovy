job("sample-init-release") {


    description("This is release initialization job. This job is responsible for preparation of brms-64-jenkins-ci.properties file")

    label("pvt-static")

    wrappers {
        preBuildCleanup()
    }

    multiscm {
        git {
            remote {
                url("https://code.engineering.redhat.com/gerrit/integration-platform-config.git/")
            }
            branch("master")
        }

        git {
            remote {
                url("https://code.engineering.redhat.com/gerrit/integration-platform-tooling.git/")
            }
            branch("master")
        }
    }

    steps {
        shell('echo "Hello!"')
    }
}