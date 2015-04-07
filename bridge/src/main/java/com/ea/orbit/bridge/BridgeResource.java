package com.ea.orbit.bridge;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;


public class BridgeResource extends ServerResource {

    ObjectMapper mapper = new ObjectMapper();

    public BridgeResource(){
        super();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    //TODO post support

    @Get("json")
    public Representation bridge(){

        try{

            String path = getRequest().getOriginalRef().getPath();

            BridgeServer.BridgeInfo info = findMatch(path);
            if (info!=null){
                String actorId = (String)getRequest().getAttributes().get("id");
                IActor actor;
                if (actorId==null){
                    actor = IActor.getReference(info.interfaceClass);
                }else{
                    actor = IActor.getReference(info.interfaceClass,actorId);
                }

                Object[] params = new Object[info.method.getParameterCount()];
                for(int t=0;t<info.method.getParameterCount();t++){
                    Parameter p = info.method.getParameters()[t];
                    Object value = getRequest().getAttributes().get(p.getName());
                    Class parameterClass = info.method.getParameterTypes()[t];
                    if (!(parameterClass.isAssignableFrom(value.getClass()))){
                        if (parameterClass == int.class){
                            value = Integer.parseInt((String)value);
                        }else if (parameterClass == float.class){
                            value = Float.parseFloat((String) value);
                        } else if (parameterClass == double.class){
                            value = Double.parseDouble((String) value);
                        } else if (parameterClass == long.class){
                            value = Long.parseLong((String) value);
                        }else if (parameterClass.isArray()){
                            //todo array support
                        }
                    }
                    params[t]=value;
                }

                Method m = actor.getClass().getDeclaredMethod(info.method.getName(),info.method.getParameterTypes());

                Task result = (Task) m.invoke(actor, params);

                return new JsonRepresentation(mapper.writeValueAsString(Result.success(result.join())));
            }else{
                return new JsonRepresentation(mapper.writeValueAsString(Result.fail("actor unknown")));
            }

        }catch (Exception e){
            e.printStackTrace();
            try{
                return new JsonRepresentation(mapper.writeValueAsString(Result.fail("unknown error")));
            }catch(Exception ignored){
            }
        }

        return new StringRepresentation("");
    }

    public static class Result{
        public String result;
        public Object value;

        public static Result success(Object value){
            Result tmp = new Result();
            tmp.result="success";
            tmp.value=value;
            return tmp;
        }

        public static Result fail(String reason){
            Result tmp = new Result();
            tmp.result="fail";
            tmp.value=reason;
            return tmp;
        }
    }

    public BridgeServer.BridgeInfo findMatch(String path){
        String[] parts = path.split("/");
        List<BridgeServer.BridgeInfo> bridgeInfos = (List<BridgeServer.BridgeInfo>) getContext().getAttributes().get("bridgeInfos");

        for(BridgeServer.BridgeInfo info:bridgeInfos){
            String[] tmps = info.path.split("/");
            boolean skip=false;
            if (parts.length==tmps.length){
                for(int t=0;t< tmps.length;t++){
                    if (!tmps[t].equals(parts[t])) {
                        if ((tmps[t].indexOf('{') == -1)) {
                            skip = true;
                            break;
                        }
                    }
                }
                if (!skip)
                    return info;
            }
        }
        return null;
    }

}
