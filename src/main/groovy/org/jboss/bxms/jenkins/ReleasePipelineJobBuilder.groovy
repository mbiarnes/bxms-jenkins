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
    String pipelineSeqFile="release-pipeline.ini"

    Job build(DslFactory dslFactory) {
      def file_content=dslFactory.readFileFromWorkspace('streams/'+release_code+'/'+pipelineSeqFile)
      String stageSeq=getStageSeq(file_content)
      String product_job_prefix=release_code+"-"
      String pipelineScript='''
        def stageSeq='''+stageSeq+'''
        def product_job_prefix="'''+product_job_prefix+'''"
        node('release-pipeline') {
          for(int i=0;i<stageSeq.size();i++){
            def insidei=i
            def branches=[:]
            for(int j=0;j<stageSeq.get(insidei).size();j++){
              def insidej=j
              branches["${insidej}"]={
                stage(stageSeq.get(insidei).get(insidej)){
                  if(stageSeq.get(insidei).get(insidej).matches("Pause")){
                    hook = registerWebhook()
                    echo "Waiting for trigger on ${hook.getURL()}."
                    sh "sed -i \'/^register_web_hook=/d\' ${CI_PROPERTIES_FILE} && echo \\\"register_web_hook=${hook.getURL()}\\\" >>${CI_PROPERTIES_FILE}"
                    data = waitForWebhook hook
                  }else if(stageSeq.get(insidei).get(insidej).matches("Input")){
                    input 'continue the work?'
                    echo "continue to next stage."
                  }else{
                    build job : product_job_prefix + stageSeq.get(insidei).get(insidej)
                  }
                }
              }
            }
            parallel branches
          }

        }
      '''

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

            scm {
                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/bxms-jenkins")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("master")
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
        }


    }
    String getStageSeq(String file_content){
      String[] lineSeq=file_content.split('\n')
      String result="["
      for (int j=0;j<lineSeq.size();j++) {
        if (lineSeq[j].matches("steps(.*)")) {
          String[] rightline=lineSeq[j].split(':')
          String[] stages=rightline[1].split(" ")
          if(result.length()==1){
            result=result+getOneArr(stages)
          }else{
            result=result+","+getOneArr(stages)
          }
        }
      }
      result=result+"]"
      return result
    }
    String getOneArr(String[] arr){
      String result="["
      for(int i=0;i<arr.size();i++){
        if(i==0){
          result=result+"'"+arr[i]+"'"
        }else{
          result=result+",'"+arr[i]+"'"
        }
      }
      return result+"]"
    }
}
