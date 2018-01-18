import org.jboss.bxms.jenkins.JobTemplate

// reviewNotificationMail.
def mailContent = """Hello,

\$product1 \${product1_milestone_version} is now available, the handover can be found below:

\$rcm_candidate_base/\$product1_name/\$product1_name-\${product1_milestone_version}/\${product1_lowcase}-handover.html

\$product2 \${product2_milestone_version} is now available, the handover can be found below:

\$rcm_candidate_base/\$product2_name/\$product2_name-\${product2_milestone_version}/\${product2_lowcase}-handover.html

Kind regards,

BxMS Prod Team"""

// Creates or updates a free style job.
def jobDefinition = job("${RELEASE_CODE}-send-handover-mail") {

    // Sets a description for the job.
    description("This job is responsible for sending handover email to the team.")

    // Adds post-build actions to the job.
    publishers {

        // Sends customizable email notifications.
        extendedEmail {

            // Adds email addresses that should receive emails.
            recipientList('pszubiak@redhat.com')

            // Adds e-mail addresses to use in the Reply-To header of the email.
            replyToList('bxms-prod@redhat.com')

            // Sets the default email subject that will be used for each email that is sent.
            defaultSubject('${product1_name} ${product1_version} ${product1_milestone} is now available.')

            // Sets the default email content that will be used for each email that is sent.
            defaultContent(mailContent)

            // Sets the content type of the emails sent after a build.
            contentType('text/html')
        }
    }
    steps {
        shell("echo -e \"Exec node IP:\${OPENSTACK_PUBLIC_IP}\\n\"")
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE)
