package io.soos.web;

import hudson.model.Action;
import hudson.model.Run;
import io.soos.commons.PluginConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultDisplayAction implements Action {

    private Run<?, ?> run;
    private String resultURL;


    public String getBuildUrl() {
        return this.run.getUrl();
    }

    public String getProjectUrl() {
        return run.getParent().getUrl();
    }

    public String getResultURL(){
        return this.resultURL;
    }

    @Override
    public String getIconFileName() {
        return StringUtils.isNotEmpty(resultURL) ? PluginConstants.SOOS_ICON_FILE_PATH : null;
    }

    @Override
    public String getDisplayName() {
        return PluginConstants.SOOS_RESULT_TAB_NAME;
    }

    @Override
    public String getUrlName() {
        return StringUtils.isNotEmpty(resultURL) ? PluginConstants.SOOS_RESULT_VIEW_URL : null ;
    }

}
