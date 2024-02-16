package io.soos;

import hudson.EnvVars;
import io.soos.commons.PluginConstants;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.Scanner;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);


    public static String getBuildPath(EnvVars env, Integer previousBuild){
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

    public static void saveReportStatusUrl(String reportStatusUrl, EnvVars env) {
        String resultFilePath = getBuildPath(env, null);
        File file = new File(resultFilePath);
        try {
            file.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(reportStatusUrl);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getReportStatusUrl(EnvVars env, Integer previousBuild){
        String resultFilePath = getBuildPath(env, previousBuild);
        File file = new File(resultFilePath);
        String resultStatusUrl = "";
        try {
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                resultStatusUrl = reader.nextLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultStatusUrl;
    }

    public static String getVersionFromProperties(){
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try {
            model = reader.read(new FileReader(PluginConstants.POM_FILE));
            return model.getVersion();
        } catch (XmlPullParserException | IOException e) {
            StringBuilder error = new StringBuilder("Cannot read file ").append("'").append(PluginConstants.POM_FILE).append("'");
            LOG.error(error.toString(), e);
        }
        return null;
    }

    public static String getOperatingSystem() {
        return System.getProperty(PluginConstants.OS_NAME).toLowerCase();
    }

    public static String getBranchName(String branchName){
        String[] arr = branchName.split("/");

        return arr.length == 2 ? arr[1] : branchName;
    }
}
