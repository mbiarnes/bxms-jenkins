package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import java.io.File
/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class ReleasePipelineJobBuilder {

    String release_code
    String ci_properties_file
    String cfg_file
    String cron_val = null
    String pipelineSeqFile="release-pipeline.ini"
    String gerritBranch
    String gerritRefspec
    String jobName

    Job build(DslFactory dslFactory) {
      def file_content=dslFactory.readFileFromWorkspace('streams/'+release_code+'/'+pipelineSeqFile)
      // String stageSeq=getStageSeq(file_content)
      String product_job_prefix=release_code+"-"
      String pipelineScript=getPipelineScript(file_content,product_job_prefix)
      // This is used by local test to set JOB_NAME to avoid error showing
        dslFactory.folder(release_code + "-release-pipeline")
        dslFactory.pipelineJob(release_code + "-release-pipeline/a-" + release_code + "-release-pipeline") {
            it.description "This job is job for run " + release_code + "release pipeline. To change the  parameter of the release pipeline, Please go to streams/release_code/env.properties"
            logRotator {
                numToKeep 8
            }
            // Adds environment variables to the build.
            environmentVariables {

                // The name of the product, e.g., bxms64.
                env("release_code", release_code)

                // Release pipeline CI properties file
                env("CI_PROPERTIES_FILE",ci_properties_file)

                // IP project configuration file
                env("IP_CONFIG_FILE", cfg_file)

                // Inject Jenkins build variables and also environment contributors and build variable contributors provided by other plugins.
                keepBuildVariables(true)

                // Injects Jenkins system variables and environment variables defined as global properties and as node properties.
                keepSystemVariables(true)

            }
            parameters {
                choiceParam('STARTSTAGE', getAllStage(file_content), 'choose the stage to start,default the first one.')
                stringParam("GERRIT_REFSPEC", gerritRefspec, "Parameter passed by Gerrit code review trigger")
                stringParam("GERRIT_BRANCH", gerritBranch, "Parameter passed by Gerrit code review trigger")
            }
            scm {
                // Adds a Git SCM source.
                git {

                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
                        name("origin")
                        refspec("\$GERRIT_REFSPEC")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("\$GERRIT_BRANCH")

                }
            }

            definition{
              cps{
                script(pipelineScript)
                //running in sandbox and  make sure adding
                // "new hudson.model.ChoiceParameterDefinition java.lang.String java.lang.String[] java.lang.String"
                // into Jenkins->Configuration->In-process Script Approval ->Signatures already approved box
                sandbox()
              }
            }
            if (cron_val != null && !jobName.matches("codereview/(.*)")) {
                triggers {
                    cron("$cron_val")
                }
            }
        }


    }
    String getPipelineScript(String file_content,String product_job_prefix){
      String pipelineScript="def startstage=\"\${STARTSTAGE}\"\ndef flag=0\nnode('release-pipeline')\n{\n"
      String[] lineSeq=file_content.split('\n')
      for (int j=0;j<lineSeq.size();j++) {
        if (lineSeq[j].matches("steps(.*)")) {
          String rightline=lineSeq[j].substring(lineSeq[j].indexOf(":")+1)
          String[] stages=rightline.split(' ')
          pipelineScript=pipelineScript+" def branches"+j+"=[:]\n"
          for(int i=0;i<stages.size();i++){
              String stageName=getStageName(stages[i])
              String fullJobName = stageName
              if (! fullJobName.endsWith("pipeline")) {
                  fullJobName = product_job_prefix + stageName
              }
            pipelineScript=pipelineScript+"  branches"+j+"["+i+"]={\n   stage('"+stageName+"'){\n     if(startstage.matches('"+stageName+"')){flag=1}\n      if(flag==1){\n"
            if(isParaExists(stages[i])){
              pipelineScript=pipelineScript+"    build(job : '"+ fullJobName +"',parameters: "+ getStagePara(stages[i])+")\n"
            }else if(!isParaExists(stages[i])){
              if(stageName.matches("Input")){
                pipelineScript=pipelineScript+"    input 'continue the work?'\n    echo 'continue to next stage.'\n"
              }else if(stageName.matches("Pause")){
                pipelineScript=pipelineScript+"\n    hook = registerWebhook()\n    echo 'Waiting for trigger on \${hook.getURL()}.'\n    sh \"sed -i \'/^register_web_hook=/d\' \${CI_PROPERTIES_FILE} && echo \\\"register_web_hook=\${hook.getURL()}\\\" >>\${CI_PROPERTIES_FILE}\"\n    data = waitForWebhook hook\n    if(data.trim() != \"OK\"){\n     error(\"CI trigger return FAIL,force job stop...\")\n    }\n"
              }else{
                pipelineScript=pipelineScript+"    build job :'"+ fullJobName +"'\n"
              }
            }
            pipelineScript=pipelineScript+"     }\n    }\n    }\n"
          }
          pipelineScript=pipelineScript+"  parallel branches"+j+"\n"

        }
      }
      pipelineScript=pipelineScript+"}\n"
      return pipelineScript
    }


    Boolean isParaExists(String arr){
      if(arr.indexOf("[")!=-1){
        return true
      }else{
        return false
      }
    }
    def getAllStage(String file_content){
      def result=[]
      String[] lineSeq=file_content.split('\n')
      for (int j=0;j<lineSeq.size();j++) {
        if (lineSeq[j].matches("steps(.*)")) {
          String rightline=lineSeq[j].substring(lineSeq[j].indexOf(":")+1)
          String[] stages=rightline.split(' ')
          for(int i=0;i<stages.size();i++){
            String stageName=getStageName(stages[i])
            if(!stageName.matches("Input") && !stageName.matches("Pause"))
            result.add(stageName)
          }
        }
      }
      return result
    }

    String getStagePara(String arr){
      String result
      if(isParaExists(arr)){
        result=arr.substring(arr.indexOf("["))
      }else{
        result=""
      }
      return result
    }
    String getStageName(String arr){
      String result
      if(isParaExists(arr)){

        result=arr.substring(0,arr.indexOf("["))
      }else{
        result=arr
      }
      return result
    }

}
