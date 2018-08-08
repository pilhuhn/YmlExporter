/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.bsd.mp.exporter.yml;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * @author hrupp
 */
public class ScheduledYamlExporter extends YamlExporter implements Runnable {

  public ScheduledYamlExporter() {
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    executor.scheduleAtFixedRate(this, 5, 30, TimeUnit.SECONDS);
  }

  @Override
  public HttpMethod getMethod() {
    return super.getMethod();    // We should introduce "NONE" and then have those not show up on /metrics
  }

  @Override
  public String getMediaType() {
    return " not for http usage ";
  }

  @Override
  public void run() {
    String output = exportOneMetric(MetricRegistry.Type.BASE,"memory.usedHeap");
    System.err.println(output);
  }
}
