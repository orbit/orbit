package com.ea.orbit.bridge;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class BridgeActor extends OrbitActor implements IBridge{

    ObjectMapper mapper = new ObjectMapper();

    public BridgeActor(){
        super();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Override
    public Task<BridgeResult> call(String url, Class responseClass) {
        try {
            ClientResource resource = new ClientResource(url);
            JsonRepresentation represent = new JsonRepresentation(resource.get());
            Object value = mapper.readValue(represent.getJsonObject().toString(),responseClass);
            return Task.fromValue(BridgeResult.success(value));
        }catch(Exception e){
            e.printStackTrace();
            return Task.fromValue(BridgeResult.fail(e.getMessage()));
        }
    }
}
