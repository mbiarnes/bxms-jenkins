package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *  Create BxMS release/build pipeline stream with Parameter
 */
class JenkinsAllJobBuilderPipeline {
    String release_code
    String job_type
    String cfg_file
    String gerrit_ref_spec

    Job build(DslFactory dslFactory) {
      String cfg_filename = cfg_file
      if (cfg_file.contains("/")) {
          String[] cfg_file_paths = cfg_file.tokenize("/")
          cfg_filename = cfg_file_paths[cfg_file_paths.length - 1]
      }
      String urlString ="https://code.engineering.redhat.com/gerrit/gitweb?p=integration-platform-config.git;a=blob_plain;f=" + cfg_filename
      if (gerrit_ref_spec != '') {
          urlString = urlString + ";hb=" + gerrit_ref_spec
      }

      Map<String, String[]> packagesMap = DependencyGraphUtils.loadPackageMapFromURL(urlString)
      // get the sequence of jobs
      ArrayList<String> jobsArr=kahnTopological(packagesMap)
      String pipelineScript=getPipelineCode(jobsArr,packagesMap)
      def choosOptScript=getChooseOpt(jobsArr)

      dslFactory.folder(release_code + "-" + job_type + "-release-pipeline")
      String _cfg = cfg_filename

      dslFactory.pipelineJob(release_code + "-" + job_type + "-release-pipeline/" + release_code + "-" + job_type + "-jenkinsbuild-pipeline") {
            it.description "This job is pipeline job for " + release_code + " " +  job_type + ". "
            logRotator {
                numToKeep 8
            }
            label("nightly-node")
            multiscm {

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-config")
                        refspec("\$CONFIG_REFS")
                    }
                    // Specify the branches to examine for changes and to build.
                    branch("FETCH_HEAD")
                }

                // Adds a Git SCM source.
                git {

                    // Adds a remote.
                    remote {

                        // Sets the remote URL.
                        url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com:22/integration-platform-tooling")
                        refspec("\$TOOLING_REFS")
                    }

                    // Specify the branches to examine for changes and to build.
                    branch("FETCH_HEAD")

                    // Adds additional behaviors.
                    extensions {

                        // Specifies a local directory (relative to the workspace root) where the Git repository will be checked out.
                        relativeTargetDirectory('ip-tooling')
                    }
                }
            }
            parameters {
                booleanParam('RUNSTAGEAFTER', true, 'Uncheck to just run the stage alone without the stages after.')
                choiceParam('STARTSTAGE', choosOptScript, 'choose the stage to start,default the first one.')
                stringParam('CONFIG_REFS','+refs/heads/master:refs/remotes/origin/master','The refs of integration-platform-config you want to pull,defautl master.')
                stringParam('TOOLING_REFS','+refs/heads/master:refs/remotes/origin/master','The refs of integration-platform-tooling you want to pull,default master.')
            }
            definition{
              cps {
                script(pipelineScript)
                //running in sandbox and  make sure adding
                // "new hudson.model.ChoiceParameterDefinition java.lang.String java.lang.String[] java.lang.String"
                // into Jenkins->Configuration->In-process Script Approval ->Signatures already approved box
                sandbox()
              }
            }

        }
    }

    // func to turn cfg requirement relationship into oneline steps use kahn
    List<String> kahnTopological(Map<String,String[]> packagesMap){
      try {
        return DependencyGraphSorter.kahnTopological(packagesMap);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("There are circles in " +cfg_file + " buildrequires map", e)
      }
    }

    // func to get the choose options.
    def getChooseOpt(ArrayList<String> jobsArr){
      def result=[]
      for (int i=0;i< jobsArr.size() ;i++  ) {
        result.add(jobsArr.get(i))
      }
      return result
    }
    // func to get the pipeline code of input cfg file
    String getPipelineCode(ArrayList<String> jobsArr,HashMap<String,String[]> packagesMap) {

      //result=result+jobsArr.get(0)
      String stageNameList="[['"+jobsArr.get(0)+"'"
      ArrayList<String> tempList=new ArrayList<String>()
      tempList.add(jobsArr.get(0))
      for (int i=1;i<jobsArr.size() ;i++ ) {
        if (packagesMap.get(jobsArr.get(i)).size() !=0) {
          int flag=0
          for (int j=0;j< packagesMap.get(jobsArr.get(i)).length;j++ ) {
            if(tempList.contains(packagesMap.get(jobsArr.get(i))[j])){
              for (int k=0;k<tempList.size() ;k++ ) {
                tempList.remove(k)
              }
              flag=1
              break
            }
          }
          if (flag==0) {
            stageNameList=stageNameList+",'"+jobsArr.get(i)+"'"
          }else{
            stageNameList=stageNameList+"],['"+jobsArr.get(i)+"'"
          }
          tempList.add(jobsArr.get(i))
        }else{
          stageNameList=stageNameList+",'"+jobsArr.get(i)+"'"
          tempList.add(jobsArr.get(i))
        }
      }
      stageNameList=stageNameList+"]]"
      // inside list to used to parellel run stages
      //stageNameList="[[''],['','',''],['','']]"
      String result='''
      def release_code="'''+release_code+'''"
      def stageNames='''+stageNameList+'''
      def runStageAfter="${RUNSTAGEAFTER}"
      def yourchoose="${STARTSTAGE}"
      int returnonestage(stageNames,insidecount,insidej,yourchoose,runStageAfter,release_code,flag){
          stage(stageNames.get(insidecount).get(insidej)){
                if(yourchoose.matches(stageNames.get(insidecount).get(insidej)) ){
                    flag=1
                }
                if((flag==1 && runStageAfter.matches("true"))|| yourchoose.matches(stageNames.get(insidecount).get(insidej))){

                    def jobresult=build(job : "y-" + release_code + "-" + stageNames.get(insidecount).get(insidej), propagate: false).getResult().trim()
                    if(jobresult == 'UNSTABLE'|| jobresult == 'SUCCESS'){
                        currentBuild.result = 'SUCCESS'
                    }else{
                        currentBuild.result = 'FAILURE'
                        error("Job ${stageNames.get(insidecount).get(insidej)} Build FAILED!")
                    }

                }
          }
          return flag
      }
      node ('release-pipeline'){
          int flag=0
          for(int count=0;count<stageNames.size();count++){
            def insidecount=count
            if(stageNames.get(insidecount).size()==1){
                flag=returnonestage(stageNames,insidecount,0,yourchoose,runStageAfter,release_code,flag)
            }else{
                def branches=[:]
                for(int j=0;j<stageNames.get(insidecount).size();j++){
                  def insidej=j
                  branches["${stageNames.get(insidecount).get(insidej)}"]={
                    flag=returnonestage(stageNames,insidecount,insidej,yourchoose,runStageAfter,release_code,flag)
                  }
                }
                parallel branches
            }
          }

        }
      '''
      return result
    }
}
