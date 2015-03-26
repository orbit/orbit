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

package com.ea.orbit.configuration;

import com.ea.orbit.exception.UncheckedException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class SecretManager
{
    private static final String MAGIC = "::::ORBIT-SECRET::::";
    private static final String AES = "AES";

    //TODO: @Config("orbit.security.keyfile") private String keyFile;

    private static final String DEFAULT_KEY = "ATA8UV00RnLCkvo0TbN8jRt7y+tAaJOn1vXAofwjUoA=";
    private static final int SALT_SIZE = 8;

    private SecretKey key;
    private SecureRandom random = new SecureRandom();

    @SuppressWarnings("PMD.SystemPrintln")
    public static void main(String[] args) throws NoSuchAlgorithmException
    {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES);
        keyGen.init(256); // for example
        SecretKey secretKey = keyGen.generateKey();
        System.out.println(Base64.encodeBase64String(secretKey.getEncoded()));
    }

    protected SecretKey getKey() throws UnsupportedEncodingException, GeneralSecurityException
    {
        if (key == null)
        {
            // TODO use keyFile, if present
            key = new SecretKeySpec(Base64.decodeBase64(DEFAULT_KEY), AES);
        }
        return key;
    }

    public String encrypt(Secret secret)
    {
        try
        {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            final byte[] salt = new byte[SALT_SIZE];
            random.nextBytes(salt);
            final byte[] bytes = Base64.encodeBase64(cipher.doFinal((Hex.encodeHexString(salt) + secret.getPlainText() + MAGIC).getBytes("UTF-8")));
            return bytes != null ? new String(bytes, "UTF-8") : null;
        }
        catch (GeneralSecurityException | UnsupportedEncodingException e)
        {
            throw new UncheckedException(e);
        }
    }

    public Secret decrypt(String data)
    {
        if (data == null)
        {
            return null;
        }
        try
        {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            String plainText = new String(cipher.doFinal(Base64.decodeBase64(data)), "UTF-8");
            if (plainText.endsWith(MAGIC))
            {
                return new Secret(plainText.substring(SALT_SIZE * 2, plainText.length() - MAGIC.length()));
            }
            return null;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new UncheckedException(e);
        }
        catch (GeneralSecurityException e)
        {
            return null;
        }
    }
}