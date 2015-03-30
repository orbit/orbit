/*
Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1.  Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
2.  Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
    its contributors may be used to endorse or promote products derived
    from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ea.orbit.actors.annotation.processing;

import com.ea.orbit.actors.annotation.processing.ActorProcessor.MethodDefinition;

public class FactoryTemplate extends ActorProcessor.Factory
{
    public String generate()
    {
		StringBuffer builder = new StringBuffer();
		builder.append("\r\npackage ");
		builder.append( clazz.packageName);
		builder.append(";\r\n\r\n@com.ea.orbit.actors.annotation.OrbitGenerated\r\npublic class ");
		builder.append(factoryName);
		builder.append(" extends com.ea.orbit.actors.runtime.ActorFactory<");
		builder.append(interfaceFullName);
		builder.append(">\r\n{\r\n    @Override\r\n    public ");
		builder.append( interfaceFullName );
		builder.append(" createReference(String id)\r\n    {\r\n        return new ");
		builder.append( clazz.packageName );
		builder.append(".");
		builder.append( factoryName );
		builder.append(".");
		builder.append( referenceName );
		builder.append("(id);\r\n    }\r\n\r\n    public static ");
		builder.append( interfaceFullName );
		builder.append(" getReference(String id)\r\n    {\r\n        return new ");
		builder.append( clazz.packageName );
		builder.append(".");
		builder.append( factoryName );
		builder.append(".");
		builder.append( referenceName );
		builder.append("(id);\r\n    }\r\n\r\n    @Override\r\n    public int getInterfaceId()\r\n    {\r\n        return ");
		builder.append( interfaceId );
		builder.append(";\r\n    }\r\n\r\n    @Override\r\n    public Class<?> getInterface()\r\n    {\r\n        return ");
		builder.append( interfaceFullName );
		builder.append(".class;\r\n    }\r\n\r\n    @Override\r\n    public com.ea.orbit.actors.runtime.ActorInvoker getInvoker()\r\n    {\r\n        return new ");
		builder.append( clazz.packageName );
		builder.append(".");
		builder.append( factoryName );
		builder.append(".");
		builder.append( invokerName );
		builder.append("();\r\n    }\r\n\r\n    @com.ea.orbit.actors.annotation.OrbitGenerated\r\n    public static class ");
		builder.append( referenceName );
		builder.append("\r\n            extends com.ea.orbit.actors.runtime.ActorReference<");
		builder.append( interfaceFullName );
		builder.append(">\r\n            implements ");
		builder.append( interfaceFullName );
		builder.append("\r\n    {\r\n        public ");
		builder.append( referenceName );
		builder.append("(String id)\r\n        {\r\n            super(id);\r\n        }\r\n\r\n        @Override\r\n        protected int _interfaceId()\r\n        {\r\n            return ");
		builder.append( interfaceId );
		builder.append(";\r\n        }\r\n\r\n        @Override\r\n        public Class<");
		builder.append( interfaceFullName );
		builder.append("> _interfaceClass()\r\n        {\r\n            return ");
		builder.append( interfaceFullName );
		builder.append(".class;\r\n        }\r\n");
		for(MethodDefinition method : methods) { 
		builder.append("\r\n        public ");
		builder.append( method.returnType );
		builder.append(" ");
		builder.append( method.name );
		builder.append("(");
		builder.append( method.paramsList() );
		builder.append(")\r\n        {\r\n");
		if(method.oneway) { 
		builder.append("            return super.invoke(true, ");
		builder.append( method.methodId );
		builder.append(", new Object[]{");
		builder.append( method.wrapParams());
		builder.append("});\r\n");
		} else {
		builder.append("            return super.invoke(false, ");
		builder.append( method.methodId );
		builder.append(", new Object[]{");
		builder.append( method.wrapParams());
		builder.append("});\r\n");
		}
		builder.append("        }\r\n");
		}
		builder.append("    }\r\n\r\n    @com.ea.orbit.actors.annotation.OrbitGenerated\r\n    public static class ");
		builder.append( invokerName );
		builder.append("\r\n            extends com.ea.orbit.actors.runtime.ActorInvoker<");
		builder.append( interfaceFullName );
		builder.append(">\r\n    {\r\n        @Override\r\n        protected int _interfaceId()\r\n        {\r\n            return ");
		builder.append( interfaceId );
		builder.append(";\r\n        }\r\n\r\n        @Override\r\n        public com.ea.orbit.concurrent.Task<?> invoke(");
		builder.append( interfaceFullName );
		builder.append(" target, int methodId, Object[] params)\r\n        {\r\n            switch (methodId)\r\n            {\r\n");
		for(MethodDefinition method :methods) { 
		builder.append("                case ");
		builder.append( method.methodId );
		builder.append(":\r\n                    return target.");
		builder.append( method.name );
		builder.append("(");
		builder.append( method.unwrapParams("params"));
		builder.append(");\r\n");
		}
		builder.append("                default:\r\n                    throw new com.ea.orbit.exception.MethodNotFoundException(\"MethodId :\" +methodId);\r\n           }\r\n        }\r\n    }\r\n}");
		return builder.toString();
	}
}