package com.meera.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Grounded RAG configuration tabs. Port of {@code pages/GroundedRagConfigPage.js}.
 */
public class GroundedRagConfigPage {

    private final Page page;
    private final Locator groundedRagConfigTab;
    private final Locator createKnowledgeBaseSubTab;
    private final Locator testAndTrainSubTab;

    public GroundedRagConfigPage(Page page) {
        this.page = page;
        this.groundedRagConfigTab = page.locator("a[href=\"#groundedTab\"]");
        this.createKnowledgeBaseSubTab = page.locator("a[href=\"#kt_tab_pane_5_1\"]");
        this.testAndTrainSubTab = page.locator("a[href=\"#kt_tab_pane_5_5\"]");
    }

    public void clickGroundedRagConfigTab() {
        groundedRagConfigTab.click();
        page.waitForTimeout(1500);
    }

    public void clickCreateKnowledgeBaseTab() {
        createKnowledgeBaseSubTab.click();
        page.waitForTimeout(1000);
    }

    public void clickTestAndTrainTab() {
        testAndTrainSubTab.click();
        page.waitForTimeout(1000);
    }
}
