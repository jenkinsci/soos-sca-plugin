
package io.soos;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import hudson.util.FormValidation;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.soos.integration.commons.Constants;
import io.soos.integration.domain.SOOS;
import io.soos.integration.domain.analysis.AnalysisResultResponse;
import io.soos.integration.domain.structure.StructureResponse;
import jenkins.tasks.SimpleBuildStep;
import lombok.Data;

@Data
public class SoosSCA extends Builder implements SimpleBuildStep{

  private static final Logger LOG = LoggerFactory.getLogger(SoosSCA.class);

  private String projectName;
  private String mode;
  private String onFailure;
  private String operatingSystem;
  private String resultMaxWait;
  private String resultPollingInterval;
  private String dirsToExclude;
  private String filesToExclude;
  private String commitHash;
  private String branchName;
  private String branchURI;
  private String buildVersion;
  private String buildURI;

  
  @DataBoundConstructor
  public SoosSCA(String projectName, String mode, String onFailure, String operatingSystem, String resultMaxWait,
      String resultPollingInterval, String dirsToExclude, String filesToExclude, String commitHash, String branchName,
      String branchURI, String buildVersion, String buildURI, Mode modes) {
    this.projectName = projectName;
    this.mode = mode;
    this.onFailure = onFailure;
    this.operatingSystem = operatingSystem;
    this.resultMaxWait = resultMaxWait;
    this.resultPollingInterval = resultPollingInterval;
    this.dirsToExclude = dirsToExclude;
    this.filesToExclude = filesToExclude;
    this.commitHash = commitHash;
    this.branchName = branchName;
    this.branchURI = branchURI;
    this.buildVersion = buildVersion;
    this.buildURI = buildURI;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {

    Map<String, String> map = new HashMap<String,String>();

    String dirsToExclude = addSoosDirToExclusion(this.dirsToExclude);
    map.put(Constants.PARAM_PROJECT_NAME_KEY, this.projectName);
    map.put(Constants.PARAM_MODE_KEY, this.mode);
    map.put(Constants.PARAM_ON_FAILURE_KEY, this.onFailure);
    map.put(Constants.PARAM_DIRS_TO_EXCLUDE_KEY, dirsToExclude);
    map.put(Constants.PARAM_FILES_TO_EXCLUDE_KEY, this.filesToExclude);
    map.put(Constants.PARAM_WORKSPACE_DIR_KEY, env.get("WORKSPACE"));
    map.put(Constants.PARAM_CHECKOUT_DIR_KEY, env.get("WORKSPACE"));
    map.put(Constants.PARAM_API_BASE_URI_KEY,"https://dev-api.soos.io/api/"); //Constants.SOOS_DEFAULT_API_URL
    map.put(Constants.PARAM_ANALYSIS_RESULT_MAX_WAIT_KEY, this.resultMaxWait);
    map.put(Constants.PARAM_ANALYSIS_RESULT_POLLING_INTERVAL_KEY, this.resultPollingInterval);
    map.put(Constants.PARAM_OPERATING_ENVIRONMENT_KEY, this.operatingSystem); // GETOUT
    map.put(Constants.PARAM_BRANCH_NAME_KEY, this.branchName); 
    map.put(Constants.PARAM_BRANCH_URI_KEY, this.branchURI);        
    map.put(Constants.PARAM_COMMIT_HASH_KEY, this.commitHash);     
    map.put(Constants.PARAM_BUILD_VERSION_KEY, this.buildVersion);
    map.put(Constants.PARAM_BUILD_URI_KEY, this.buildURI);
    map.put(Constants.PARAM_INTEGRATION_NAME_KEY, PluginConstants.INTEGRATION_NAME);
    map.put("SOOS_CLIENT_ID", env.get("SOOS_CLIENT_ID"));
    map.put("SOOS_API_KEY", env.get("SOOS_API_KEY"));

    
    if(StringUtils.isBlank(this.resultMaxWait)) {
        map.put(Constants.PARAM_ANALYSIS_RESULT_MAX_WAIT_KEY, String.valueOf(Constants.MIN_RECOMMENDED_ANALYSIS_RESULT_MAX_WAIT));
    }
    if(StringUtils.isBlank(this.resultPollingInterval)) {
        map.put(Constants.PARAM_ANALYSIS_RESULT_POLLING_INTERVAL_KEY, String.valueOf(Constants.MIN_ANALYSIS_RESULT_POLLING_INTERVAL));
    }

    setEnvProperties(map, listener);
    String reportUrl = "";
    try {
        SOOS soos = new SOOS();
       
        StructureResponse structure = soos.getStructure();
        System.out.println(structure.toString());
        long filesProcessed = soos.sendManifestFiles(structure.getProjectId(), structure.getAnalysisId());
        LOG.info("File processed: ".concat(String.valueOf(filesProcessed)));
        
        if(filesProcessed > 0) {
            soos.startAnalysis(structure.getProjectId(), structure.getAnalysisId());
            AnalysisResultResponse results;
            switch (soos.getMode()) {
                case RUN_AND_WAIT:
                    results = soos.getResults(structure.getReportStatusUrl());
                    reportUrl = soos.getStructure().getReportURL();
                    listener.getLogger().println("CLICK THE LINK TO SEE THE REPORT: ".concat(reportUrl));
                    LOG.info(results.toString());
                    break;
                case ASYNC_INIT:
                    listener.getLogger().println("async_init mode selected, starting asynchronous analysis...");
                    soos.startAnalysis(structure.getProjectId(), structure.getAnalysisId());
                    break;
                case ASYNC_RESULT:
                    listener.getLogger().println("async_result mode selected, getting result from previous analysis...");
                    results = soos.getResults(structure.getReportStatusUrl());
                    reportUrl = soos.getStructure().getReportURL();
                    listener.getLogger().println("CLICK THE LINK TO SEE THE REPORT: ".concat(reportUrl));
                    LOG.info(results.toString());
                    break;
            }
        }
        
    } catch (Exception e) {
        if(this.onFailure.equals(PluginConstants.FAIL_THE_BUILD)){
            listener.error("SOOS SCA cannot be done, error: ".concat(e.getMessage()).concat("- the build has failed!"));
            run.setResult(Result.FAILURE);
            return;
        } 
        listener.getLogger().println("SOOS SCA cannot be done, error: ".concat(e.getMessage()).concat("- Continuing the build... "));
    } 
  }

  @Extension
  public static final class SoosSCADescriptor extends BuildStepDescriptor<Builder> {

      @Override
      public boolean isApplicable(Class<? extends AbstractProject> jobType) {
          return true;
      }

      @Override
      public String getDisplayName() {
          return "SOOS SCA";
      }

      public FormValidation doCheckProjectName(@QueryParameter String projectName) {

          if( StringUtils.isBlank(projectName) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_NOT_BE_NULL);
          }
          if( projectName.length() < 5 ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_MORE_THAN_5_CHARACTERS);
          }
          return FormValidation.ok();
      }
      public FormValidation doCheckResultMaxWait(@QueryParameter String resultMaxWait) {

          if( !ObjectUtils.isEmpty(resultMaxWait) && !validateNumber(resultMaxWait) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
          }
          return FormValidation.ok();
      }
      public FormValidation doCheckResultPollingInterval(@QueryParameter String resultPollingInterval) {

          if( !ObjectUtils.isEmpty(resultPollingInterval) && !validateNumber(resultPollingInterval) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
          }
          return FormValidation.ok();
      }

      public Mode[] getModes() {
          return Mode.values();
      }

      public OnFailure[] getOptions(){
          return OnFailure.values();
      }

      public OperatingSystem[] getOSList(){
          return OperatingSystem.values();
      }

      private Boolean validateNumber(String value) {
          try {
              Integer.parseInt(value);
              return true;
          } catch ( Exception e ){
              return false;
          }
      }

  }

  private void setEnvProperties(Map<String, String> map,TaskListener listener){

    map.forEach((key, value) -> {
        if(StringUtils.isNotBlank(value)) {
            System.setProperty(key, value);
            LOG.warn(key + ": "+ value);
            listener.getLogger().println(key + ": "+ value);
 
        }
    });
  }

  private String addSoosDirToExclusion(String dirs){
      if(StringUtils.isNotBlank(dirs)){
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(dirs).append(",").append(PluginConstants.SOOS_DIR_NAME);
           
          return stringBuilder.toString();
      } 
      
      return PluginConstants.SOOS_DIR_NAME;
  }




/* */

}
