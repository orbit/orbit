package com.ea.orbit.bridge;

import java.io.Serializable;

public class BridgeResult implements Serializable {

    public String result;
    public Object value;

    public static BridgeResult success(Object value){
        BridgeResult tmp = new BridgeResult();
        tmp.result="success";
        tmp.value=value;
        return tmp;
    }

    public static BridgeResult fail(String reason){
        BridgeResult tmp = new BridgeResult();
        tmp.result="fail";
        tmp.value=reason;
        return tmp;
    }

}
