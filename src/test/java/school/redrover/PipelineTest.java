package school.redrover;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;
import school.redrover.common.BaseTest;
import school.redrover.page.HomePage;

import java.util.List;
import java.util.Random;

public class PipelineTest extends BaseTest {

    private static final String PIPELINE_NAME = "PipelineName";

    private static final Random random = new Random();

    private void createPipeline(String name) {
        getDriver().findElement(By.xpath("//a[@href='/view/all/newJob']")).click();

        getDriver().findElement(By.id("name")).sendKeys(name);
        getDriver().findElement(By.className("org_jenkinsci_plugins_workflow_job_WorkflowJob")).click();
        getDriver().findElement(By.id("ok-button")).click();
        getDriver().findElement(By.name("Submit")).click();
    }

    @Test
    public void testCreateNewPipeline() {
        List<String> actualProjectList = new HomePage(getDriver())
                .clickCreateJob()
                .sendName(PIPELINE_NAME)
                .selectPipelineAndSubmit()
                .gotoHomePage()
                .getProjectList();

        Assert.assertTrue(actualProjectList.contains(PIPELINE_NAME),
                String.format("Pipeline with name '%s' was not created", PIPELINE_NAME));
    }

    @Test
    public void testCreatePipeline() throws InterruptedException {
        getDriver().findElement(By.cssSelector(".task:nth-child(1) a")).click();
        getDriver().findElement(By.cssSelector("#name")).sendKeys(PIPELINE_NAME);
        getDriver().findElement(By.cssSelector("div:first-child > ul > li:nth-child(2)")).click();
        getDriver().findElement(By.id("ok-button")).click();
        getDriver().findElement(By.xpath("//button[@name='Submit']")).click();

        Thread.sleep(2000);
        getDriver().findElement(By.xpath("//a[@href='/']/img")).click();

        Assert.assertEquals(getDriver().findElement(By.xpath("//a[@href='job/" + PIPELINE_NAME + "/']")).getText(),
                PIPELINE_NAME);
    }

    @Test
    public void testDeletePipeline() throws InterruptedException {
        getDriver().findElement(By.cssSelector(".task:nth-child(1) a")).click();
        getDriver().findElement(By.cssSelector("#name")).sendKeys(PIPELINE_NAME);
        getDriver().findElement(By.cssSelector("div:first-child > ul > li:nth-child(2)")).click();
        getDriver().findElement(By.id("ok-button")).click();
        getDriver().findElement(By.xpath("//button[@name='Submit']")).click();

        Thread.sleep(2000);
        getDriver().findElement(By.xpath("//a[@href='/']/img")).click();

        List<WebElement> countPosition = getDriver().findElements(By.cssSelector("#projectstatus > tbody > tr"));

        getDriver().findElement(By.xpath("//a[@href='job/" + PIPELINE_NAME + "/']")).click();
        getDriver().findElement(By.cssSelector(".task:nth-child(6)")).click();
        getDriver().findElement(By.xpath("//button[@data-id='ok']")).click();

        Assert.assertEquals(countPosition.size() - 1, 0);
    }

    @Test
    public void testSuccessfulBuildPipeline() {
        createPipeline(PIPELINE_NAME);

        getDriver().findElement(By.xpath("//a[@data-build-success='Build scheduled']")).click();

        getWait10().until(ExpectedConditions.elementToBeClickable(By.id("jenkins-build-history"))).click();
        getDriver().findElement(By.xpath("//a[substring-before(@href, 'console')]")).click();

        WebElement consoleOutput = getDriver().findElement(By.id("out"));
        getWait10().until(d -> consoleOutput.getText().contains("Finished:"));

        Assert.assertTrue(consoleOutput.getText().contains("Finished: SUCCESS"),
                "Build output should contain 'Finished: SUCCESS'");
    }

    @Test
    public void testAddDescription() {
        final String textDescription = "TextDescription";

        String descriptionText = new HomePage(getDriver())
                .clickNewItemOnLeftMenu()
                .sendName(PIPELINE_NAME)
                .selectPipelineAndSubmit()
                .clickSaveButton()
                .clickAddDescriptionButton()
                .addDescriptionAndSave(textDescription)
                .getDescription();

        Assert.assertEquals(descriptionText, textDescription);
    }

    @Test(dependsOnMethods = "testAddDescription")
    public void testEditDescription() {
        final String textDescription = "TextDescription";

        getWait2().until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href = 'job/%s/']".formatted(PIPELINE_NAME))))
                .click();

        getWait5().until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href = 'editDescription']")))
                .click();
        WebElement descriptionField = getDriver().findElement(By.name("description"));
        descriptionField.clear();
        descriptionField.sendKeys(textDescription);
        getDriver().findElement(By.name("Submit")).click();

        getWait5().until(ExpectedConditions.elementToBeClickable(By.id("description-link")));
        WebElement descriptionText = getWait5().until(
                ExpectedConditions.visibilityOfElementLocated(By.id("description-content")));

        Assert.assertEquals(
                descriptionText.getText(),
                textDescription,
                "Не совпал текст description после его редактирования");
    }
}
