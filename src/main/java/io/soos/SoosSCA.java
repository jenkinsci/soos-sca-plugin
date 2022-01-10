
package io.soos;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.soos.commons.ErrorMessage;
import io.soos.commons.PluginConstants;
import io.soos.domain.Mode;
import io.soos.domain.OnFailure;
import io.soos.domain.OperatingSystem;
import io.soos.integration.commons.Constants;
import io.soos.integration.domain.SOOS;
import io.soos.integration.domain.analysis.AnalysisResultResponse;
import io.soos.integration.domain.structure.StructureResponse;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
    private String reportStatusUrl;

    @DataBoundConstructor
    public SoosSCA(String projectName, String mode, String onFailure, String operatingSystem, String resultMaxWait,
                        String resultPollingInterval, String apiBaseURI, String dirsToExclude, String filesToExclude, String commitHash, String branchName,
                        String branchURI, String buildVersion, String buildURI, String reportStatusUrl) {

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
        this.reportStatusUrl = reportStatusUrl;
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
            StructureResponse structure = null;
            AnalysisResultResponse result = null;
            LOG.info("--------------------------------------------");
            switch (soos.getMode()) {
                case RUN_AND_WAIT:
                    listener.getLogger().println(PluginConstants.RUN_AND_WAIT_MODE_SELECTED);
                    LOG.info("Run and Wait Scan");
                    LOG.info("--------------------------------------------");
                    structure = soos.startAnalysis();
                    LOG.info("Analysis request is running");
                    result = soos.getResults(structure.getReportStatusUrl());
                    listener.hyperlink(result.getReportUrl(),PluginConstants.LINK_TEXT);
                    LOG.info("Scan analysis finished successfully. To see the results go to: {}", result.getReportUrl());
                    run.setDisplayName(createCustomDisplayName(run, Mode.RUN_AND_WAIT.getName()));
                    break;
                case ASYNC_INIT:
                    listener.getLogger().println(PluginConstants.ASYNC_INIT_MODE_SELECTED);
                    LOG.info("Async Init Scan");
                    LOG.info("--------------------------------------------");
                    structure = soos.startAnalysis();
                    StringBuilder reportStatusText = new StringBuilder("Analysis request is running, access the report status using this link: \n");
                    reportStatusText.append(structure.getReportStatusUrl());
                    listener.getLogger().println(reportStatusText);
                    LOG.info("Analysis request is running, access the report status using this link: {}", structure.getReportStatusUrl());
                    run.setDisplayName(createCustomDisplayName(run, Mode.ASYNC_INIT.getName()));
                    break;
                case ASYNC_RESULT:
                    listener.getLogger().println(PluginConstants.ASYNC_RESULT_MODE_SELECTED);
                    LOG.info("Async Result Scan");
                    LOG.info("--------------------------------------------");
                    LOG.info("Checking Scan Status from: {}", this.reportStatusUrl);
                    result = soos.getResults(this.reportStatusUrl);
                    listener.hyperlink(result.getReportUrl(),PluginConstants.LINK_TEXT);
                    LOG.info("Scan analysis finished successfully. To see the results go to: {}", result.getReportUrl());
                    run.setDisplayName(createCustomDisplayName(run, Mode.ASYNC_RESULT.getName()));
                    break;
                default:
                    throw new Exception("Invalid SCA Mode");
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

    private String createCustomDisplayName (Run<?, ?> run, String mode) throws IOException {
        StringBuilder displayNameText = new StringBuilder("#");
        displayNameText.append(run.getNumber());
        displayNameText.append(" - ");
        displayNameText.append(mode);
        displayNameText.append(" ");
        displayNameText.append("mode.");
        return displayNameText.toString();
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
            if( !StringUtils.isNumeric(resultMaxWait) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
            }
            return FormValidation.ok();
        }
        public FormValidation doCheckResultPollingInterval(@QueryParameter String resultPollingInterval) {
            if( !StringUtils.isNumeric(resultPollingInterval) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckReportStatusUrl(@QueryParameter String mode, @QueryParameter String reportStatusUrl) {
            if( StringUtils.equals(mode, Mode.ASYNC_RESULT.getValue()) && StringUtils.isBlank(reportStatusUrl) ) {
              return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_NOT_BE_NULL);
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

        public int getDefaultAnalysisResultMaxWait(){
            return Constants.MIN_RECOMMENDED_ANALYSIS_RESULT_MAX_WAIT;
        }

        public int getDefaultAnalysisResultPollingInterval(){
            return Constants.MIN_ANALYSIS_RESULT_POLLING_INTERVAL;
        }

        public String getDefaultBaseURI() {
            return Constants.SOOS_DEFAULT_API_URL;
        }

        public ListBoxModel doFillModeItems() {
            ListBoxModel list = new ListBoxModel();
            list.add(Mode.RUN_AND_WAIT.getName(), Mode.RUN_AND_WAIT.getValue());
            list.add(Mode.ASYNC_INIT.getName(), Mode.ASYNC_INIT.getValue());
            list.add(Mode.ASYNC_RESULT.getName(), Mode.ASYNC_RESULT.getValue());
            return list;
        }

        public ListBoxModel doFillOnFailureItems() {
            ListBoxModel list = new ListBoxModel();
            list.add(OnFailure.FAIL_THE_BUILD.getName(), OnFailure.FAIL_THE_BUILD.getValue());
            list.add(OnFailure.CONTINUE_ON_FAILURE.getName(), OnFailure.CONTINUE_ON_FAILURE.getValue());
            return list;
        }

        public ListBoxModel doFillOperatingSystemItems() {
            ListBoxModel list = new ListBoxModel();
            list.add(OperatingSystem.LINUX.getName(), OperatingSystem.LINUX.getValue());
            list.add(OperatingSystem.MAC.getName(), OperatingSystem.MAC.getValue());
            list.add(OperatingSystem.WINDOWS.getName(), OperatingSystem.WINDOWS.getValue());
            return list;
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
    private String getVersionFromProperties(){
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            model = reader.read(new FileReader(PluginConstants.POM_FILE));
            return model.getVersion();
        } catch (XmlPullParserException | IOException e) {
            StringBuilder error = new StringBuilder("Cannot read file ").append("'").append(PluginConstants.POM_FILE).append("'");
            LOG.error(error.toString(), e);
        }
        return null;
    }
}
