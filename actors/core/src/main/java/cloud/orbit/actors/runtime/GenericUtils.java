/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.runtime;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * @author Daniel Sperry
 */
class GenericUtils
{
    // TODO: this class is the same from the RestClient, unify?

    /**
     * Converts a type to a jvm signature with the type parameters replaced.
     *
     * @param type the generic java type to be converted
     * @return a generic signature like:  {@code Ljava/util/List<Ljava/lang/Number;>;}
     */
    static String toGenericSignature(final Type type)
    {
        StringBuilder sb = new StringBuilder();
        toGenericSignature(sb, type);
        return sb.toString();
    }

    // see: http://stackoverflow.com/questions/29797393/how-to-convert-method-getgenericreturntype-to-a-jvm-type-signature
    static void toGenericSignature(StringBuilder sb, final Type type)
    {
        if (type instanceof GenericArrayType)
        {
            sb.append("[");
            toGenericSignature(sb, ((GenericArrayType) type).getGenericComponentType());
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            sb.append('L');
            sb.append(((Class) pt.getRawType()).getName().replace('.', '/'));
            sb.append('<');
            for (Type p : pt.getActualTypeArguments())
            {
                toGenericSignature(sb, p);
            }
            sb.append(">;");
        }
        else if (type instanceof Class)
        {
            Class clazz = (Class) type;
            if (!clazz.isPrimitive() && !clazz.isArray())
            {
                sb.append('L');
                sb.append(clazz.getName().replace('.', '/'));
                sb.append(';');
            }
            else
            {
                sb.append(clazz.getName().replace('.', '/'));
            }
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wc = (WildcardType) type;
            Type[] lowerBounds = wc.getLowerBounds();
            Type[] upperBounds = wc.getUpperBounds();
            boolean hasLower = lowerBounds != null && lowerBounds.length > 0;
            boolean hasUpper = upperBounds != null && upperBounds.length > 0;

            if (hasUpper && hasLower && Object.class.equals(lowerBounds[0]) && Object.class.equals(upperBounds[0]))
            {
                sb.append('*');
            }
            else if (hasLower)
            {
                sb.append("-");
                for (Type b : lowerBounds)
                {
                    toGenericSignature(sb, b);
                }
            }
            else if (hasUpper)
            {
                if (upperBounds.length == 1 && Object.class.equals(upperBounds[0]))
                {
                    sb.append("*");
                }
                else
                {
                    sb.append("+");
                    for (Type b : upperBounds)
                    {
                        toGenericSignature(sb, b);
                    }
                }
            }
            else
            {
                sb.append('*');
            }
        }
        else if (type instanceof TypeVariable)
        {
            // This is the other option: sb.append("T").append(((TypeVariable) type).getName()).append(";");
            // Instead replace the type variable with it's first bound.
            toGenericSignature(sb, ((TypeVariable) type).getBounds()[0]);
        }
        else
        {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

}
