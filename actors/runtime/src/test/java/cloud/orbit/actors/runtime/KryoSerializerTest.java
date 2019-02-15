/*
 Copyright (C) 2019 Electronic Arts Inc.  All rights reserved.

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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultClassResolver;

import java.util.Objects;
import java.util.function.Supplier;

public class KryoSerializerTest
{

    private void runSerializeDeserializeTest(Supplier<KryoSerializer> kryoSerializerSupplier,
                                             Object in,
                                             Object expectedOut) throws Exception
    {
        BasicRuntime basicRuntime = Mockito.mock(BasicRuntime.class);
        KryoSerializer kryoSerializer = kryoSerializerSupplier.get();

        byte[] bytes = kryoSerializer.serializeMessage(basicRuntime, new Message().withPayload(in));
        Message message = kryoSerializer.deserializeMessage(basicRuntime, bytes);

        Assert.assertEquals(expectedOut, message.getPayload());
    }

    @Test
    public void serializeAndDeserialize_withDefaultClassResolver() throws Exception
    {
        runSerializeDeserializeTest(KryoSerializer::new, new ExampleDTO("a"), new ExampleDTO("a"));
    }

    @Test
    public void serializeAndDeserialize_withCustomClassResolver() throws Exception
    {
        runSerializeDeserializeTest(
                () -> new KryoSerializer(
                        kryo -> {},
                        new DefaultClassResolver()
                        {
                            @Override
                            public Registration writeClass(final Output output, final Class type)
                            {
                                if (type == ExampleDTO.class)
                                {
                                    return super.writeClass(output, ExampleDTOCopy.class);
                                }
                                return super.writeClass(output, type);
                            }
                        }),
                new ExampleDTO("a"),
                new ExampleDTOCopy("a"));
    }

    private static class ExampleDTO
    {
        private final String a;

        public ExampleDTO(final String a)
        {
            this.a = a;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o == null || getClass() != o.getClass()) return false;
            return Objects.equals(a, ((ExampleDTO) o).a);
        }
    }

    private static class ExampleDTOCopy
    {
        private final String a;

        public ExampleDTOCopy(final String a)
        {
            this.a = a;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o == null || getClass() != o.getClass()) return false;
            return Objects.equals(a, ((ExampleDTOCopy) o).a);
        }
    }
}
