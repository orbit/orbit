# Orbit Metrics


Orbit Metrics is a wrapper for the Dropwizard Metrics library. Once the MetricsManager is initialized with one or more Reporters, you can register objects contaning annotated fields or methods for export.

An Orbit Container module is provided to initialize the MetricsManager using the orbit.yaml config file. 

An Orbit Actors Extension "orbit-actors-metrics" that allows Actors to declare annotated fields and methods and have them automatically registered and unregistered when the actor is activated or deactivated.

Currently, the following Dropwizard reporters are supported:
 - JMX
 - Graphite
 - Ganglia
 - SLF4J


## Metric Scopes


Orbit Metrics supports two scopes: Singleton and Prototype (instance). Singleton Metrics are unique per JVM and are generally intended for cases where there exists only one instance of an object that produces that metric. Prototype or Instance Metrics are intended to be used when more than one instance of an object can produce a particular metric.
 
In order to register objects that export prototype metrics, a unique runtime id must be provided at the time of registration. 

### Example

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

## JVM Metrics


Orbit Metrics also allows for the activation of the Dropwizard JVM metrics via an Orbit Startable named JvmMetricsComponent. Simply add it to the list of components in your orbit.yaml file to activate it like so:

```
orbit.components:
	...
	- com.ea.orbit.metrics.JvmMetricsComponent
```

Currently, the following JVM metrics are exposed:
 - Garbage Collection (provided by Dropwizard)
 - Memory Usage (provided by Dropwizard)
 - Thread Metrics with Deadlock detection (provided by Dropwizard)
 - Thread CPU Time usage
 
 ### Metric Information

 #### Garbage Collection Metrics
 
 GC Metrics are named like: gc.\<GarbageCollectorName\>.\<MetricName\>
 
 | Metric Name | Description 									| Units |
 | ----------- | ----------------------------------------------	| ----- |
 | count 	   | The number of times this GC has run 		  	| runs 	|
 | time        | The total accumulated time spend collecting 	| msec 	|
 
 #### Memory Usage Metrics
 
 Memory Metrics are named like: memory.\<MemoryPoolName\>.\<MetricName\>
 
 | Metric Name	| Description 															| Units |
 | ------------ | ---------------------------------------------------------------------	| ----- |
 | init			| The initial amount of Memory requested by the JVM 		  			| bytes 	|
 | used			| The amount of memory currently being used								| bytes 	|
 | max			| The maximum amount of memory to be used in this pool. 				| bytes 	|
 | committed	| The amount of memory that is guaranteed to be available for the JVM 	| bytes 	|
 | usage        | Ratio of Used over Max. 												| % 	|
 
 #### Thread Metrics
 
 Thread Metrics are named like: threads.\<MetricName\>
 
 | Metric Name	| Description 															| Units |
 | ------------ | ---------------------------------------------------------------------	| ----- |
 | <ThreadStateName>.count	| The number of threads in a particular thread state		|  	|
 | count					| The Number of live threads								|  	|
 | daemon.count 			| The number of live daemon threads 						|  	|
 | deadlock.count			| The number of deadlocked threads 							|  	|
 | deadlocks        		| A set of stack traces of any deadlocked threads 			| 	|
 
  #### Thread CPU Metrics
 
 Thread CPU Metrics are named like: threads.thread.\<ThreadName\>.cputime
 
 | Metric Name	| Description 										| Units |
 | ------------ | ----------------------------------------------	| ----- |
 | cputime		| The cumulative CPU time consumed by this thread	| nanoseconds 	|

 