Orbit can be extended by extensions, this page serves as a list to locate useful extensions to the framework.

| Extension| Type |  Description  |Official|
|:---------|:-----|:--------------|:-------|
|[Infinispan/JGroups](https://github.com/orbit/orbit/tree/master/actors/infinispan-cluster)|ClusterPeer|The built-in ClusterPeer extension for Orbit, using Infinispan and JGroups|Yes|
|[JSON](https://github.com/orbit/orbit/tree/master/actors/json)|Storage/Serializer|The built-in InMemoryStorageExtension and JSON serializer|Yes|
|[SimpleStream](https://github.com/orbit/orbit/tree/master/actors/runtime/src/main/java/cloud/orbit/actors/streams/simple)|Stream|The built-in simple stream extension|Yes|
|[DynamoDB](https://github.com/orbit/orbit-dynamodb)|Storage|AWS DynamoDB Storage Extension|Yes|
|[S3](https://github.com/orbit/orbit-s3)|Storage|AWS S3 Storage Extension|Yes|
|[Dynamo S3](https://github.com/orbit/orbit-dynamo-s3)|Storage|Storage extension that defaults to AWS DynamoDB and then falls back to S3 for large records|Yes|
|[MongoDB](https://github.com/orbit/orbit-mongodb)|Storage|Storage extension for MongoDB|Yes|
|[HK2](https://github.com/orbit/orbit-hk2)|Lifetime/Container|An extension using HK2 to offer actor injection support and an application container|Yes|
|[Jetty](https://github.com/orbit/orbit-jetty)|Web|An extension using Jetty and HK2 to allow the development of web based services (REST, Servlet, Static and Websocket) using Orbit|Yes|