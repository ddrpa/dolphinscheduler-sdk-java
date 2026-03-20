package com.github.weaksloth.dolphins.process;

import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TaskDefinitionLog extends TaskDefinition {

  /** operator user id */
  private int operator;

  /** operate time */
  private Date operateTime;
}
