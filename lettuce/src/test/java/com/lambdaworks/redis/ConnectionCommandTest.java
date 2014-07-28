// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionCommandTest extends AbstractCommandTest {
    @Test
    public void auth() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisConnection<String, String> connection = client.connect();
                try {
                    connection.ping();
                    fail("Server doesn't require authentication");
                } catch (RedisException e) {
                    assertEquals("NOAUTH Authentication required.", e.getMessage());
                    assertEquals("OK", connection.auth(passwd));
                    assertEquals("OK", connection.set(key, value));
                }
            }
        };
    }

    @Test
    public void echo() throws Exception {
        assertEquals("hello", redis.echo("hello"));
    }

    @Test
    public void ping() throws Exception {
        assertEquals("PONG", redis.ping());
    }

    @Test
    public void select() throws Exception {
        redis.set(key, value);
        assertEquals("OK", redis.select(1));
        assertNull(redis.get(key));
    }

    @Test
    public void authReconnect() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisConnection<String, String> connection = client.connect();
                assertEquals("OK", connection.auth(passwd));
                assertEquals("OK", connection.set(key, value));
                connection.quit();
                assertEquals(value, connection.get(key));
            }
        };
    }

    @Test
    public void selectReconnect() throws Exception {
        redis.select(1);
        redis.set(key, value);
        redis.quit();
        assertEquals(value, redis.get(key));
    }

    @Test
    public void isValid() throws Exception {

        assertTrue(Connections.isValid(redis));

        RedisAsyncConnection<String, String> asyncConnection = client.connectAsync();
        assertTrue(Connections.isValid(asyncConnection));
        Connections.close(asyncConnection);
        assertFalse(Connections.isOpen(asyncConnection));
        assertFalse(Connections.isValid(asyncConnection));
    }

    @Test
    public void getSetReconnect() throws Exception {
        redis.set(key, value);
        redis.quit();
        assertEquals(value, redis.get(key));
    }

    @Test
    public void authInvalidPassword() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        try {
            async.auth("invalid");
            fail("Authenticated with invalid password");
        } catch (RedisException e) {
            assertEquals("ERR Client sent AUTH, but no password is set", e.getMessage());
            Field f = async.getClass().getDeclaredField("password");
            f.setAccessible(true);
            assertNull(f.get(async));
        } finally {
            async.close();
        }
    }

    @Test
    public void selectInvalid() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        try {
            async.select(1024);
            fail("Selected invalid db index");
        } catch (RedisException e) {
            assertEquals("ERR invalid DB index", e.getMessage());
            Field f = async.getClass().getDeclaredField("db");
            f.setAccessible(true);
            assertEquals(0, f.get(async));
        } finally {
            async.close();
        }
    }
}
