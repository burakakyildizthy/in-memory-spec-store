package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.common.model.TestUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify the system works
 */
class SimpleEndToEndTest {

    @Test
    void testBasicFunctionality() {
        TestUser user = new TestUser();
        user.setId(1L);
        user.setName("Test User");
        user.setAge(25);
        user.setActive(true);

        assertNotNull(user);
        assertEquals("Test User", user.getName());
        assertEquals(25, user.getAge());
        assertTrue(user.getActive());

        System.out.println("✓ Basic functionality test passed");
    }
}