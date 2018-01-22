// This job-DSL createsa a job that monitor the bxms ci message
job('rhba-ci-message-monitor'){
  description("This DSL generates a job that monitor bxms ci message")

  // both tag and label will trigger the job
  triggers{
    ciBuildTrigger {
        selector("label='rhba-ci'")
        providerName('CI Publish')
    }
  }

  // print message
  steps{
      shell('''echo "I am trigger by a CI message"
echo "$CI_MESSAGE"''')
    }

}
