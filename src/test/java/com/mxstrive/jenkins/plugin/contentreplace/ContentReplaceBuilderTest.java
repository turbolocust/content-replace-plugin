package com.mxstrive.jenkins.plugin.contentreplace;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
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

    private List<FileContentReplaceConfig> buildConfigurations(
            String filePath, String search, String replace) throws IOException {

        List<FileContentReplaceConfig> configs = new ArrayList<>();
        List<FileContentReplaceItemConfig> cfgs = new ArrayList<>();
        FileContentReplaceItemConfig cfg = new FileContentReplaceItemConfig();
        cfg.setSearch(search);
        cfg.setReplace(replace);
        cfg.setMatchCount(0);
        cfg.setVerbose(true); // is necessary for asserting log messages
        cfgs.add(cfg);
        FileContentReplaceConfig config = new FileContentReplaceConfig(filePath, fileEncoding, cfgs);
        config.setLineSeparator(lineSeparator);
        configs.add(config);
        return configs;
    }

    @Test
    public void testBuild() throws Exception {
        final String content = "Version=0.0.0";
        final String searchStringRegex = "(Version=)\\d+.\\d+.\\d+";
        final String replaceString = "$11.0.${BUILD_ID}";

        File file = File.createTempFile("content-replace_", null);
        FileUtils.write(file, content, Charset.forName(fileEncoding));
        List<FileContentReplaceConfig> configs = buildConfigurations(
                file.getAbsolutePath(), searchStringRegex, replaceString);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String logMessage = "   > replace times: 1, [" + searchStringRegex + "] => [$11.0." + build.getNumber() + "]";
        jenkins.assertLogContains(logMessage, build);

        Assert.assertEquals(FileUtils.readFileToString(file,
                Charset.forName(fileEncoding)),
                "Version=1.0." + build.getNumber());

        file.delete();
    }

    @Test
    public void testBuildQuiet() throws Exception {
        final String content = "Version=0.0.0";
        final String searchStringRegex = "(Version=)\\d+.\\d+.\\d+";
        final String replaceString = "$11.0.${BUILD_ID}";

        File file = File.createTempFile("content-replace_", null);
        FileUtils.write(file, content, Charset.forName(fileEncoding));
        List<FileContentReplaceConfig> configs = buildConfigurations(
                file.getAbsolutePath(), searchStringRegex, replaceString);

        configs.get(0).getConfigs().get(0).setVerbose(false);
        configs.get(0).setLineSeparator("Windows");

        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String logMessage = "   > replace : [Version=0.0.0] => [Version=1.0." + build.getNumber() + "]";
        jenkins.assertLogNotContains(logMessage, build);
        logMessage = "   > replace times: 1, [" + searchStringRegex + "] => [$11.0." + build.getNumber() + "]";
        jenkins.assertLogNotContains(logMessage, build);

        Assert.assertEquals(FileUtils.readFileToString(file,
                Charset.forName(fileEncoding)),
                "Version=1.0." + build.getNumber());

        file.delete();
    }

    @Test
    public void testBuildMultiLineFile() throws Exception {
        final String sampleChartYamlBase64 = "YXBpVmVyc2lvbjogdjIKbmFtZTogZGVtbwpkZXNjcmlwdGlvbjogQSBkZW1vIGNoYXJ0LgoKIyBBIGNoYXJ0IGNhbiBiZSBlaXRoZXIgYW4gJ2FwcGxpY2F0aW9uJyBvciBhICdsaWJyYXJ5JyBjaGFydC4KIwojIEFwcGxpY2F0aW9uIGNoYXJ0cyBhcmUgYSBjb2xsZWN0aW9uIG9mIHRlbXBsYXRlcyB0aGF0IGNhbiBiZSBwYWNrYWdlZCBpbnRvIHZlcnNpb25lZCBhcmNoaXZlcwojIHRvIGJlIGRlcGxveWVkLgojCiMgTGlicmFyeSBjaGFydHMgcHJvdmlkZSB1c2VmdWwgdXRpbGl0aWVzIG9yIGZ1bmN0aW9ucyBmb3IgdGhlIGNoYXJ0IGRldmVsb3Blci4gVGhleSdyZSBpbmNsdWRlZCBhcwojIGEgZGVwZW5kZW5jeSBvZiBhcHBsaWNhdGlvbiBjaGFydHMgdG8gaW5qZWN0IHRob3NlIHV0aWxpdGllcyBhbmQgZnVuY3Rpb25zIGludG8gdGhlIHJlbmRlcmluZwojIHBpcGVsaW5lLiBMaWJyYXJ5IGNoYXJ0cyBkbyBub3QgZGVmaW5lIGFueSB0ZW1wbGF0ZXMgYW5kIHRoZXJlZm9yZSBjYW5ub3QgYmUgZGVwbG95ZWQuCnR5cGU6IGFwcGxpY2F0aW9uCgojIFBsYWNlaG9sZGVyIGZvciBhIHZlcnNpb247IHRoZSBhY3R1YWwgdmVyc2lvbiBpcyBhdXRvbWF0aWNhbGx5IHNldCBieSBKZW5raW5zIGFuZCBkZXRlcm1pbmVkIHZpYSBHSVQgdGFncwp2ZXJzaW9uOiAxLjIuMwoKIyBUaGlzIGlzIHRoZSB2ZXJzaW9uIG51bWJlciBvZiB0aGUgYXBwbGljYXRpb24gYmVpbmcgZGVwbG95ZWQuIFRoaXMgdmVyc2lvbiBudW1iZXIgc2hvdWxkIGJlCiMgaW5jcmVtZW50ZWQgZWFjaCB0aW1lIHlvdSBtYWtlIGNoYW5nZXMgdG8gdGhlIGFwcGxpY2F0aW9uLiBWZXJzaW9ucyBhcmUgbm90IGV4cGVjdGVkIHRvCiMgZm9sbG93IFNlbWFudGljIFZlcnNpb25pbmcuIFRoZXkgc2hvdWxkIHJlZmxlY3QgdGhlIHZlcnNpb24gdGhlIGFwcGxpY2F0aW9uIGlzIHVzaW5nLgojIEl0IGlzIHJlY29tbWVuZGVkIHRvIHVzZSBpdCB3aXRoIHF1b3Rlcy4KYXBwVmVyc2lvbjogIjEuMC4wIgoKIyBEZXBlbmRlbmNpZXMgdG8gb3RoZXIgY2hhcnRzLCBtYWlubHkgbGlicmFyeSBjaGFydHMuCiMgUmVnYXJkaW5nIHRoZSB2ZXJzaW9uLCBzZWUgaHR0cHM6Ly9naXRodWIuY29tL01hc3Rlcm1pbmRzL3NlbXZlciNjaGVja2luZy12ZXJzaW9uLWNvbnN0cmFpbnRzCmRlcGVuZGVuY2llczoKLSBuYW1lOiBsaWItc29tZXRoaW5nCiAgdmVyc2lvbjogXjEueAogIHJlcG9zaXRvcnk6IGh0dHBzOi8vZHVtbXktYXJ0aWZhY3RvcnkvcmVwb3NpdG9yeS9teS1yZXBvc2l0b3J5";
        final String searchStringRegex = "version: \\^1.x";
        final String searchString = "version: ^1.x";
        final String replaceString = "version: 1.10";

        byte[] decodedBytes = Base64.getDecoder().decode(sampleChartYamlBase64);
        String content = new String(decodedBytes);

        File file = File.createTempFile("content-replace_", null);
        FileUtils.write(file, content, Charset.forName(fileEncoding));
        List<FileContentReplaceConfig> configs = buildConfigurations(
                file.getAbsolutePath(), searchStringRegex, replaceString);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        ContentReplaceBuilder builder = new ContentReplaceBuilder(configs);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        String logMessage = "   > replace times: 1, [" + searchStringRegex + "] => [" + replaceString + "]";
        jenkins.assertLogContains(logMessage, build);

        String expectedFileContent = content.replace(searchString, replaceString);

        Assert.assertEquals(FileUtils.readFileToString(file,
                Charset.forName(fileEncoding)), expectedFileContent);

        file.delete();
    }

}