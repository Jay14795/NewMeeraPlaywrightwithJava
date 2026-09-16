package com.meera.pages;

import com.microsoft.playwright.Page;

/**
 * Small scrolling helper page object. Port of {@code pages/FollowupPage.js}.
 */
public class FollowupPage {

    private final Page page;

    public FollowupPage(Page page) {
        this.page = page;
    }

    public void scrollUpPage() {
        page.evaluate("() => { window.scrollTo(0, 0); }");
        page.waitForTimeout(500);
    }

    public void scrollDownPage() {
        page.evaluate("() => { window.scrollTo(0, document.body.scrollHeight); }");
        page.waitForTimeout(500);
    }

    public void scrollToElement(String selector) {
        page.locator(selector).scrollIntoViewIfNeeded();
        page.waitForTimeout(500);
    }
}
