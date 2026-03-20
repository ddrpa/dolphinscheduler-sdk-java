package com.github.weaksloth.dolphins.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.weaksloth.dolphins.core.AbstractOperator;
import com.github.weaksloth.dolphins.core.DolphinException;
import com.github.weaksloth.dolphins.process.TaskDefinition;
import com.github.weaksloth.dolphins.process.TaskDefinitionLog;
import com.github.weaksloth.dolphins.remote.DolphinsRestTemplate;
import com.github.weaksloth.dolphins.remote.HttpRestResult;
import com.github.weaksloth.dolphins.remote.Query;
import com.github.weaksloth.dolphins.util.JacksonUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskOperator extends AbstractOperator {

  public TaskOperator(
      String dolphinAddress, String token, DolphinsRestTemplate dolphinsRestTemplate) {
    super(dolphinAddress, token, dolphinsRestTemplate);
  }

  /**
   * query task definition by task code
   *
   * @param projectCode project code
   * @param taskCode task definition code
   * @return task definition
   */
  public TaskDefinition queryTaskDefinition(Long projectCode, Long taskCode) {
    String url = dolphinAddress + "/projects/" + projectCode + "/task-definition/" + taskCode;
    try {
      HttpRestResult<JsonNode> restResult =
          this.dolphinsRestTemplate.get(url, this.getHeader(), null, JsonNode.class);
      if (restResult.getSuccess()) {
        JsonNode data = restResult.getData();
        String taskType = data.get("taskType").asText();
        JsonNode taskParamsNode = data.get("taskParams");

        AbstractTask taskParams = null;
        if (taskParamsNode != null) {
          Class<? extends AbstractTask> taskClass = matchTaskClass(taskType);
          taskParams = JacksonUtils.parseObject(taskParamsNode.toString(), taskClass);
        }
        if (data.isObject()) {
          ((ObjectNode) data).remove("taskParams");
        }
        TaskDefinition taskDefinition =
            JacksonUtils.parseObject(data.toString(), TaskDefinition.class);
        if (taskParams != null) {
          taskDefinition.setTaskParams(taskParams);
        }
        return taskDefinition;
      }
      log.error("query task definition response:{}", restResult);
      throw new DolphinException("query dolphin scheduler task definition fail");
    } catch (Exception e) {
      throw new DolphinException("query dolphin scheduler task definition fail", e);
    }
  }

  /**
   * update task definition
   *
   * <p>注意需要先下线 processDefinition 才可以修改，且下线后需要等待服务端执行片刻
   *
   * @param projectCode project code
   * @param taskCode task definition code
   * @param taskDefinition update payload
   * @return taskCode, modify task definition won't change it
   */
  public Long updateTaskDefinition(
      Long projectCode, Long taskCode, TaskDefinitionLog taskDefinition) {
    String url = dolphinAddress + "/projects/" + projectCode + "/task-definition/" + taskCode;
    log.info("update task definition,url:{}, param:{}", url, taskDefinition);
    try {
      if (taskDefinition == null) {
        throw new DolphinException("task definition update param can not be null");
      }

      Query query =
          new Query().addParam("taskDefinitionJsonObj", JacksonUtils.toJSONString(taskDefinition));
      HttpRestResult<Long> restResult =
          dolphinsRestTemplate.putForm(url, getHeader(), query, null, Long.class);
      if (restResult.getSuccess()) {
        return restResult.getData();
      } else {
        log.error("dolphin scheduler response:{}", restResult);
        throw new DolphinException("update dolphin scheduler workflow fail");
      }
    } catch (Exception e) {
      throw new DolphinException("update dolphin scheduler task definition fail", e);
    }
  }

  private static Class<? extends AbstractTask> matchTaskClass(String taskType) {
    switch (taskType) {
      case "SHELL":
        return ShellTask.class;
      case "SQL":
        return SqlTask.class;
      case "PROCEDURE":
        return ProcedureTask.class;
      case "HTTP":
        return HttpTask.class;
      case "PYTHON":
        return PythonTask.class;
      case "DATAX":
        return DataxTask.class;
      case "CONDITIONS":
        return ConditionTask.class;
      case "SPARK":
        return SparkTask.class;
      default:
        return GenericTask.class;
    }
  }
}
