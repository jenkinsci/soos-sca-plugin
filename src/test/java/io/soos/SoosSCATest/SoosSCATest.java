package io.soos.SoosSCATest;

import io.soos.commons.PluginConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.*;

public class SoosSCATest {

    @Test
    public void versionIsNumeric(){
        String version = getVersionFromProperties();
        version = version.substring(0,1);
        assertTrue(StringUtils.isNumeric(version));
    }

    private String getVersionFromProperties(){
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            model = reader.read(new FileReader(PluginConstants.POM_FILE));
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return model.getVersion();
    }
}
