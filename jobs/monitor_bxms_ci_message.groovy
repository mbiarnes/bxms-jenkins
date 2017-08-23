// This job-DSL createsa a job that monitor the bxms ci message
job('utility/monitoring-bxms-CI-message'){
  description("This DSL generates a job that monitor bxms ci message")

  /*environmentVariables {
      propertiesFile('${HOME}/${release_prefix}-jenkins-ci.properties')
      keepSystemVariables(true)
      keepBuildVariables(true)
  }*/

  // both tag and label will trigger the job
  triggers{
    ciBuildTrigger {
        selector("label='bxms-ci'")
        providerName('default')
    }
  }

  // print message
  steps{
      shell('''echo "I am trigger by a CI message"
echo "$CI_MESSAGE"''')
    }

   /* send irc message. Currently connection failed
   publishers {
        irc {
            	channel('#prod-bxms')
            	setNotifyOnBuildStarts(true)
            	strategy('ALL')
            	notificationMessage('Default')
        	}
        }*/
        
}
