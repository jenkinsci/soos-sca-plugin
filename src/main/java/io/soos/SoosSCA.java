
package io.soos;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import io.soos.commons.ErrorMessage;
import io.soos.commons.PluginConstants;
import io.soos.commons.Utils;
import io.soos.domain.Mode;
import io.soos.domain.OnFailure;
import io.soos.domain.OperatingSystem;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
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
import io.soos.integration.domain.structure.StructureResponse;
import jenkins.tasks.SimpleBuildStep;

@Getter
@Setter
public class SoosSCA extends Builder implements SimpleBuildStep{

    private static final Logger LOG = LoggerFactory.getLogger(SoosSCA.class);

    private String projectName;
    private String mode;
    private String onFailure;
    private String operatingSystem;
    private String resultMaxWait;
    private String resultPollingInterval;
    private String apiBaseURI;
    private String dirsToExclude;
    private String filesToExclude;
    private String commitHash;
    private String branchName;
    private String branchURI;
    private String buildVersion;
    private String buildURI;

    @DataBoundConstructor
    public SoosSCA(String projectName, String mode, String onFailure, String operatingSystem, String resultMaxWait,
                        String resultPollingInterval, String apiBaseURI, String dirsToExclude, String filesToExclude, String commitHash, String branchName,
                        String branchURI, String buildVersion, String buildURI) {

        this.projectName = projectName;
        this.mode = mode;
        this.onFailure = onFailure;
        this.operatingSystem = operatingSystem;
        this.resultMaxWait = resultMaxWait;
        this.resultPollingInterval = resultPollingInterval;
        this.apiBaseURI = apiBaseURI;
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

        map.putAll(populateContext(env));

        map.putAll(getEnvironmentVariables());

        setEnvProperties(map);
        try {
            SOOS soos = new SOOS();

            StructureResponse structure = soos.getStructure();
            System.out.println(structure.toString());
            long filesProcessed = soos.sendManifestFiles(structure.getProjectId(), structure.getAnalysisId());
            StringBuilder fileProcessed = new StringBuilder("File processed: ").append(String.valueOf(filesProcessed));
            listener.getLogger().println(fileProcessed);
            LOG.info(fileProcessed.toString());

            if(filesProcessed > 0) {
                String reportUrl = soos.getStructure().getReportURL();
                switch (soos.getMode()) {
                    case RUN_AND_WAIT:
                        listener.getLogger().println(PluginConstants.RUN_AND_WAIT_MODE_SELECTED);
                        startAnalysis(soos);
                        processResult(soos);
                        listener.hyperlink(reportUrl,PluginConstants.LINK_TEXT);
                        break;
                    case ASYNC_INIT:
                        startAnalysis(soos);
                        listener.getLogger().println(PluginConstants.ASYNC_INIT_MODE_SELECTED);
                        break;
                    case ASYNC_RESULT:
                        listener.getLogger().println(PluginConstants.ASYNC_RESULT_MODE_SELECTED);
                        processResult(soos);
                        listener.hyperlink(reportUrl,PluginConstants.LINK_TEXT);
                        break;
                }
            }

        } catch (Exception e) {
            StringBuilder errorMsg = new StringBuilder("SOOS SCA cannot be done, error: ").append(e);
            if(this.onFailure.equals(PluginConstants.FAIL_THE_BUILD)){
                errorMsg.append(" - the build has failed!");
                listener.error(errorMsg.toString());
                run.setResult(Result.FAILURE);
                return;
            }
            errorMsg.append(" - Continuing the build... ");
            listener.getLogger().println(errorMsg);
        }
    }
    private void startAnalysis(SOOS soos) throws Exception {
        StructureResponse structure = soos.getStructure();
        soos.startAnalysis(structure.getProjectId(), structure.getAnalysisId());
    }

    private void processResult(SOOS soos) throws Exception {
        StructureResponse structure = soos.getStructure();
        soos.getResults(structure.getReportStatusUrl());
    }


    @Extension
    public static final class SoosSCADescriptor extends BuildStepDescriptor<Builder> {

        String apiBaseURI;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
          return true;
        }

        @Override
        public String getDisplayName() {
          return PluginConstants.DISPLAY_NAME;
        }

        public FormValidation doCheckProjectName(@QueryParameter String projectName) {

            if( StringUtils.isBlank(projectName) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_NOT_BE_NULL);
            } else if( projectName.length() < PluginConstants.MIN_NUMBER_OF_CHARACTERS ) {
              return FormValidation.errorWithMarkup(ErrorMessage.shouldBeMoreThanXCharacters(PluginConstants.MIN_NUMBER_OF_CHARACTERS));
            }
            return FormValidation.ok();
        }
        public FormValidation doCheckResultMaxWait(@QueryParameter String resultMaxWait) {

            if( !Utils.validateIsNumeric(resultMaxWait) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
            }
            return FormValidation.ok();
        }
        public FormValidation doCheckResultPollingInterval(@QueryParameter String resultPollingInterval) {

            if( !Utils.validateIsNumeric(resultPollingInterval) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
            }
            return FormValidation.ok();
        }

        public void doCheckApiBaseURI(@QueryParameter String apiBaseURI){
            this.apiBaseURI = apiBaseURI;
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

        public String getDefaultBaseURI() {
            return StringUtils.isEmpty(apiBaseURI) ? Constants.SOOS_DEFAULT_API_URL : this.apiBaseURI;
        }

    }

    private Map<String, String> populateContext(EnvVars env) {
        Map<String, String> map = new HashMap<>();

        String dirsToExclude = addSoosDirToExclusion(this.dirsToExclude);
        map.put(Constants.PARAM_PROJECT_NAME_KEY, this.projectName);
        map.put(Constants.PARAM_MODE_KEY, this.mode);
        map.put(Constants.PARAM_ON_FAILURE_KEY, this.onFailure);
        map.put(Constants.PARAM_DIRS_TO_EXCLUDE_KEY, dirsToExclude);
        map.put(Constants.PARAM_FILES_TO_EXCLUDE_KEY, this.filesToExclude);
        map.put(Constants.PARAM_WORKSPACE_DIR_KEY, env.get(PluginConstants.WORKSPACE));
        map.put(Constants.PARAM_CHECKOUT_DIR_KEY, env.get(PluginConstants.WORKSPACE));
        map.put(Constants.PARAM_ANALYSIS_RESULT_MAX_WAIT_KEY, this.resultMaxWait);
        map.put(Constants.PARAM_ANALYSIS_RESULT_POLLING_INTERVAL_KEY, this.resultPollingInterval);
        map.put(Constants.PARAM_OPERATING_ENVIRONMENT_KEY, this.operatingSystem);
        map.put(Constants.PARAM_API_BASE_URI_KEY, this.apiBaseURI);
        map.put(Constants.PARAM_BRANCH_NAME_KEY, this.branchName);
        map.put(Constants.PARAM_BRANCH_URI_KEY, this.branchURI);
        map.put(Constants.PARAM_COMMIT_HASH_KEY, this.commitHash);
        map.put(Constants.PARAM_BUILD_VERSION_KEY, this.buildVersion);
        map.put(Constants.PARAM_BUILD_URI_KEY, this.buildURI);
        map.put(Constants.PARAM_INTEGRATION_NAME_KEY, PluginConstants.INTEGRATION_NAME);
        map.put(PluginConstants.SOOS_CLIENT_ID, env.get(PluginConstants.SOOS_CLIENT_ID));
        map.put(PluginConstants.SOOS_API_KEY, env.get(PluginConstants.SOOS_API_KEY));

        if(StringUtils.isBlank(this.resultMaxWait)) {
            map.put(Constants.PARAM_ANALYSIS_RESULT_MAX_WAIT_KEY, String.valueOf(Constants.MIN_RECOMMENDED_ANALYSIS_RESULT_MAX_WAIT));
        }
        if(StringUtils.isBlank(this.resultPollingInterval)) {
            map.put(Constants.PARAM_ANALYSIS_RESULT_POLLING_INTERVAL_KEY, String.valueOf(Constants.MIN_ANALYSIS_RESULT_POLLING_INTERVAL));
        }
        if(StringUtils.isBlank(this.apiBaseURI)){
            map.put(Constants.PARAM_API_BASE_URI_KEY, Constants.SOOS_DEFAULT_API_URL);
        }

        return map;
    }

    private void setEnvProperties(Map<String, String> map){
        map.forEach((key, value) -> {
            if(StringUtils.isNotBlank(value)) {
                System.setProperty(key, value);
            }
        });
    }

    private String addSoosDirToExclusion(String dirs){
        if(StringUtils.isNotBlank(dirs)){
            StringBuilder stringBuilder = new StringBuilder(dirs).append(",").append(PluginConstants.SOOS_DIR_NAME);
            return stringBuilder.toString();
        }
        return PluginConstants.SOOS_DIR_NAME;
    }

    private Map<String, String> getEnvironmentVariables(){
        Jenkins jenkins = Jenkins.get();
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = jenkins.getGlobalNodeProperties();
        List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);
        EnvironmentVariablesNodeProperty envVarNodeProperty = envVarsNodePropertyList.get(0);

        return new LinkedHashMap<>(envVarNodeProperty.getEnvVars());
    }
}
