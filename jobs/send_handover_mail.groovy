import org.jboss.bxms.jenkins.JobTemplate

// reviewNotificationMail.
def mailContent = """
"""

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-release-pipeline/${PRODUCT_NAME}-send-handover-mail") {

    // Sets a description for the job.
    description("This job is responsible for sending the PR review email to the team.")

    // Adds post-build actions to the job.
    publishers {

        // Sends customizable email notifications.
        extendedEmail {

            // Adds email addresses that should receive emails.
            recipientList('pszubiak@redhat.com')

            // Adds e-mail addresses to use in the Reply-To header of the email.
            replyToList('bxms-prod@redhat.com')

            // Sets the default email subject that will be used for each email that is sent.
            defaultSubject('${product_name} ${product_version} ${release_milestone}  Release Handover Review')

            // Sets the default email content that will be used for each email that is sent.
            defaultContent(mailContent)

            // Sets the content type of the emails sent after a build.
            contentType('text/html')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)