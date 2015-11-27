Json Message Serializer
====

The json message serializer is an actor pipeline handler that
converts message objects into json.

Uses
---

The json message serializer is meant to be used with client connections.
One example usage are websocket based connections.

It can also be used as the cluster communication protocol, tough it imposes limitations to the message payload.

Features
---


 * Adds the field "@type" with to objects whose type is ambiguous during serialization.
    * "@type" by default will contain the orbit classId of the object: <br/>
      Usually `object.getClass().getName().replace('$','.').hashcode()`.
    * Will use the field "@type" resolve type ambiguities during deserialization.


Message format
----

```
{
  "messageType" : int8,      // 0 = oneWay; 1 = request; 2,3,4 - responses
  "messageId"   : int32,     // unique messageId (required for responses and requests)
  "interfaceId" : int32,     // target actor interface Id (required for oneway and requests)
  "headers"     : { },       // map with the message headers, optional
  "objectId"    : string,    // actor Id (required for oneway and requests, may be null depending of the target)
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


#### One way messages:

One way messages represent remote method invocations where no return is expected.
They need to specify the target object and the parameters.

```
{
  "messageType" : 0,         // 0 = one way
  "messageId"   : int32,     // unique messageId, optional, recommended.
  "interfaceId" : int32,     // target actor interface Id
  "headers"     : { },       // map with the message headers, optional
  "objectId"    : string     // actor Id
  "methodId"    : int32,     // target method id
  "payload"     : any        // parameter array, required, unless void method
}
```

#### Request messages:

Request are remote method invocations where a response is expected.
The response will be the method return value or an error.
They need to specify the target object, the parameters, and a message id.

The response will be matched to the request by the messageId.


```
{
  "messageType" : 1,         // 1 = request
  "messageId"   : int32,     // unique messageId
  "interfaceId" : int32,     // target actor interface Id
  "headers"     : { },       // map with the message headers, optional
  "objectId"    : string     // actor Id
  "methodId"    : int32,     // target method id
  "payload"     : any        // parameter array, required, unless void method
}
```

#### Response Messages

Responses are the result of a remote request. Either a success (messageType=2) or
a failure, (2=application error, 3=protocol error).

The payload of the response will contain the serialized value returned by the application.
or a json representation of the error that occurred.

```
{
  "messageType" : 2,         // 2,3,4 = responses
  "messageId"   : int32,     // original request messageId
  "headers"     : { },       // map with the message headers, optional
  "payload"     : any        // required, unless null. may be: string, number, boolean, null, array, map
}
```

Limitations
----

 * No support for serializing object graphs.

 * Ambiguous array types will be deserialized as ArrayList.
   that's because the @type field is never added to, nor expected in, arrays.

     ```java
     class SomeData
     {
        Object a;
     }
     ```

     Example: if the json `"a": "[1,2,3]"` is provided as the serialized form of the field `someData.a`
     it will always be deserialized as an ArrayList, even if the original data in the serialization end was Object[].

     The array type won't be ambiguous if:

     ```java
     class SomeData
     {
        int[] a;
        // or: Object[] a;
        // or: double[] a;
     }
     ```

     This only affect methods whose signatures or data types contain ambiguous field types.

     This is the same limitation encountered in regular REST apis.


