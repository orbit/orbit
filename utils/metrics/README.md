Orbit Metrics
============

Orbit Metrics is a wrapper for the Dropwizard Metrics library. Once the MetricsManager is initialized with one or more Reporters, you can register objects contaning annotated fields or methods for export.

An Orbit Container module is provided to initialize the MetricsManager using the orbit.yaml config file. 

An Orbit Actors Extension "orbit-actors-metrics" that allows Actors to declare annotated fields and methods and have them automatically registered and unregistered when the actor is activated or deactivated.

Currently, the following Dropwizard reporters are supported:
 - JMX
 - Graphite
 - Ganglia
 - SLF4J


Metric Scopes
---------------

Orbit Metrics supports two scopes: Singleton and Prototype (instance). Singleton Metrics are unique per JVM and are generally intended for cases where there exists only one instance of an object that produces that metric. Prototype or Instance Metrics are intended to be used when more than one instance of an object can produce a particular metric.
 
In order to register objects that export prototype metrics, a unique runtime id must be provided at the time of registration. 

Example
========

The Chat Example for Orbit Actors has had Metrics added to it. See [Chat](../../samples/chat)

```java

public class MetricsProducer
{
	@ExportMetric(name="coolMetric")
	int metric1;
	
	@ExportMetric(name="neatMetric", scope=MetricScope.PROTOTYPE)
	int uniqueMetric;
	
	String producerName;
	
	public void setProducerName() { ... }
	
	public String getProducerName() { ... }
}

public static void main(String args[])
{
	MetricsProducer producer = new MetricsProducer();
	List<ReporterConfig> reporterConfigs = new ArrayList<>();
	JMXReporterConfig jmxReporter = new JMXReporterConfig();
	reporterConfigs.add(jmxReporter);
	
    MetricsManager.getInstance().initializeMetrics(reporterConfigs);
	MetricsManager.getInstance().registerExportedMetrics(producer, producer.getProducerName());
}

```