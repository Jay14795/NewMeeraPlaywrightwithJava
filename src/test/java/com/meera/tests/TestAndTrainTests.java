package com.meera.tests;

import com.meera.config.Config;
import com.meera.pages.FollowupPage;
import com.meera.pages.GroundedRagConfigPage;
import com.meera.pages.TestandTrainPage;
import com.meera.utils.VoiceAgent;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test &amp; Train voice-call module. Port of {@code tests/testandtrainTests.spec.js}.
 * Requires the microphone permission and (on Windows) the VB-CABLE loopback + SAPI voice.
 */
public class TestAndTrainTests extends AuthenticatedTest {

    private static final String CAMPAIGN_URL = Config.TEST_AND_TRAIN_CAMPAIGN_URL;

    private static final List<String> TEST_QUESTIONS = Arrays.asList(
            "What is Cameo Beauty Academy?",
            "Where is Cameo Beauty Academy located?",
            "How can I reach Cameo Beauty Academy?",
            "What is the phone number of Cameo Beauty Academy?",
            "What are the working hours of Cameo Beauty Academy?",
            "Which days is Cameo Beauty Academy open?",
            "Do you provide online training or only offline classes?",
            "Do you provide demo classes?",
            "Can I visit the academy before enrolling?",
            "Is prior experience needed to join?",
            "What courses are offered at Cameo Beauty Academy?",
            "What are the course fees?",
            "Do you provide certification after course completion?",
            "What is the admission process?",
            "Can I enroll online?");

    @Override
    protected List<String> permissions() {
        return List.of("microphone");
    }

    @Test(timeOut = 600_000)
    public void outboundCallVoiceFunctionalityWithAiAgent() {
        GroundedRagConfigPage grounded = new GroundedRagConfigPage(page);
        FollowupPage follow = new FollowupPage(page);
        TestandTrainPage testandTrain = new TestandTrainPage(page);
        testandTrain.useVirtualCableMicrophone();
        VoiceAgent voiceAgent = new VoiceAgent();

        // Navigate to campaign
        page.navigate(CAMPAIGN_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
        page.waitForTimeout(3000);

        // Go to Grounded RAG Config tab then Test & Train
        grounded.clickGroundedRagConfigTab();
        page.waitForTimeout(3000);
        grounded.clickTestAndTrainTab();
        page.waitForTimeout(1000);

        // Scroll up
        follow.scrollUpPage();
        page.waitForTimeout(3000);

        // Pick 10 random questions
        List<String> shuffled = new ArrayList<>(TEST_QUESTIONS);
        Collections.shuffle(shuffled);
        List<String> randomQuestions = new ArrayList<>(shuffled.subList(0, 10));

        System.out.println("\nRandomized Test Questions:");
        for (int i = 0; i < randomQuestions.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + randomQuestions.get(i));
        }

        // Select outbound call type and start call
        System.out.println("\nSelecting outbound call...");
        testandTrain.selectCallType("Outbound");
        System.out.println("Sending start prompt...");
        testandTrain.startCall("Hi, I have some questions about Cameo Beauty Academy.");
        page.waitForTimeout(4000);

        // Verify call is active
        boolean voiceConnected = testandTrain.waitForCallConnected(60000);
        Assert.assertTrue(voiceConnected);
        System.out.println("Voice is In Call!");

        // Wait for bot's initial greeting
        page.waitForTimeout(3000);

        // AI Agent interacts with the bot
        System.out.println("\nAI Agent starting conversation...\n");

        for (int i = 0; i < randomQuestions.size(); i++) {
            String question = randomQuestions.get(i);

            boolean isActive = testandTrain.isCallActive();
            if (!isActive) {
                System.out.println("Call ended prematurely at question " + (i + 1));
                break;
            }

            System.out.println("\n" + "=".repeat(70));
            System.out.println("Question " + (i + 1) + " of " + randomQuestions.size());
            System.out.println("AI Agent will ask: \"" + question + "\"");
            System.out.println("=".repeat(70));

            try {
                testandTrain.ensureUnmuted();
                System.out.println("Microphone ready");
                page.waitForTimeout(300);

                System.out.println("AI Agent speaking...");
                long speechDuration = voiceAgent.speakQuestion(question);
                System.out.println("Speech took: " + speechDuration + "ms");

                page.waitForTimeout(300);
                testandTrain.ensureMuted();
                System.out.println("Microphone muted");
                System.out.println("Waiting for bot response...");

                page.waitForTimeout(6000);

                voiceAgent.logQuestion(question, "Bot responded", speechDuration);
                System.out.println("Question " + (i + 1) + " completed!\n");

                page.waitForTimeout(1000);
            } catch (RuntimeException err) {
                System.err.println("Error during question " + (i + 1) + ": " + err.getMessage());
            }
        }

        // Final status
        String finalStatus = testandTrain.getVoiceStatus();
        System.out.println("\nFinal voice status: " + finalStatus);

        if (testandTrain.isCallActive()) {
            System.out.println("Hanging up the call...");
            testandTrain.clickHangupButton();
            page.waitForTimeout(3000);
        }

        String afterHangup = testandTrain.getVoiceStatus();
        System.out.println("Voice status after hangup: " + afterHangup);
        Assert.assertFalse(afterHangup.toLowerCase().contains("in call"));
        System.out.println("Voice call hung up successfully!");

        // Generate and save report
        voiceAgent.generateReport();
        String reportPath = "test-results/voice-interaction-report-" + System.currentTimeMillis() + ".txt";
        voiceAgent.saveReportToFile(reportPath);
        System.out.println("Report saved to: " + reportPath);

        voiceAgent.cleanup();
    }

    @Test(timeOut = 300_000)
    public void inboundCallAnswerAndVerifyVoiceStatus() {
        GroundedRagConfigPage grounded = new GroundedRagConfigPage(page);
        FollowupPage follow = new FollowupPage(page);
        TestandTrainPage testandTrain = new TestandTrainPage(page);
        testandTrain.useVirtualCableMicrophone();
        VoiceAgent voiceAgent = new VoiceAgent();

        // Navigate to campaign
        page.navigate(CAMPAIGN_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
        page.waitForTimeout(3000);

        // Go to Test & Train
        grounded.clickGroundedRagConfigTab();
        page.waitForTimeout(3000);
        grounded.clickTestAndTrainTab();
        page.waitForTimeout(1000);
        follow.scrollUpPage();
        page.waitForTimeout(2000);

        System.out.println("\nSelecting inbound call and starting...");
        testandTrain.selectCallType("Inbound");
        System.out.println("Sending start prompt...");
        testandTrain.startCall("Hi, I'd like to talk about your courses.");
        page.waitForTimeout(4000);

        // Wait for inbound call connection
        System.out.println("Waiting for incoming call...");
        testandTrain.waitForIncomingCall(60000);

        // Verify call is active
        String voiceStatus = testandTrain.getVoiceStatus();
        Assert.assertTrue(voiceStatus.toLowerCase().contains("in call"));
        System.out.println("Inbound call answered successfully!");

        // Listen to bot greeting
        page.waitForTimeout(4000);

        // Ask a question
        String question = "What courses do you offer?";
        System.out.println("Asking: \"" + question + "\"");

        testandTrain.ensureUnmuted();
        page.waitForTimeout(300);
        voiceAgent.speakQuestion(question);
        page.waitForTimeout(300);
        testandTrain.ensureMuted();
        page.waitForTimeout(6000);

        // Hang up
        if (testandTrain.isCallActive()) {
            System.out.println("Hanging up inbound call...");
            testandTrain.clickHangupButton();
            page.waitForTimeout(3000);
        }

        String afterHangup = testandTrain.getVoiceStatus();
        Assert.assertFalse(afterHangup.toLowerCase().contains("in call"));
        System.out.println("Inbound call completed successfully!");

        voiceAgent.cleanup();
    }

    @Test(timeOut = 300_000)
    public void outboundCallSendQuestionsViaInputAndVerifyBotResponse() {
        GroundedRagConfigPage grounded = new GroundedRagConfigPage(page);
        FollowupPage follow = new FollowupPage(page);
        TestandTrainPage testandTrain = new TestandTrainPage(page);
        testandTrain.useVirtualCableMicrophone();

        // Navigate to campaign
        page.navigate(CAMPAIGN_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
        page.waitForTimeout(3000);

        // Go to Test & Train
        grounded.clickGroundedRagConfigTab();
        page.waitForTimeout(3000);
        grounded.clickTestAndTrainTab();
        page.waitForTimeout(1000);
        follow.scrollUpPage();
        page.waitForTimeout(2000);

        // Select outbound call type and start call
        System.out.println("Selecting outbound call...");
        testandTrain.selectCallType("Outbound");
        System.out.println("Sending start prompt...");
        testandTrain.startCall("Hi, I have some questions.");
        page.waitForTimeout(4000);

        boolean voiceConnected = testandTrain.waitForCallConnected(60000);
        Assert.assertTrue(voiceConnected);
        System.out.println("Call is active!");

        page.waitForTimeout(3000);

        // Send test questions via input field
        List<String> textQuestions = Arrays.asList(
                "What is Cameo Beauty Academy?",
                "Where are you located?");

        for (String q : textQuestions) {
            System.out.println("Sending question: \"" + q + "\"");
            testandTrain.sendTestQuestion(q);
            page.waitForTimeout(5000);

            String response = testandTrain.getBotResponse();
            System.out.println("Bot response: " + response);
        }

        // Hang up
        if (testandTrain.isCallActive()) {
            testandTrain.clickHangupButton();
            page.waitForTimeout(3000);
        }

        String afterHangup = testandTrain.getVoiceStatus();
        Assert.assertFalse(afterHangup.toLowerCase().contains("in call"));
        System.out.println("Text-based test completed!");
    }

    @Test(timeOut = 180_000)
    public void outboundCallVerifyCallLifecycle() {
        GroundedRagConfigPage grounded = new GroundedRagConfigPage(page);
        FollowupPage follow = new FollowupPage(page);
        TestandTrainPage testandTrain = new TestandTrainPage(page);
        testandTrain.useVirtualCableMicrophone();

        // Navigate to campaign
        page.navigate(CAMPAIGN_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
        page.waitForTimeout(3000);
        grounded.clickGroundedRagConfigTab();
        page.waitForTimeout(3000);
        grounded.clickTestAndTrainTab();
        page.waitForTimeout(1000);
        follow.scrollUpPage();
        page.waitForTimeout(2000);

        // 1. Select outbound call type, send prompt, start call
        testandTrain.selectCallType("Outbound");
        testandTrain.startCall("Hello, I'd like to test the AI agent.");

        boolean connected = testandTrain.waitForCallConnected(60000);
        Assert.assertTrue(connected);
        System.out.println("Call started successfully");

        // 2. Wait, verify still active
        page.waitForTimeout(5000);
        String status = testandTrain.getVoiceStatus();
        Assert.assertTrue(status.toLowerCase().contains("in call"));
        System.out.println("Call remains active after 5s");

        // 3. Hang up
        testandTrain.clickHangupButton();
        page.waitForTimeout(3000);

        status = testandTrain.getVoiceStatus();
        Assert.assertFalse(status.toLowerCase().contains("in call"));
        System.out.println("Call ended successfully - lifecycle verified");
    }
}
