package com.mxstrive.jenkins.plugin.contentreplace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
	private File file;
	private List<FileContentReplaceConfig> configs;

    @Before
    public void init() throws IOException {
    	file = new File(getClass().getResource(".").getPath() + "tmp.txt");
    	configs = new ArrayList<>();
    	List<FileContentReplaceItemConfig> cfgs = new ArrayList<>();
		FileContentReplaceItemConfig cfg = new FileContentReplaceItemConfig();
		cfg.setSearch("(Version=)\\d+.\\d+.\\d+");
		cfg.setReplace("$11.0.${BUILD_ID}");
		cfg.setMatchCount(0);
		cfg.setVerbose(true);
		cfgs.add(cfg);
    	FileUtils.write(file, content, Charset.forName(fileEncoding));
    	FileContentReplaceConfig config = new FileContentReplaceConfig(file.getAbsolutePath(), fileEncoding, cfgs);
    	config.setLineSeparator(lineSeparator);
    	configs.add(config);
    }

    @After
    public void clean() throws IOException {
    	FileUtils.forceDelete(file);
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("   > replace times: 1, [(Version=)\\d+.\\d+.\\d+] => [$11.0." + build.getNumber() + "]", build);
        Assert.assertEquals(FileUtils.readFileToString(file, Charset.forName(fileEncoding)), "Version=1.0." + build.getNumber());
    }

    @Test
    public void testBuildQuiet() throws Exception {
        configs.get(0).getConfigs().get(0).setVerbose(false);
        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogNotContains("   > replace : [Version=0.0.0] => [Version=1.0." + build.getNumber() + "]", build);
		jenkins.assertLogNotContains(
				"   > replace times: 1, [(Version=)\\d+.\\d+.\\d+] => [$11.0." + build.getNumber() + "]", build);
        Assert.assertEquals(FileUtils.readFileToString(file, Charset.forName(fileEncoding)), "Version=1.0." + build.getNumber());
    }

}