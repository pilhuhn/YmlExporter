package de.bsd.mp.exporter.yml;

import static org.eclipse.microprofile.metrics.spi.MetricExporter.HttpMethod.GET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotSupportedException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.spi.MetricExporter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author hrupp
 */
@ApplicationScoped
public class YamlExporter implements MetricExporter {

  private Yaml yaml;
  private Map<MetricRegistry.Type, MetricRegistry> registryMap;

  public YamlExporter() {

    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true); // Does that work for BLOCK?
    options.setIndent(2);
    yaml = new Yaml(options);

  }

  @Override
  public void setRegistries(Map<MetricRegistry.Type, MetricRegistry> registryMap) {
    this.registryMap = registryMap;
  }

  public int getPriority() {
    return 5;
  }

  public String getMediaType() {
    return "text/yml";
  }

  public HttpMethod getMethod() {
    return GET;
  }

  public String exportOneScope(MetricRegistry.Type scope) {
    Map<String, List<Map>> map = getDataForScope(scope);
    return yaml.dump(map);
  }


  public String exportAllScopes() {
    List<Map<String,List<Map>>> list = new ArrayList<>(MetricRegistry.Type.values().length);
    for (MetricRegistry.Type type : MetricRegistry.Type.values()) {
      list.add(getDataForScope(type));
    }
    return yaml.dump(list);
  }

  public String exportOneMetric(MetricRegistry.Type scope, String metricName) {

    Map mwu = getSingleMetric(scope, metricName);

    return yaml.dump(mwu);
  }

  private Map<String, List<Map>> getDataForScope(MetricRegistry.Type scope) {
    MetricRegistry registry = registryMap.get(scope);

    Map<String, List<Map>> map = new HashMap<>(1);

    List<Map> metrics = new ArrayList<>();
    if (registry!=null) { // They may be lazily initialised
      for (String name : registry.getNames()) {
        metrics.add(getSingleMetric(scope, name));
      }
    }
    map.put(scope.getName(),metrics);
    return map;
  }


  private Map getSingleMetric(MetricRegistry.Type scope, String metricName) {
    Metric m;
    Metadata meta;

    MetricRegistry registry = registryMap.get(scope);

    m = registry.getMetrics().get(metricName);
    meta = registry.getMetadata().get(metricName);

    Map<String,Object> out = new LinkedHashMap<>();
    out.put("name",meta.getName());
    out.put("type",meta.getType());
    if (meta.getUnit().isPresent()) {
      String value = meta.getUnit().get();
      out.put("unit", value);
    }
    if (meta.getDescription().isPresent()) {
      out.put("description",meta.getDescription().get());
    }

    switch (meta.getTypeRaw()) {
      case GAUGE:
        Gauge gauge = (Gauge) m;
        Number num = (Number) gauge.getValue();
        double dvalue = num.doubleValue();
        out.put("value",dvalue);
        break;
      case COUNTER:
        Counter counter = (Counter) m;
        out.put("count",counter.getCount());
        break;
      case TIMER:
        Timer timer = (Timer) m;
        out.put("count",timer.getCount());
        out.put("1minRate",timer.getOneMinuteRate());
        out.put("5minRate",timer.getFiveMinuteRate());
        out.put("15minRate",timer.getFifteenMinuteRate());
        out.put("meanRate",timer.getMeanRate());

        addSnapshot(out, timer.getSnapshot());
        break;
      case METERED:
        Metered metered = (Metered) m;
        out.put("count",metered.getCount());
        out.put("1minRate",metered.getOneMinuteRate());
        out.put("5minRate",metered.getFiveMinuteRate());
        out.put("15minRate",metered.getFifteenMinuteRate());
        out.put("meanRate",metered.getMeanRate());
        break;
      case HISTOGRAM:
        Histogram histogram = (Histogram) m;
        out.put("count",histogram.getCount());
        addSnapshot(out,histogram.getSnapshot());
        break;
      default:
        throw new NotSupportedException("Type not yet supported: " + meta.getType());
    }

    return out;
  }

  /**
   * Render the contents of @{@link Snapshot}
   * @param out The map to put the values in
   * @param snapshot The snapshot data
   */
  private void addSnapshot(Map<String, Object> out, Snapshot snapshot) {
    out.put("min",snapshot.getMin());
    out.put("median",snapshot.getMedian());
    out.put("mean",snapshot.getMean());
    out.put("max",snapshot.getMax());
    out.put("stddev",snapshot.getStdDev());
    out.put("75percentile",snapshot.get75thPercentile());
    out.put("95percentile",snapshot.get95thPercentile());
    out.put("98percentile",snapshot.get98thPercentile());
    out.put("99percentile",snapshot.get99thPercentile());
    out.put("999percentile",snapshot.get999thPercentile());
  }
}
