job("sample-init-jira") {

    description("This is jira initialization job. This job is responsible for creation of Jira ticket.")

    label("pvt-static")

    wrappers {
        preBuildCleanup()
    }

    steps {
        shell('echo "Do Jira ticket.')
    }
}