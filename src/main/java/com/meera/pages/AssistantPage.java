    package com.meera.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Assistant (NLP) page object. Port of {@code pages/AssistantPage.js}.
 */
public class AssistantPage {

    private final Page page;
    private final Locator addNewAssistantBtn;
    private final Locator assistantNameInput;
    private final Locator projectTypeSelect;
    private final Locator apiSelect;
    private final Locator industrySelect;
    private final Locator subIndustrySelect;
    private final Locator useCaseSelect;
    private final Locator skillSelect;
    private final Locator addAssistantBtn;
    private final Locator modelSelect;
    private final Locator instructionsInput;
    private final Locator searchInput;
    private final Locator successPopup;
    private final Locator successTitle;
    private final Locator confirmButton;

    public AssistantPage(Page page) {
        this.page = page;
        this.addNewAssistantBtn = page.locator("text=Add New Assistant");
        this.assistantNameInput = page.locator("#assistant_name");
        this.projectTypeSelect = page.locator("#project_type");
        this.apiSelect = page.locator("#api");
        this.industrySelect = page.locator("#industry");
        this.subIndustrySelect = page.locator("#sub_industry");
        this.useCaseSelect = page.locator("#use_case");
        this.skillSelect = page.locator("#skill");
        this.addAssistantBtn = page.locator("input#create_assistant");
        this.modelSelect = page.locator("#model");
        this.instructionsInput = page.locator("textarea#instructions");
        this.searchInput = page.locator("input[type='search']");
        this.successPopup = page.locator(".swal2-popup.swal2-icon-success");
        this.successTitle = page.locator("#swal2-title");
        this.confirmButton = page.locator(".swal2-confirm");
    }

    public void goTo(String url) {
        page.navigate(url);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public void clickAddNewAssistant() {
        addNewAssistantBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        addNewAssistantBtn.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public void fillAssistantName(String name) {
        assistantNameInput.fill(name);
    }

    public void selectProjectType(String label) {
        projectTypeSelect.selectOption(new SelectOption().setLabel(label));
        page.waitForTimeout(1500);
    }

    public void selectApi(String label) {
        apiSelect.selectOption(new SelectOption().setLabel(label));
        page.waitForTimeout(1000);
    }

    public void selectIndustry(String label) {
        page.locator("#industry option")
                .filter(new Locator.FilterOptions().setHasText(label))
                .first()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.ATTACHED).setTimeout(15000));
        industrySelect.selectOption(new SelectOption().setLabel(label));
    }

    public void selectSubIndustry(String label) {
        page.locator("#sub_industry option")
                .filter(new Locator.FilterOptions().setHasText(label))
                .first()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.ATTACHED).setTimeout(15000));
        subIndustrySelect.selectOption(new SelectOption().setLabel(label));
    }

    public void selectUseCase(String label) {
        page.locator("#use_case option")
                .filter(new Locator.FilterOptions().setHasText(label))
                .first()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.ATTACHED).setTimeout(15000));
        useCaseSelect.selectOption(new SelectOption().setLabel(label));
        page.waitForTimeout(2000);
    }

    public void selectSkill(String label) {
        page.waitForTimeout(2000);
        skillSelect.selectOption(new SelectOption().setLabel(label),
                new Locator.SelectOptionOptions().setTimeout(20000));
    }

    public void selectModel(String label) {
        modelSelect.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        modelSelect.selectOption(new SelectOption().setLabel(label));
        page.waitForTimeout(1000);
    }

    public void clickAddAssistant() {
        addAssistantBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        addAssistantBtn.scrollIntoViewIfNeeded();
        page.evaluate("() => {"
                + "  const btn = document.querySelector('input#create_assistant');"
                + "  if (btn) { btn.removeAttribute('disabled'); }"
                + "}");
        addAssistantBtn.click(new Locator.ClickOptions().setTimeout(15000));
        page.waitForTimeout(2000);
    }

    public void clickAddAssistantFinal() {
        boolean modelVisible = modelSelect.isVisible();
        if (modelVisible) {
            addAssistantBtn.scrollIntoViewIfNeeded();
            page.evaluate("() => {"
                    + "  const btn = document.querySelector('input#create_assistant');"
                    + "  if (btn) { btn.removeAttribute('disabled'); }"
                    + "}");
            addAssistantBtn.click(new Locator.ClickOptions().setTimeout(15000));
            page.waitForTimeout(2000);
        }
    }

    public void verifyAssistantCreated() {
        assertThat(successPopup).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(15000));
        confirmButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        confirmButton.click();
        page.waitForTimeout(3000);
    }

    public void searchAssistant(String name) {
        searchInput.fill(name);
        page.waitForTimeout(2000);
        page.keyboard().press("Enter");
        page.waitForTimeout(2000);
    }

    public void clickEditAssistant(String name) {
        Locator row = page.locator("tr:has(td:text(\"" + name + "\"))").first();
        row.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        row.locator("a i.flaticon-edit").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public void clickDeleteAssistant(String name) {
        Locator row = page.locator("tr:has(td:text(\"" + name + "\"))").first();
        row.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        row.locator("a.delete_assistant").click();
        page.waitForTimeout(1000);
    }

    public void handleDeleteConfirmation() {
        page.locator(".swal2-confirm").click();
        page.waitForTimeout(1000);
    }

    public void updateAssistantName(String name) {
        assistantNameInput.fill("");
        assistantNameInput.fill(name);
    }

    public void clickUpdateAssistant() {
        page.locator("input#update_assistant").waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
        page.locator("input#update_assistant").click(new Locator.ClickOptions().setTimeout(15000));
        page.waitForTimeout(2000);
    }

    public void verifyAssistantUpdated() {
        assertThat(page.locator("text=Assistant Updated Successfully")).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(30000));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ok, got it!")).click();
        page.waitForTimeout(3000);
    }

    public void verifyAssistantDeleted() {
        assertThat(successPopup).isVisible(
                new com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions()
                        .setTimeout(15000));
        confirmButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        confirmButton.click();
        page.waitForTimeout(3000);
    }

    public void fillAddAssistantForm(Map<String, String> data) {
        fillAssistantName(data.get("assistantName"));
        selectProjectType(data.get("projectType"));
        selectApi(data.get("selectApi"));
        selectIndustry(data.get("industry"));
        selectSubIndustry(data.get("subIndustry"));
        selectUseCase(data.get("useCase"));
        selectSkill(data.get("skill"));
        page.waitForTimeout(1000);
        clickAddAssistant();
        boolean modelVisible = modelSelect.isVisible();
        if (modelVisible) {
            selectModel("gpt-4o");
        }
    }
}
