package io.soos;

import hudson.EnvVars;
import io.soos.commons.PluginConstants;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);


    public static String getBuildPath(EnvVars env, Integer previousBuild) {
        final String JENKINS_HOME_PATH = env.get(PluginConstants.JENKINS_HOME);
        final String JOBS_DIR = PluginConstants.JOBS_DIR;
        final String JOB_BASE_NAME = env.get(PluginConstants.JOB_BASE_NAME);
        final String BUILDS_DIR = PluginConstants.BUILDS_DIR;
        final String BUILD_ID = ObjectUtils.isNotEmpty(previousBuild) ? String.valueOf(previousBuild) : env.get(PluginConstants.BUILD_ID);
        final String RESULT_URL_FILE = PluginConstants.RESULT_URL_FILE;
        String pathSeparator = PluginConstants.SLASH;
        StringBuilder buildPath = new StringBuilder();
        buildPath.append(JENKINS_HOME_PATH);
        buildPath.append(pathSeparator);
        buildPath.append(JOBS_DIR);
        buildPath.append(pathSeparator);
        buildPath.append(JOB_BASE_NAME);
        buildPath.append(pathSeparator);
        buildPath.append(BUILDS_DIR);
        buildPath.append(pathSeparator);
        buildPath.append(BUILD_ID);
        buildPath.append(pathSeparator);
        buildPath.append(RESULT_URL_FILE);

        return buildPath.toString();
    }

    public static String getOperatingSystem() {
        return System.getProperty(PluginConstants.OS_NAME).toLowerCase();
    }

    public static String getBranchName(String branchName) {
        String[] arr = branchName.split("/");

        return arr.length == 2 ? arr[1] : branchName;
    }
}
