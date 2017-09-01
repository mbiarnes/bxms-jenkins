import org.jboss.bxms.jenkins.JobTemplate

// reviewNotificationMail.
String mailContent = '''<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"> 
<html xmlns="http://www.w3.org/1999/xhtml"> 
    <head> 
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> 
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" /> 
        <title>Message Title</title> 
    </head> 
    <body class="jira" style="color: #333; font-family: Arial, sans-serif; font-size: 14px; line-height: 1.429"> 
        <table id="background-table" cellpadding="0" cellspacing="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #f5f5f5; border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt"> 
            <!-- header here --> 
            <tr> 
                <td id="header-pattern-container" style="padding: 0px; border-collapse: collapse; padding: 10px 20px"> 
                 </td> 
            </tr> 
            <tr> 
                <td id="email-content-container" style="padding: 0px; border-collapse: collapse; padding: 0 20px"> 
                    <table id="email-content-table" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0; border-collapse: separate"> 
                        <tr> 
                            <!-- there needs to be content in the cell for it to render in some clients --> 
                            <td class="email-content-rounded-top mobile-expand" style="padding: 0px; border-collapse: collapse; color: #fff; padding: 0 15px 0 16px; height: 15px; background-color: #fff; border-left: 1px solid #ccc; border-top: 1px solid #ccc; border-right: 1px solid #ccc; border-bottom: 0; border-top-right-radius: 5px; border-top-left-radius: 5px; height: 10px; line-height: 10px; padding: 0 15px 0 16px; mso-line-height-rule: exactly">
                                &nbsp;
                            </td> 
                        </tr> 
                        <tr> 
                            <td class="email-content-main mobile-expand " style="padding: 0px; border-collapse: collapse; border-left: 1px solid #ccc; border-right: 1px solid #ccc; border-top: 0; border-bottom: 0; padding: 0 15px 0 16px; background-color: #fff"> 
                                <table class="page-title-pattern" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt"> 
                                    <tr> 
                                        <td style="vertical-align: top;; padding: 0px; border-collapse: collapse; padding-right: 5px; font-size: 20px; line-height: 30px; mso-line-height-rule: exactly" class="page-title-pattern-header-container"> <span class="page-title-pattern-header" style="font-family: Arial, sans-serif; padding: 0; font-size: 20px; line-height: 30px; mso-text-raise: 2px; mso-line-height-rule: exactly; vertical-align: middle"> <a href="" style="color: #3b73af; text-decoration: none">${product_name} ${product_version} ${release_milestone}  Release Handover Review</a> </span> 
                                        </td> 
                                    </tr> 
                                </table> 
                            </td> 
                        </tr> 
                        <tr> 
                            <td id="text-paragraph-pattern-top" class="email-content-main mobile-expand  comment-top-pattern" style="padding: 0px; border-collapse: collapse; border-left: 1px solid #ccc; border-right: 1px solid #ccc; border-top: 0; border-bottom: 0; padding: 0 15px 0 16px; background-color: #fff; border-bottom: none; padding-bottom: 0"> 
                                <table class="text-paragraph-pattern" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; font-family: Arial, sans-serif; font-size: 14px; line-height: 20px; mso-line-height-rule: exactly; mso-text-raise: 2px"> 
                                    <tr> 
                                        <td class="text-paragraph-pattern-container mobile-resize-text " style="padding: 0px; border-collapse: collapse; padding: 0 0 10px 0"> 
                                            <p style="margin: 10px 0 0 0">Release handover is <font color="#ff0000">waiting for review</font> in remote: <a href="${handover_pr}" class="external-link" rel="nofollow" style="color: #3b73af; text-decoration: none">${handover_pr}</a></p> 
                                            <p style="margin: 10px 0 0 0">Staging folder URL: <br /> <a href="${rcm_stage_base}/${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/" class="external-link" rel="nofollow" style="color: #3b73af; text-decoration: none">${rcm_stage_base}/${brms_stage_folder}/${brms_product_name}-${product_version}.${release_milestone}/</a></p> 
                                            <p style="margin: 10px 0 0 0"><a href="${rcm_stage_base}/${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/" class="external-link" rel="nofollow" style="color: #3b73af; text-decoration: none">${rcm_stage_base}/${bpms_stage_folder}/${bpms_product_name}-${product_version}.${release_milestone}/</a></p> 
                                        </td> 
                                    </tr> 
                                </table> 
                            </td> 
                        </tr> 
                        <tr> 
                            <td class="email-content-main mobile-expand " style="padding: 0px; border-collapse: collapse; border-left: 1px solid #ccc; border-right: 1px solid #ccc; border-top: 0; border-bottom: 0; padding: 0 15px 0 16px; background-color: #fff"> 
                                <table id="actions-pattern" cellspacing="0" cellpadding="0" border="0" width="100%" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; font-family: Arial, sans-serif; font-size: 14px; line-height: 20px; mso-line-height-rule: exactly; mso-text-raise: 1px"> 
                                    <tr> 
                                        <td id="actions-pattern-container" valign="middle" style="padding: 0px; border-collapse: collapse; padding: 10px 0 10px 24px; vertical-align: middle; padding-left: 0"> 
                                        </td> 
                                    </tr> 
                                </table> 
                            </td> 
                        </tr> 
                        <!-- there needs to be content in the cell for it to render in some clients --> 
                        <tr> 
                            <td class="email-content-rounded-bottom mobile-expand" style="padding: 0px; border-collapse: collapse; color: #fff; padding: 0 15px 0 16px; height: 5px; line-height: 5px; background-color: #fff; border-top: 0; border-left: 1px solid #ccc; border-bottom: 1px solid #ccc; border-right: 1px solid #ccc; border-bottom-right-radius: 5px; border-bottom-left-radius: 5px; mso-line-height-rule: exactly">
                                &nbsp;
                            </td> 
                        </tr> 
                    </table> 
                </td> 
            </tr> 
            <tr> 
                <td id="footer-pattern" style="padding: 0px; border-collapse: collapse; padding: 12px 20px"> 
                    <table id="footer-pattern-container" cellspacing="0" cellpadding="0" border="0" style="border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt"> 
                        <tr> 
                            <td id="footer-pattern-text" class="mobile-resize-text" width="100%" style="padding: 0px; border-collapse: collapse; color: #999; font-size: 12px; line-height: 18px; font-family: Arial, sans-serif; mso-line-height-rule: exactly; mso-text-raise: 2px">
                                This message was sent by <span id="footer-build-information">${BUILD_URL} </span> 
                            </td> 
                            <td id="footer-pattern-logo-desktop-container" valign="top" style="padding: 0px; border-collapse: collapse; padding-left: 20px; vertical-align: top">                                 
                            </td> 
                        </tr> 
                    </table> 
                </td> 
            </tr> 
        </table>   
    </body>
</html>
'''

// Creates or updates a free style job.
def jobDefinition = job("${PRODUCT_NAME}-send-review-notification-mail") {

    // Sets a description for the job.
    description("This job is responsible for sending the PR review email to the team.")

    // Adds post-build actions to the job.
    publishers {

        // Sends customizable email notifications.
        extendedEmail {

            // Adds email addresses that should receive emails.
            recipientList('${release_engineer}')

            // Adds e-mail addresses to use in the Reply-To header of the email.
            replyToList('${release_engineer}')

            // Sets the default email subject that will be used for each email that is sent.
            defaultSubject('[ACTION REQUIRED] [BxMS Release CI] ${product_name} ${product_version} ${release_milestone}  Release Handover Review')

            // Sets the default email content that will be used for each email that is sent.
            defaultContent(mailContent)

            // Sets the content type of the emails sent after a build.
            contentType('text/html')
        }
    }
}

JobTemplate.addCommonConfiguration(jobDefinition, CI_PROPERTIES_FILE, PRODUCT_NAME)