The json message serializer is meant to be used by client connections.
One sample usage is websocket based connections.

Limitations:
 * No support for serializing object graphs.

Features:
 * Adds the field "@type" with to objects whose type is ambiguous during serialization.
    * "@type" by default will contain the orbit classId of the object: <br/>
      Usually `object.getClass().getName().replace('$','.').hashcode()`.
    * Will use the field "@type" resolve type ambiguities during deserialization.


Message format
----
```
{
  "messageType" : int8       // 0 = oneWay; 1 = request; 2,3,4 - responses
  "messageId"   : int32,     // unique messageId (required for responses and requests)
  "interfaceId" : int32,     // target actor interface Id (required for oneway and requests)
  "headers"     : { }        // map with the message headers, optional
  "objectId"    : string     // actor Id (required for oneway and requests, may be null depending of the target)
  "methodId"    : int32,     // target method id (required for oneway and requests)
  "payload"     : any        // array, map, number, string, boolean, null (required may be null)
}
```

Message types:
----

0. One way: No response will be sent
1. Request: A response will be sent
2. Response: A normal response
3. Error Response: A normal response
4. Protocol Error Response: An error occurred with the protocol. Usually invalid data.


One way messages:
```
{
  "messageType" : 0          // 0 = one way
  "messageId"   : int32,     // unique messageId, optional, recommended.
  "interfaceId" : int32,     // target actor interface Id
  "headers"     : { }        // map with the message headers, optional
  "objectId"    : string     // actor Id
  "methodId"    : int32,     // target method id
  "payload"     : any        // parameter array, required, unless void method
}
```

Request way messages:
```
{
  "messageType" : 1          // 1 = request
  "messageId"   : int32,     // unique messageId
  "interfaceId" : int32,     // target actor interface Id
  "headers"     : { }        // map with the message headers, optional
  "objectId"    : string     // actor Id
  "methodId"    : int32,     // target method id
  "payload"     : any        // parameter array, required, unless void method
}
```

Response Messages
```
{
  "messageType" : 2          // 2,3,4 = responses
  "messageId"   : int32,     // original request messageId
  "headers"     : { }        // map with the message headers, optional
  "payload"     : any        // required, unless null. may be: string, number, boolean, null, array, map
}
```
