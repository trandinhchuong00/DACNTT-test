package com.oam.utils.log4j;

import cucumber.api.*;
import cucumber.api.event.*;
import cucumber.api.formatter.ColorAware;
import cucumber.api.formatter.NiceAppendable;
import cucumber.runtime.formatter.Format;
import cucumber.runtime.formatter.Formats;
import cucumber.util.FixJava;
import cucumber.util.Mapper;
import gherkin.ast.*;
import gherkin.pickles.PickleTag;

import java.util.List;

public abstract class AbstractPrettyFormatter implements EventListener, ColorAware {

    private static final String SCENARIO_INDENT = "  ";
    private static final String STEP_INDENT = "    ";
    private static final String EXAMPLES_INDENT = "    ";
    private static final String ERROR_INDENT = "      ";
    private final TestSourcesModel testSources = new TestSourcesModel();
    private Formats formats;
    private NiceAppendable out;
    private String currentFeatureFile;
    private TestCase currentTestCase;
    private ScenarioOutline currentScenarioOutline;
    private Examples currentExamples;
    private int locationIndentation;
    private Mapper<Tag, String> tagNameMapper = new Mapper<Tag, String>() {
        @Override
        public String map(Tag tag) {
            return tag.getName();
        }
    };
    private Mapper<PickleTag, String> pickleTagNameMapper = new Mapper<PickleTag, String>() {
        @Override
        public String map(PickleTag pickleTag) {
            return pickleTag.getName();
        }
    };

    private EventHandler<TestSourceRead> testSourceReadHandler = new EventHandler<TestSourceRead>() {
        @Override
        public void receive(TestSourceRead event) {
            handleTestSourceRead(event);
        }
    };
    private EventHandler<TestCaseStarted> caseStartedHandler = new EventHandler<TestCaseStarted>() {
        @Override
        public void receive(TestCaseStarted event) {
            handleTestCaseStarted(event);
        }
    };
    private EventHandler<TestStepStarted> stepStartedHandler = new EventHandler<TestStepStarted>() {
        @Override
        public void receive(TestStepStarted event) {
            handleTestStepStarted(event);
        }
    };
    private EventHandler<TestStepFinished> stepFinishedHandler = new EventHandler<TestStepFinished>() {
        @Override
        public void receive(TestStepFinished event) {
            handleTestStepFinished(event);
        }
    };
    private EventHandler<WriteEvent> writeEventHandler = new EventHandler<WriteEvent>() {
        @Override
        public void receive(WriteEvent event) {
            handleWrite(event);
        }
    };
    private EventHandler<TestRunFinished> runFinishedHandler = new EventHandler<TestRunFinished>() {
        @Override
        public void receive(TestRunFinished event) {
            finishReport();
        }
    };

    @SuppressWarnings("WeakerAccess") // Used by PluginFactory
    public AbstractPrettyFormatter(Appendable out) {
        this.out = new NiceAppendable(out);
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestSourceRead.class, testSourceReadHandler);
        publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
        publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
        publisher.registerHandlerFor(WriteEvent.class, writeEventHandler);
        publisher.registerHandlerFor(TestRunFinished.class, runFinishedHandler);
    }

    @Override
    public void setMonochrome(boolean monochrome) {
        if (monochrome) {
            formats = new MonochromeFormats();
        } else {
            formats = new AnsiFormats();
        }
    }

    abstract protected void printlnLog(String logEntry);

    private void printlnLog() {
        printlnLog("");
    }

    NiceAppendable out() {
        return out;
    }

    private void handleTestSourceRead(TestSourceRead event) {
        testSources.addTestSourceReadEvent(event.uri, event);
    }

    private void handleTestCaseStarted(TestCaseStarted event) {
        handleStartOfFeature(event);
        handleScenarioOutline(event);
        if (testSources.hasBackground(currentFeatureFile, event.testCase.getLine())) {
            printBackground(event.testCase);
            currentTestCase = event.testCase;
        } else {
            printScenarioDefinition(event.testCase);
        }
    }

    private void handleTestStepStarted(TestStepStarted event) {
        if (event.testStep instanceof PickleStepTestStep) {
            if (isFirstStepAfterBackground((PickleStepTestStep) event.testStep)) {
                printScenarioDefinition(currentTestCase);
                currentTestCase = null;
            }
        }
    }

    private void handleTestStepFinished(TestStepFinished event) {
        if (event.testStep instanceof PickleStepTestStep) {
            printStep((PickleStepTestStep) event.testStep, event.result);
        }
        printError(event.result);
    }

    private void handleWrite(WriteEvent event) {
        printlnLog(event.text);
    }

    abstract void finishReport();

    private void handleStartOfFeature(TestCaseStarted event) {
        if (currentFeatureFile == null || !currentFeatureFile.equals(event.testCase.getUri())) {
            if (currentFeatureFile != null) {
                printlnLog();
            }
            currentFeatureFile = event.testCase.getUri();
            printFeature(currentFeatureFile);
        }
    }

    private void handleScenarioOutline(TestCaseStarted event) {
        TestSourcesModel.AstNode astNode = testSources.getAstNode(currentFeatureFile, event.testCase.getLine());
        if (TestSourcesModel.isScenarioOutlineScenario(astNode)) {
            ScenarioOutline scenarioOutline = (ScenarioOutline) TestSourcesModel.getScenarioDefinition(astNode);
            if (currentScenarioOutline == null || !currentScenarioOutline.equals(scenarioOutline)) {
                currentScenarioOutline = scenarioOutline;
                printScenarioOutline(currentScenarioOutline);
            }
            if (currentExamples == null || !currentExamples.equals(astNode.parent.node)) {
                currentExamples = (Examples) astNode.parent.node;
                printExamples(currentExamples);
            }
        } else {
            currentScenarioOutline = null;
            currentExamples = null;
        }
    }

    private void printScenarioOutline(ScenarioOutline scenarioOutline) {
        printlnLog();
        printTags(scenarioOutline.getTags(), SCENARIO_INDENT);
        printlnLog(
                SCENARIO_INDENT + getScenarioDefinitionText(scenarioOutline) + " " + getLocationText(currentFeatureFile,
                        scenarioOutline.getLocation().getLine()));
        printDescription(scenarioOutline.getDescription());
        for (Step step : scenarioOutline.getSteps()) {
            printlnLog(STEP_INDENT + formats.get("skipped").text(step.getKeyword() + step.getText()));
        }
    }

    private void printExamples(Examples examples) {
        printlnLog();
        printTags(examples.getTags(), EXAMPLES_INDENT);
        printlnLog(EXAMPLES_INDENT + examples.getKeyword() + ": " + examples.getName());
        printDescription(examples.getDescription());
    }

    private void printStep(PickleStepTestStep testStep, Result result) {
        String keyword = getStepKeyword(testStep);
        String stepText = testStep.getStepText();
        String locationPadding = createPaddingToLocation(STEP_INDENT, keyword + stepText);
        String formattedStepText = formatStepText(keyword, stepText, formats.get(result.getStatus().lowerCaseName()),
                formats.get(result.getStatus().lowerCaseName() + "_arg"), testStep.getDefinitionArgument());
        printlnLog(STEP_INDENT + formattedStepText + locationPadding + getLocationText(testStep.getCodeLocation()));
    }

    String formatStepText(String keyword, String stepText, Format textFormat, Format argFormat,
                          List<Argument> arguments) {
        int beginIndex = 0;
        StringBuilder result = new StringBuilder(textFormat.text(keyword));
        for (Argument argument : arguments) {
            // can be null if the argument is missing.
            if (argument.getValue() != null) {
                int argumentOffset = argument.getStart();
                // a nested argument starts before the enclosing argument ends; ignore it when formatting
                if (argumentOffset < beginIndex) {
                    continue;
                }
                String text = stepText.substring(beginIndex, argumentOffset);
                result.append(textFormat.text(text));
            }
            // val can be null if the argument isn't there, for example @And("(it )?has something")
            if (argument.getValue() != null) {
                String text = stepText.substring(argument.getStart(), argument.getEnd());
                result.append(argFormat.text(text));
                // set beginIndex to end of argument
                beginIndex = argument.getEnd();
            }
        }
        if (beginIndex != stepText.length()) {
            String text = stepText.substring(beginIndex, stepText.length());
            result.append(textFormat.text(text));
        }
        return result.toString();
    }

    private String getScenarioDefinitionText(ScenarioDefinition definition) {
        return definition.getKeyword() + ": " + definition.getName();
    }

    private String getLocationText(String file, int line) {
        return getLocationText(file + ":" + line);
    }

    private String getLocationText(String location) {
        return formats.get("comment").text("# " + location);
    }

    private StringBuffer stepText(PickleStepTestStep testStep) {
        String keyword = getStepKeyword(testStep);
        return new StringBuffer(keyword + testStep.getStepText());
    }

    private String getStepKeyword(PickleStepTestStep testStep) {
        TestSourcesModel.AstNode astNode = testSources.getAstNode(currentFeatureFile, testStep.getStepLine());
        if (astNode != null) {
            Step step = (Step) astNode.node;
            return step.getKeyword();
        } else {
            return "";
        }
    }

    private boolean isFirstStepAfterBackground(PickleStepTestStep testStep) {
        return currentTestCase != null && !isBackgroundStep(testStep);
    }

    private boolean isBackgroundStep(PickleStepTestStep testStep) {
        TestSourcesModel.AstNode astNode = testSources.getAstNode(currentFeatureFile, testStep.getStepLine());
        if (astNode != null) {
            return TestSourcesModel.isBackgroundStep(astNode);
        }
        return false;
    }

    private void printFeature(String path) {
        Feature feature = testSources.getFeature(path);
        printTags(feature.getTags());
        printlnLog(feature.getKeyword() + ": " + feature.getName());
        printDescription(feature.getDescription());
    }

    private void printTags(List<Tag> tags) {
        printTags(tags, "");
    }

    private void printTags(List<Tag> tags, String indent) {
        if (!tags.isEmpty()) {
            printlnLog(indent + FixJava.join(FixJava.map(tags, tagNameMapper), " "));
        }
    }

    private void printPickleTags(List<PickleTag> tags, String indent) {
        if (!tags.isEmpty()) {
            printlnLog(indent + FixJava.join(FixJava.map(tags, pickleTagNameMapper), " "));
        }
    }

    private void printDescription(String description) {
        if (description != null) {
            printlnLog(description);
        }
    }

    private void printBackground(TestCase testCase) {
        TestSourcesModel.AstNode astNode = testSources.getAstNode(currentFeatureFile, testCase.getLine());
        if (astNode != null) {
            Background background = TestSourcesModel.getBackgroundForTestCase(astNode);
            String backgroundText = getScenarioDefinitionText(background);
            boolean useBackgroundSteps = true;
            calculateLocationIndentation(SCENARIO_INDENT + backgroundText, testCase.getTestSteps(), useBackgroundSteps);
            String locationPadding = createPaddingToLocation(SCENARIO_INDENT, backgroundText);
            printlnLog();
            printlnLog(SCENARIO_INDENT + backgroundText + locationPadding + getLocationText(currentFeatureFile,
                    background.getLocation().getLine()));
            printDescription(background.getDescription());
        }
    }

    private void printScenarioDefinition(TestCase testCase) {
        ScenarioDefinition scenarioDefinition = testSources
                .getScenarioDefinition(currentFeatureFile, testCase.getLine());
        String definitionText = scenarioDefinition.getKeyword() + ": " + testCase.getName();
        calculateLocationIndentation(SCENARIO_INDENT + definitionText, testCase.getTestSteps());
        String locationPadding = createPaddingToLocation(SCENARIO_INDENT, definitionText);
        printlnLog();
        printPickleTags(testCase.getTags(), SCENARIO_INDENT);
        printlnLog(SCENARIO_INDENT + definitionText + locationPadding + getLocationText(currentFeatureFile,
                testCase.getLine()));
        printDescription(scenarioDefinition.getDescription());
    }

    private void printError(Result result) {
        if (result.getError() != null) {
            printlnLog(ERROR_INDENT + formats.get(result.getStatus().lowerCaseName()).text(result.getErrorMessage()));
        }
    }

    private void calculateLocationIndentation(String definitionText, List<TestStep> testSteps) {
        boolean useBackgroundSteps = false;
        calculateLocationIndentation(definitionText, testSteps, useBackgroundSteps);
    }

    private void calculateLocationIndentation(String definitionText, List<TestStep> testSteps,
                                              boolean useBackgroundSteps) {
        int maxTextLength = definitionText.length();
        for (TestStep step : testSteps) {
            if (step instanceof PickleStepTestStep) {
                PickleStepTestStep testStep = (PickleStepTestStep) step;
                if (isBackgroundStep(testStep) == useBackgroundSteps) {
                    StringBuffer stepText = stepText(testStep);
                    maxTextLength = Math.max(maxTextLength, STEP_INDENT.length() + stepText.length());
                }
            }
        }
        locationIndentation = maxTextLength + 1;
    }

    private String createPaddingToLocation(String indent, String text) {
        StringBuffer padding = new StringBuffer();
        for (int i = indent.length() + text.length(); i < locationIndentation; ++i) {
            padding.append(' ');
        }
        return padding.toString();
    }
}
