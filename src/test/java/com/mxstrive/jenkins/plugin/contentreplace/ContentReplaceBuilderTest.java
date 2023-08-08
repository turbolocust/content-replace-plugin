package com.mxstrive.jenkins.plugin.contentreplace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class ContentReplaceBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final String fileEncoding = "UTF-8";
    private final String lineSeparator = "Unix";
    private final String content = "Version=0.0.0";

    private List<FileContentReplaceConfig> buildConfigurations(
            String filePath, String search, String replace) throws IOException {
        List<FileContentReplaceConfig> configs = new ArrayList<>();
        List<FileContentReplaceItemConfig> cfgs = new ArrayList<>();
        FileContentReplaceItemConfig cfg = new FileContentReplaceItemConfig();
        cfg.setSearch(search);
        cfg.setReplace(replace);
        cfg.setMatchCount(0);
        cfg.setVerbose(true);
        cfgs.add(cfg);
        FileContentReplaceConfig config = new FileContentReplaceConfig(filePath, fileEncoding, cfgs);
        config.setLineSeparator(lineSeparator);
        configs.add(config);
        return configs;
    }

    @Test
    public void testBuild() throws Exception {
        final String searchString = "(Version=)\\d+.\\d+.\\d+";
        final String replaceString = "$11.0.${BUILD_ID}";

        File file = File.createTempFile(fileEncoding, content);
        FileUtils.write(file, content, Charset.forName(fileEncoding));
        List<FileContentReplaceConfig> configs = buildConfigurations(
                file.getAbsolutePath(), searchString, replaceString);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String logMessage = "   > replace times: 1, [" + searchString + "] => [$11.0." + build.getNumber() + "]";
        jenkins.assertLogContains(logMessage, build);

        Assert.assertEquals(FileUtils.readFileToString(file,
                Charset.forName(fileEncoding)),
                "Version=1.0." + build.getNumber());

        file.delete();
    }

    @Test
    public void testBuildQuiet() throws Exception {
        final String searchString = "(Version=)\\d+.\\d+.\\d+";
        final String replaceString = "$11.0.${BUILD_ID}";

        File file = File.createTempFile(fileEncoding, content);
        FileUtils.write(file, content, Charset.forName(fileEncoding));
        List<FileContentReplaceConfig> configs = buildConfigurations(
                file.getAbsolutePath(), searchString, replaceString);

        configs.get(0).getConfigs().get(0).setVerbose(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String logMessage = "   > replace : [Version=0.0.0] => [Version=1.0." + build.getNumber() + "]";
        jenkins.assertLogNotContains(logMessage, build);
        logMessage = "   > replace times: 1, [" + searchString + "] => [$11.0." + build.getNumber() + "]";
        jenkins.assertLogNotContains(logMessage, build);

        Assert.assertEquals(FileUtils.readFileToString(file,
                Charset.forName(fileEncoding)),
                "Version=1.0." + build.getNumber());

        file.delete();
    }

}