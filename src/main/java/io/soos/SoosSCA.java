
package io.soos;
import java.io.*;
import java.util.*;

import hudson.*;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.soos.commons.ErrorMessage;
import io.soos.commons.PluginConstants;
import io.soos.domain.OnFailure;
import io.soos.integration.commons.Constants;
import io.soos.integration.domain.SOOS;
import io.soos.integration.domain.analysis.AnalysisResultResponse;
import io.soos.integration.domain.scan.ScanResponse;
import io.soos.web.ResultDisplayAction;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

import static com.sun.activation.registries.LogSupport.log;

@Getter
@Setter
public class SoosSCA extends Builder implements SimpleBuildStep {

    private static final Logger LOG = LoggerFactory.getLogger(SoosSCA.class);

    private Secret SOOSClientId;
    private Secret SOOSApiKey;
    private String projectName;
    private String onFailure;
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
    private String packageManagers;

    @DataBoundConstructor
    public SoosSCA(Secret SOOSClientId, Secret SOOSApiKey, String projectName, String onFailure, String resultMaxWait,
                   String resultPollingInterval, String apiBaseURI, String dirsToExclude, String filesToExclude, String commitHash, String branchName,
                   String branchURI, String buildVersion, String buildURI, String packageManagers) {
        this.SOOSClientId = SOOSClientId;
        this.SOOSApiKey = SOOSApiKey;
        this.projectName = projectName;
        this.onFailure = onFailure;
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
        this.packageManagers = packageManagers;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        Map<String, String> map = new HashMap<>();
        map.putAll(populateContext(env, run));
        setEnvProperties(map);
        String resultURL = null;
        try {
            SOOS soos = new SOOS();
            soos.getContext().setScriptVersion(Utils.getVersionFromProperties());
            ScanResponse scan;
            AnalysisResultResponse result;
            LOG.info("--------------------------------------------");
            listener.getLogger().println("SOOS SCA Scan");
            listener.getLogger().println("--------------------------------------------");
            scan = soos.startAnalysis();
            listener.getLogger().println("Analysis request is running");
            result = soos.getResults(scan.getScanStatusUrl());
            resultURL = result.getScanUrl();
            listener.hyperlink(result.getScanUrl(), PluginConstants.LINK_TEXT);
            listener.getLogger().println("Violations found: " + result.getViolations() + " | Vulnerabilities found: " + result.getVulnerabilities() );
            LOG.info("Scan analysis finished successfully. To see the results go to: {}", result.getScanUrl());

        } catch (Exception e) {
            StringBuilder errorMsg = new StringBuilder("SOOS SCA cannot be done, error: ").append(e);
            if (this.onFailure.equals(PluginConstants.FAIL_THE_BUILD)) {
                errorMsg.append(" - the build has failed!");
                listener.error(errorMsg.toString());
                run.setResult(Result.FAILURE);
                return;
            }
            errorMsg.append(" - Continuing the build... ");
            listener.getLogger().println(errorMsg);
        }
        run.addAction(new ResultDisplayAction(run, resultURL));
    }

    private String createCustomDisplayName(Run<?, ?> run, String mode) throws IOException {
        StringBuilder displayNameText = new StringBuilder("#");
        displayNameText.append(run.getNumber());
        displayNameText.append(" - ");
        displayNameText.append(" ");
        return displayNameText.toString();
    }

    @Extension
    public static final class SoosSCADescriptor extends BuildStepDescriptor<Builder> {
        private Secret SOOSClientId;
        private Secret SOOSApiKey;
        String apiBaseURI;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public void setSOOSApiKey(Secret SOOSApiKey) {
            this.SOOSApiKey = SOOSApiKey;
        }

        public Secret getSOOSApiKey() {
            return SOOSApiKey;
        }

        public void setSOOSClientId(Secret SOOSClientId) {
            this.SOOSClientId = SOOSClientId;
        }

        public Secret getSOOSClientId() {
            return SOOSClientId;
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            return true;
        }

        @Override
        public String getDisplayName() {
            return PluginConstants.DISPLAY_NAME;
        }

        public FormValidation doCheckProjectName(@QueryParameter String projectName) {
            if (StringUtils.isBlank(projectName)) {
                return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_NOT_BE_NULL);
            } else if (projectName.length() < PluginConstants.MIN_NUMBER_OF_CHARACTERS) {
                return FormValidation.errorWithMarkup(ErrorMessage.shouldBeMoreThanXCharacters(PluginConstants.MIN_NUMBER_OF_CHARACTERS));
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckResultMaxWait(@QueryParameter String resultMaxWait) {
            if (!StringUtils.isNumeric(resultMaxWait)) {
                return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckResultPollingInterval(@QueryParameter String resultPollingInterval) {
            if (!StringUtils.isNumeric(resultPollingInterval)) {
                return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
            }
            return FormValidation.ok();
        }

        public void doCheckApiBaseURI(@QueryParameter String apiBaseURI) {
            this.apiBaseURI = apiBaseURI;
        }

        public int getDefaultAnalysisResultMaxWait() {
            return Constants.MIN_RECOMMENDED_ANALYSIS_RESULT_MAX_WAIT;
        }

        public int getDefaultAnalysisResultPollingInterval() {
            return Constants.MIN_ANALYSIS_RESULT_POLLING_INTERVAL;
        }

        public String getDefaultBaseURI() {
            return Constants.SOOS_DEFAULT_API_URL;
        }


        public ListBoxModel doFillOnFailureItems() {
            ListBoxModel list = new ListBoxModel();
            list.add(OnFailure.CONTINUE_ON_FAILURE.getName(), OnFailure.CONTINUE_ON_FAILURE.getValue());
            list.add(OnFailure.FAIL_THE_BUILD.getName(), OnFailure.FAIL_THE_BUILD.getValue());
            return list;
        }
    }

    private Map<String, String> populateContext(EnvVars env, Run<?, ?> run) throws AbortException {
        Map<String, String> map = new HashMap<>();

        String branchName = Utils.getBranchName(env.get(PluginConstants.GIT_BRANCH));
        String dirsToExclude = addSoosDirToExclusion(this.dirsToExclude);
        map.put(Constants.SOOS_CLIENT_ID, getSOOSClientId().getPlainText());
        map.put(Constants.SOOS_API_KEY, getSOOSApiKey().getPlainText());
        map.put(Constants.PARAM_PROJECT_NAME_KEY, this.projectName);
        map.put(Constants.PARAM_ON_FAILURE_KEY, this.onFailure);
        map.put(Constants.PARAM_DIRS_TO_EXCLUDE_KEY, dirsToExclude);
        map.put(Constants.PARAM_FILES_TO_EXCLUDE_KEY, this.filesToExclude);
        map.put(Constants.PARAM_WORKSPACE_DIR_KEY, env.get(PluginConstants.WORKSPACE));
        map.put(Constants.PARAM_CHECKOUT_DIR_KEY, env.get(PluginConstants.WORKSPACE));
        map.put(Constants.PARAM_ANALYSIS_RESULT_MAX_WAIT_KEY, this.resultMaxWait);
        map.put(Constants.PARAM_ANALYSIS_RESULT_POLLING_INTERVAL_KEY, this.resultPollingInterval);
        map.put(Constants.PARAM_OPERATING_ENVIRONMENT_KEY, Utils.getOperatingSystem());
        map.put(Constants.PARAM_API_BASE_URI_KEY, this.apiBaseURI);
        map.put(Constants.PARAM_BRANCH_NAME_KEY, branchName);
        map.put(Constants.PARAM_BRANCH_URI_KEY, env.get(PluginConstants.GIT_URL));
        map.put(Constants.PARAM_COMMIT_HASH_KEY, env.get(PluginConstants.GIT_COMMIT));
        map.put(Constants.PARAM_BUILD_VERSION_KEY, env.get(PluginConstants.BUILD_ID));
        map.put(Constants.PARAM_BUILD_URI_KEY, env.get(PluginConstants.BUILD_URL));
        map.put(Constants.PARAM_INTEGRATION_NAME_KEY, PluginConstants.INTEGRATION_NAME);
        map.put(Constants.PARAM_PACKAGE_MANAGERS_KEY, this.packageManagers);
        if (StringUtils.isBlank(this.resultMaxWait)) {
            map.put(Constants.PARAM_ANALYSIS_RESULT_MAX_WAIT_KEY, String.valueOf(Constants.MIN_RECOMMENDED_ANALYSIS_RESULT_MAX_WAIT));
        }
        if (StringUtils.isBlank(this.resultPollingInterval)) {
            map.put(Constants.PARAM_ANALYSIS_RESULT_POLLING_INTERVAL_KEY, String.valueOf(Constants.MIN_ANALYSIS_RESULT_POLLING_INTERVAL));
        }
        if (StringUtils.isBlank(this.apiBaseURI)) {
            map.put(Constants.PARAM_API_BASE_URI_KEY, Constants.SOOS_DEFAULT_API_URL);
        }

        final String user = getChangeUser(run);
        if(!user.isEmpty()) {
            map.put(Constants.PARAM_CONTRIBUTING_DEVELOPER_KEY, user);
            map.put(Constants.PARAM_CONTRIBUTING_DEVELOPER_ENV_KEY, "JENKINS_ENTRY_USER");
        }
        return map;
    }

    private void setEnvProperties(Map<String, String> map) {
        map.forEach((key, value) -> {
                System.setProperty(key, value);
        });
    }

    private String addSoosDirToExclusion(String dirs) {
        if (StringUtils.isNotBlank(dirs)) {
            StringBuilder stringBuilder = new StringBuilder(dirs).append(",").append(PluginConstants.SOOS_DIR_NAME);
            return stringBuilder.toString();
        }
        return PluginConstants.SOOS_DIR_NAME;
    }

    private String getVersionFromProperties() {
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

    private String getChangeUser(Run<?, ?> run) throws AbortException {
        String userName = null;
        Cause.UserIdCause user = (Cause.UserIdCause) run.getCause(Cause.UserIdCause.class);
        if (user != null && user.getUserName() != null) {
            log("Build initiated by: " + user.getUserName());
            if (!user.getUserName().contains("anonymous")) {
                userName = user.getUserName();
            }
        }
        if (userName == null) {
            ItemGroup<?> ig = run.getParent().getParent();
            nextItem: for (Item item : ig.getItems()) {
                if (!item.getFullDisplayName().equals(run.getFullDisplayName())
                        && !item.getFullDisplayName().equals(run.getParent().getFullDisplayName())) {
                    continue;
                }
                for (Job<?, ?> job : item.getAllJobs()) {
                    if (job instanceof AbstractProject<?, ?>) {
                        AbstractProject<?, ?> p = (AbstractProject<?, ?>) job;
                        for (AbstractBuild<?, ?> b : p.getBuilds()) {
                            for (ChangeLogSet.Entry entry : b.getChangeSet()) {
                                if (entry.getAuthor() != null) {
                                    userName = entry.getAuthor().getFullName();
                                    break nextItem;
                                }
                            }
                        }
                    }
                }
            }
        }
        return userName;
    }

}
