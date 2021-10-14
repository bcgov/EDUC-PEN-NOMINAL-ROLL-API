def performDeploy(String stageEnv, String projectEnv, String repoName, String appName, String jobName, String tag, String sourceEnv, String targetEnvironment, String appDomain, String rawApiDcURL, String minReplicas, String maxReplicas, String minCPU, String maxCPU, String minMem, String maxMem, String targetEnv, String NAMESPACE, String commonNamespace){
  script{
    deployStageNoEnv(stageEnv, projectEnv, repoName, appName, jobName,  tag, sourceEnv, targetEnvironment, appDomain, rawApiDcURL, minReplicas, maxReplicas, minCPU, maxCPU, minMem, maxMem, false)
  }
  configMapSetup("${appName}","${appName}".toUpperCase(), NAMESPACE, "${targetEnv}", "${sourceEnv}");
  performStandardUpdateConfigMapStep("${repoName}", "${tag}", "${targetEnv}", "${appName}", "${NAMESPACE}", "${commonNamespace}");
  performStandardRollout(appName, projectEnv, jobName)
  script{
    deployStageNoEnv(stageEnv, projectEnv, repoName, appName, jobName,  tag, sourceEnv, targetEnvironment, appDomain, rawApiDcURL, minReplicas, maxReplicas, minCPU, maxCPU, minMem, maxMem, true)
  }
}

def deployStageNoEnv(String stageEnv, String projectEnv, String repoName, String appName, String jobName, String tag, String sourceEnv, String targetEnvironment, String appDomain, String rawApiDcURL, String minReplicas, String maxReplicas, String minCPU, String maxCPU, String minMem, String maxMem, Boolean deployEvenIfExists) {
  openshift.withCluster() {
    openshift.withProject(projectEnv) {
      def dcApp = openshift.selector('dc', "${appName}-${jobName}")
      if(!dcApp.exists() || deployEvenIfExists){
        echo "Tagging ${appName} image with version ${tag}"
        openshift.tag("${sourceEnv}/${repoName}-${jobName}:${tag}", "${repoName}-${jobName}:${tag}")
        def dcTemplate = openshift.process('-f',
          "${rawApiDcURL}",
          "REPO_NAME=${repoName}",
          "JOB_NAME=${jobName}",
          "NAMESPACE=${projectEnv}",
          "APP_NAME=${appName}",
          "HOST_ROUTE=${appName}-${targetEnvironment}.${appDomain}",
          "TAG=${tag}",
          "MIN_REPLICAS=${minReplicas}",
          "MAX_REPLICAS=${maxReplicas}",
          "MIN_CPU=${minCPU}",
          "MAX_CPU=${maxCPU}",
          "MIN_MEM=${minMem}",
          "MAX_MEM=${maxMem}"
        )

        echo "Applying Deployment for ${appName}"
        def dc = openshift.apply(dcTemplate).narrow('dc')
      }else{
        echo "DC already exists for ${appName}-${jobName}, skipping initial rollout"
      }
    }
  }
}

def configMapSetup(String appName,String appNameUpper, String namespace, String targetEnv, String sourceEnv){
  script {

    try{
      sh( script: "oc -n ${namespace}-${targetEnv} describe configmaps ${appName}-${targetEnv}-setup-config", returnStdout: true)
      echo 'Config map already exists. Moving to next stage...'
    } catch(e){
      configProperties = input(
        id: 'configProperties', message: "Please enter the required credentials to allow ${appName} to run:",
        parameters: [
          string(defaultValue: "",
            description: 'JDBC connect string for database',
            name: 'DB_JDBC_CONNECT_STRING'),
          string(defaultValue: "",
            description: "Username for ${appName} to connect to the database",
            name: "DB_USER"),
          password(defaultValue: "",
            description: "Password for ${appName} to connect to the database",
            name: "DB_PWD"),
          string(defaultValue: "",
            description: "Token for ${appName} FluentBit sidecar to connect to the Splunk",
            name: "SPLUNK_TOKEN")
        ])
      sh """
		  set +x
          echo Creating ${appName}-${targetEnv}-setup-config configmap...
		  oc create -n ${namespace}-${targetEnv} configmap ${appName}-${targetEnv}-setup-config --from-literal=DB_PWD_${appNameUpper}='${configProperties.DB_PWD}' --from-literal=SPLUNK_TOKEN_${appNameUpper}=${configProperties.SPLUNK_TOKEN} --from-literal=DB_JDBC_CONNECT_STRING=${configProperties.DB_JDBC_CONNECT_STRING} --from-literal=DB_USER_${appNameUpper}=${configProperties.DB_USER} --dry-run -o yaml | oc apply -f -
        """
    }
  }
}

def performStandardUpdateConfigMapStep(String repoName,String tag, String targetEnv, String appName, String NAMESPACE, String commonNamespace){
  script{
    dir('tools/jenkins'){
      if(tag == "latest") {
        sh "curl -s https://raw.githubusercontent.com/bcgov/${repoName}/main/tools/jenkins/update-configmap.sh | bash /dev/stdin \"${targetEnv}\" \"${appName}\" \"${NAMESPACE}\" \"${commonNamespace}\""
      } else {
        sh "curl -s https://raw.githubusercontent.com/bcgov/${repoName}/${tag}/tools/jenkins/update-configmap.sh | bash /dev/stdin \"${targetEnv}\" \"${appName}\" \"${NAMESPACE}\" \"${commonNamespace}\""
      }
    }
  }
}
def performStandardRollout(String appName, String projectEnv, String jobName){
  script{
    echo "Rolling out ${appName}-${jobName}"
    try {
      sh(script: "oc -n ${projectEnv} rollout latest dc/${appName}-${jobName}", returnStdout: true)
    }
    catch(e){
      //Do nothing
    }
  }
}
return this;
