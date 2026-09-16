package com.meera.tests.nlp.assistant;

import com.meera.config.Config;
import com.meera.pages.AssistantPage;
import com.meera.tests.AuthenticatedTest;
import com.meera.utils.ExcelDataReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Add-assistant tests. Port of {@code tests/NLP/Assistant/addAssistantTests.spec.js}.
 */
public class AddAssistantTests extends AuthenticatedTest {

    @DataProvider(name = "addAssistantData")
    public Object[][] addAssistantData() {
        return ExcelDataReader.getTestDataAsProvider(Config.ASSISTANT_DATA, "AddAssistant");
    }

    @Test(dataProvider = "addAssistantData", timeOut = 300_000)
    public void addAssistant(Map<String, String> data) {
        System.out.println("Running test case: " + data.get("testCase"));
        AssistantPage assistantPage = new AssistantPage(page);
        attachPageEventLoggers(page);

        assistantPage.goTo(Config.ASSISTANTS_URL);
        assistantPage.clickAddNewAssistant();
        assistantPage.fillAddAssistantForm(data);
        assistantPage.clickAddAssistantFinal();
        assistantPage.verifyAssistantCreated();
    }
}
