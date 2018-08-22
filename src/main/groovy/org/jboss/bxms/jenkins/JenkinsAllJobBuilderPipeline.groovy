package org.jboss.bxms.jenkins

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
import ca.szc.configparser.Ini

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
        if (gerrit_ref_spec != '')
            urlString = urlString + ";hb=" + gerrit_ref_spec
      BufferedReader configReader = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()))
      Ini _ini_cfg = new Ini().read(configReader)
      Map<String,Map<String,String>> sections = _ini_cfg.getSections()
      Map packagesMap = new HashMap()
      for (String section_name : sections.keySet())
      {
          Map<String, String> section=sections.get(section_name)
          if ((!section.containsKey("config_type")) || (section.containsKey("config_type") && section.get("config_type").equals("bom-builder")) )
          {
              if(section.containsKey("buildrequires")){
                if(section.get("buildrequires").length()!=0){
                  String[] requiresArr=section.get("buildrequires").split(" ")
                  packagesMap.put(section_name,requiresArr)
                }else{
                  packagesMap.put(section_name,"")
                }
              }else{
                packagesMap.put(section_name,"")
              }
          }
      }
      // get the squence of jobs
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
    ArrayList<String> kahnTopological(HashMap<String,String[]> packagesMap){
        ArrayList<String> resultAL=new ArrayList<String>()
        Map packagesMapIndegree=new HashMap()
        ArrayList<String> zeroIndegreeArr=new ArrayList<String>()
        int allEdges=0
        for (String keyName : packagesMap.keySet()) {
          if (packagesMap.get(keyName).size()==0) {
            zeroIndegreeArr.add(keyName)

          }
          allEdges+=packagesMap.get(keyName).size()
          packagesMapIndegree.put(keyName,packagesMap.get(keyName).size())
        }

        while(zeroIndegreeArr.size()>0){
          String current=zeroIndegreeArr.get(0)
          zeroIndegreeArr.remove(0)
          resultAL.add(current)
          ArrayList<String> currentPkgChild =new ArrayList<String>()
          for (Map.Entry<String, String[]> entry : packagesMap.entrySet()) {
            int flag=0
            for (ele in entry.getValue()) {
              if(ele.matches(current)){
                flag=1
                break
              }
            }
            if (flag ==1) {
              currentPkgChild.add(entry.getKey())
            }
          }
        for (int i=0;i<currentPkgChild.size() ;i++ ) {
          int indegree=packagesMapIndegree.get(currentPkgChild.get(i))-1
          allEdges=allEdges-1
          packagesMapIndegree.put(currentPkgChild.get(i),indegree)
          if (indegree == 0) {
            zeroIndegreeArr.add(currentPkgChild.get(i))
          }
        }
      }

      if (allEdges!=0) {

        throw new IOException("There has circles in " +cfg_file + " buildrequires map, Debug information:\n" + packagesMapIndegree)
      }
      return resultAL
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
