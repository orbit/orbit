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

package cloud.orbit.concurrent.test;

import cloud.orbit.concurrent.Task;
import cloud.orbit.tuples.Pair;
import cloud.orbit.util.StringUtils;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class TaskTest
{

    public abstract static class NotNeeded
    {
        public abstract Object get();

        public abstract Object get(long t, TimeUnit tu);

        public abstract Object isDone(long t, TimeUnit tu);
    }

    @Test
    public void checkOverrides()
    {

        final Set<Pair<String, List<Class<?>>>> taskMethods =
                Stream.concat(Stream.of(Task.class.getDeclaredMethods()), Stream.of(NotNeeded.class.getDeclaredMethods()))
                        .map(m -> Pair.of(m.getName(), Arrays.asList(m.getParameterTypes())))
                        .collect(Collectors.toSet());

        Set<String> ignore = new HashSet<>(Arrays.asList(
                "get,getNow,cancel,completedFuture,obtrudeValue,obtrudeException,isDone"
                        .split(",")));


        final List<Method> notOverriden = Stream.of(CompletableFuture.class.getMethods())
                .filter(m -> !ignore.contains(m.getName()))
                .filter(m -> !taskMethods.contains(Pair.of(m.getName(), Arrays.asList(m.getParameterTypes()))))
                .filter(m -> m.getParameterTypes().length > 0)
                .filter(m -> !(m.getGenericReturnType() instanceof Class))
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());


        if (notOverriden.size() > 0)
        {
//            notOverriden.stream()
//                    .forEach(p -> System.out.println(p));
//
//            notOverriden.stream()
//                    .flatMap(m -> Stream.of(m.getParameterTypes()))
//                    .distinct()
//                    .forEach(p -> System.out.println(p));

            Pattern captures = Pattern.compile("BiFunction|Runnable|BiConsumer|Consumer|Function|Supplier", Pattern.CASE_INSENSITIVE);

            notOverriden.stream().forEach(p -> {
                if (!Modifier.isStatic(p.getModifiers()))
                {
                    System.out.println("\t@Override");
                    System.out.print("\tpublic ");
                }
                else
                {
                    System.out.print("\tpublic static ");
                }
                if (p.getTypeParameters().length > 0)
                {
                    final Object collect = Stream.of(p.getTypeParameters())
                            .map(m -> m.toString())
                            .collect(Collectors.joining(", "));
                    System.out.print("<" +
                            collect + "> ");
                }
                System.out.println(p.getGenericReturnType().toString().replaceAll("^.*(CompletableFuture|CompletionStage)", "Task") + " "
                        + p.getName() + "(" +
                        Stream.of(p.getGenericParameterTypes())
                                .map(c -> lastName(c.getTypeName()) + " " + toVarName(c.getTypeName()))
                                .collect(Collectors.joining(", "))
                        + ") {");
                System.out.print("\t\treturn Task.from(");
                if (Modifier.isStatic(p.getModifiers()))
                {
                    System.out.print("CompletableFuture");
                }
                else
                {
                    System.out.print("super");
                }
                System.out.println("." + p.getName() + "(" +
                        Stream.of(p.getGenericParameterTypes())
                                .map(c -> captures.matcher(toVarName(c.getTypeName())).matches()
                                        ? "ExecutionContext.wrap(" + toVarName(c.getTypeName()) + ")"
                                        : toVarName(c.getTypeName()))
                                .collect(Collectors.joining(", "))
                        + "));");
                System.out.println("\t}");

            });
        }
        assertEquals("Task must override completable future methods", 0, notOverriden.size());

    }

    private String toVarName(final String typeName)
    {
        final String name = typeName.replaceAll("^[^<]*[.]", "").replaceAll("<.*", "");
        return StringUtils.uncapitalize(name).toString();
    }

    private String lastName(final String typeName)
    {
        return typeName.replaceAll("^[^<]*[.]", "");
    }


}
