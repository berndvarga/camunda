package com.test.dmn;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.MapEntry;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.engine.dmn.DecisionsEvaluationBuilder;
import org.camunda.bpm.engine.repository.DecisionDefinition;
import org.camunda.bpm.engine.repository.DecisionDefinitionQuery;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DecisionServiceTest {
  @Rule
  public ProcessEngineRule rule = new ProcessEngineRule();

  @Before
  public void setup() {
    deployTenantMocks();
  }

  @Test
  public void evaluateDecisionWithoutTenant() {
    VariableMap items = Variables.createVariables();
    items.putValue("status", "silver");
    items.putValue("sum", 100.0);
    VariableMap variables = Variables.createVariables().putValue("TestObject", items);

    DmnDecisionResultEntries result =
        this.evaluateDecision(true, "orderDecision", null, variables);

    Assertions.assertThat(result)
        .containsOnly(
            MapEntry.entry("response.result", "ok"),
            MapEntry.entry("reason", "you little fish will get what you want")
        );


    result =
        this.evaluateDecision(false, "orderDecision", null, variables);

    Assertions.assertThat(result)
        .containsOnly(
            MapEntry.entry("response.result", "ok"),
            MapEntry.entry("reason", "you little fish will get what you want")
        );
  }

  @Test
  public void evaluateDecisionWithTenant() {
    VariableMap items = Variables.createVariables();
    items.putValue("status", "silver");
    items.putValue("sum", 100.0);
    VariableMap variables = Variables.createVariables().putValue("TestObject", items);

    DmnDecisionResultEntries result =
        this.evaluateDecision(true, "orderDecision", "fintec1", variables);

    Assertions.assertThat(result)
        .containsOnly(
            MapEntry.entry("response.result", "ok"),
            MapEntry.entry("reason", "FINTEC1 - you little fish will get what you want")
        );

    result =
        this.evaluateDecision(false, "orderDecision", "fintec1", variables);

    Assertions.assertThat(result)
        .containsOnly(
            MapEntry.entry("response.result", "ok"),
            MapEntry.entry("reason", "FINTEC1 - you little fish will get what you want")
        );
  }

  @Test
  public void evaluateDecisionWithUndefinedTenant() {
    VariableMap items = Variables.createVariables();
    items.putValue("status", "silver");
    items.putValue("sum", 100.0);
    VariableMap variables = Variables.createVariables().putValue("TestObject", items);

    DmnDecisionResultEntries result =
        this.evaluateDecision(true, "orderDecision", "undefined", variables);

    Assertions.assertThat(result)
        .containsOnly(
            MapEntry.entry("response.result", "ok"),
            MapEntry.entry("reason", "you little fish will get what you want")
        );

    result =
        this.evaluateDecision(false, "orderDecision", "undefined", variables);

    Assertions.assertThat(result)
        .containsOnly(
            MapEntry.entry("response.result", "ok"),
            MapEntry.entry("reason", "you little fish will get what you want")
        );
  }

  private DmnDecisionResultEntries evaluateDecision(boolean workaround, String decisionDefinitionKey, String tenant, VariableMap variables) {

    if (workaround) {
      DecisionDefinitionQuery decisionDefinitionQuery =
          rule.getRepositoryService().createDecisionDefinitionQuery().decisionDefinitionKey(decisionDefinitionKey)
              .latestVersion();
      if (tenant != null && !tenant.isEmpty()) {
        decisionDefinitionQuery.tenantIdIn(tenant);
        decisionDefinitionQuery.includeDecisionDefinitionsWithoutTenantId();
      }

      List<DecisionDefinition> decisionDefinitions = decisionDefinitionQuery.list();
      AtomicReference<String> tenantId = new AtomicReference<>();
      if (decisionDefinitions.size() == 2) {
        decisionDefinitions.forEach(d -> {
          if (d.getTenantId() != null) {
            tenantId.set(d.getTenantId());
          }
        });
      }

      DecisionsEvaluationBuilder decisionsEvaluationBuilder = rule.getDecisionService()
          .evaluateDecisionByKey(decisionDefinitionKey);

      if (tenantId.get() != null) {
        decisionsEvaluationBuilder.decisionDefinitionTenantId(tenantId.get());
      } else {
        decisionsEvaluationBuilder.decisionDefinitionWithoutTenantId();
      }
      DmnDecisionResult result = decisionsEvaluationBuilder.variables(variables).evaluate();
      DmnDecisionResultEntries singleResult = result.getSingleResult();

      return singleResult;
    } else {
      DecisionsEvaluationBuilder decisionsEvaluationBuilder = rule.getDecisionService()
          .evaluateDecisionByKey(decisionDefinitionKey);

      if (tenant == null || tenant.isEmpty()) {
        decisionsEvaluationBuilder.decisionDefinitionWithoutTenantId();
      } else {
        decisionsEvaluationBuilder.decisionDefinitionTenantId(tenant);
      }

      DmnDecisionResult result = decisionsEvaluationBuilder.variables(variables).evaluate();
      DmnDecisionResultEntries singleResult = result.getSingleResult();

      return singleResult;
    }
  }


  private void deployTenantMocks() {
    this.rule.getRepositoryService()
        .createDeployment()
        .addClasspathResource("Example.dmn")
        .deploy();
    this.rule.getRepositoryService()
        .createDeployment()
        .tenantId("fintec1")
        .addClasspathResource("fintec1/Example.dmn")
        .deploy();
    this.rule.getRepositoryService()
        .createDeployment()
        .tenantId("fintec2")
        .addClasspathResource("fintec2/Example.dmn")
        .deploy();
  }
}
