package com.hoffi.minimal.microservices.microservice.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import annotations.TrivialTest;

class MessageDTOTest {

    @Disabled
    @TrivialTest
    void failTests() {}

    @TrivialTest
    void functionalTests() {
        String testOpName = new Object() {}.getClass().getEnclosingMethod().getName(); // this method name
        testOpName = MessageDTOTest.class.getSimpleName() + "." + testOpName;

        MessageDTO m1 = MessageDTO.create("<initial>", "");
        m1.seq = "44";
        assertEquals("44", m1.seq);
        assertEquals("<initial>", m1.message);
        assertEquals("", m1.modifications);
        
        String newMessage = "newMessage";
        String modificationString = "TrivialTest";
        MessageDTO m2 = m1.transform(newMessage, modificationString);
        assertTrue(m1 != m2);
        assertEquals(m2.seq, m1.seq);
        assertEquals(newMessage, m2.message);
        assertEquals(modificationString, m2.modifications);

        assertNotNull(m2.toString());
    }

}
