package com.hoffi.minimal.microservices.microservice.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.stream.Collectors;
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
        BOP opBOP = BOP.initInitially("testDomain", "testProcess", testOpName, "42", "5");

        MessageDTO m1 = MessageDTO.create(opBOP);
        assertEquals(Integer.valueOf(1), m1.seq);
        assertEquals("<initial>", m1.message);
        assertEquals("", m1.modifications);
        assertEquals("testProcess", m1.bops.getFirst().businessProcess);
        assertEquals("i42", m1.bops.getFirst().instanceIndex);
        assertEquals("testDomain", m1.bops.getFirst().businessDomain);
        assertEquals(1, m1.bops.getFirst().bpIds.size());
        assertEquals("5", m1.bops.getFirst().bpIds.stream().collect(Collectors.joining(",")));
        
        String newMessage = "newMessage";
        String modificationString = "TrivialTest";
        MessageDTO m2 = m1.transform(opBOP, newMessage, modificationString);
        assertTrue(m1 != m2);
        assertEquals(m2.seq, m1.seq);
        assertEquals(newMessage, m2.message);
        assertEquals(modificationString, m2.modifications);
        assertEquals("testProcess", m1.bops.getFirst().businessProcess);
        assertEquals("i42", m1.bops.getFirst().instanceIndex);
        assertEquals("testDomain", m1.bops.getFirst().businessDomain);
        assertEquals(1, m1.bops.getFirst().bpIds.size());
        assertEquals("5", m1.bops.getFirst().bpIds.stream().collect(Collectors.joining(",")));

        assertNotNull(m2.toString());
    }

}
