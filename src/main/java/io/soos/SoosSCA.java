package io.soos;

import java.io.*;

import hudson.*;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.soos.commons.ErrorMessage;
import io.soos.commons.PluginConstants;
import io.soos.integration.Configuration;
import io.soos.integration.Enums;
import io.soos.integration.SoosScaWrapper;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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

    private Secret SOOSApiKey;
    private Secret SOOSClientId;
    private String apiBaseURI;
    private String dirsToExclude;
    private String filesToExclude;
    private String logLevel;
    private String nodePath;
    private String onFailure;
    private String outputFormat;
    private String packageManagers;
    private String projectName;
    private Boolean verbose;

    @DataBoundConstructor
    public SoosSCA(Secret SOOSApiKey, Secret SOOSClientId, String apiBaseURI,
                   String dirsToExclude, String filesToExclude, String logLevel, String nodePath,
                   String onFailure, String outputFormat, String packageManagers, String projectName,
                   Boolean verbose) {

        this.SOOSApiKey = SOOSApiKey;
        this.SOOSClientId = SOOSClientId;
        this.apiBaseURI = apiBaseURI;
        this.dirsToExclude = dirsToExclude;
        this.filesToExclude = filesToExclude;
        this.logLevel = logLevel;
        this.nodePath = nodePath;
        this.onFailure = onFailure;
        this.outputFormat = outputFormat;
        this.packageManagers = packageManagers;
        this.projectName = projectName;
        this.verbose = verbose;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        try {
            Configuration configuration = new Configuration();
            configuration.setApiKey(SOOSApiKey.getPlainText());
            configuration.setApiURL(apiBaseURI);
            configuration.setBranchName(Utils.getBranchName(env.get(PluginConstants.GIT_BRANCH, "")));
            configuration.setBranchURI(env.get(PluginConstants.GIT_URL));
            configuration.setBuildURI(env.get(PluginConstants.BUILD_URL));
            configuration.setBuildVersion(env.get(PluginConstants.BUILD_ID));
            configuration.setClientId(SOOSClientId.getPlainText());
            configuration.setCommitHash(env.get(PluginConstants.GIT_COMMIT));
            configuration.setDirectoriesToExclude(dirsToExclude);
            configuration.setFilesToExclude(filesToExclude);
            configuration.setIntegrationName(PluginConstants.INTEGRATION_NAME);
            configuration.setLogLevel(logLevel);
            configuration.setNodePath(nodePath);
            configuration.setOnFailure(onFailure);
            configuration.setOutputFormat(outputFormat);
            configuration.setPackageManagers(packageManagers);
            configuration.setProjectName(projectName);
            configuration.setVerbose(verbose);

            final String user = getChangeUser(run);
            if (user != null && !user.isEmpty()) {
                configuration.setContributingDeveloperId(user);
                configuration.setContributingDeveloperSource("JENKINS_ENTRY_USER");
            }

            SoosScaWrapper soosScaWrapper = new SoosScaWrapper(configuration, listener.getLogger());
            final int exitCode = soosScaWrapper.runSca();

            if (exitCode != 0) {
                StringBuilder errorMsg = new StringBuilder("SOOS SCA failed with exit code: ").append(exitCode);
                if (this.onFailure.equalsIgnoreCase(PluginConstants.FAIL_THE_BUILD)) {
                    errorMsg.append(" - the build has failed!");
                    listener.error(errorMsg.toString());
                    run.setResult(Result.FAILURE);
                    return;
                }
                errorMsg.append(" - Continuing the build... ");
                run.setResult(Result.UNSTABLE);
                listener.getLogger().println(errorMsg);
            }

        } catch (Exception e) {
            StringBuilder errorMsg = new StringBuilder("SOOS SCA cannot be done, error: ").append(e);
            if (this.onFailure.equalsIgnoreCase(PluginConstants.FAIL_THE_BUILD)) {
                errorMsg.append(" - the build has failed!");
                listener.error(errorMsg.toString());
                run.setResult(Result.FAILURE);
                return;
            }
            errorMsg.append(" - Continuing the build... ");
            listener.getLogger().println(errorMsg);
        }
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

        public FormValidation doCheckResultPollingInterval(@QueryParameter String resultPollingInterval) {
            if (!StringUtils.isNumeric(resultPollingInterval)) {
                return FormValidation.errorWithMarkup(ErrorMessage.SHOULD_BE_A_NUMBER);
            }
            return FormValidation.ok();
        }

        public void doCheckApiBaseURI(@QueryParameter String apiBaseURI) {
            this.apiBaseURI = apiBaseURI;
        }


        public ListBoxModel doFillOnFailureItems() {
            ListBoxModel list = new ListBoxModel();
            for (Enums.OnFailure onFailure : Enums.OnFailure.values()) {
                list.add(onFailure.name(), onFailure.name());
            }
            return list;
        }

        public ListBoxModel doFillLogLevelItems() {
            ListBoxModel list = new ListBoxModel();
            for (Enums.LogLevel logLevel : Enums.LogLevel.values()) {
                list.add(logLevel.name(), logLevel.name());
            }

            return list;
        }

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
            nextItem:
            for (Item item : ig.getItems()) {
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
