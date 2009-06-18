/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */
package edu.ucsb.eucalyptus.util;

import java.nio.ByteBuffer;


public class WalrusDataMessage {
    private Header header;
    private byte[] payload;
    private static final String DELIMITER = "/";

    public enum Header {
        START, DATA, INTERRUPT, EOF
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public WalrusDataMessage(Header header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    public WalrusDataMessage() {}

    public static WalrusDataMessage EOF() {
        return new WalrusDataMessage(Header.EOF, new byte[0]);
    }

    public static WalrusDataMessage InterruptTransaction() {
        return new WalrusDataMessage(Header.INTERRUPT, new byte[0]);
    }

    public static WalrusDataMessage StartOfData(long size) {
        return new WalrusDataMessage(Header.START, String.valueOf(size).getBytes());
    }

    public static WalrusDataMessage DataMessage(byte[] data) {
        return new WalrusDataMessage(Header.DATA, data);
    }

    public static WalrusDataMessage DataMessage(byte[] data, int length) {
        byte[] bytes = new byte[length];
        copyBytes(data, bytes, 0, length);
        return new WalrusDataMessage(Header.DATA, bytes);
    }

    public static WalrusDataMessage DataMessage(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes, 0, length);
        return new WalrusDataMessage(Header.DATA, bytes);
    }


    public static boolean isStart(WalrusDataMessage message) {
        if(Header.START.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static boolean isEOF(WalrusDataMessage message) {
        if(Header.EOF.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static boolean isInterrupted(WalrusDataMessage message) {
        if(Header.INTERRUPT.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static boolean isData(WalrusDataMessage message) {
        if(Header.DATA.equals(message.header)) {
            return true;
        }
        return false;
    }

    public static void copyBytes(byte[]sourceBytes, byte[]destBytes, int offset, int length) {
        System.arraycopy(sourceBytes, 0, destBytes, offset, length);
    }
}