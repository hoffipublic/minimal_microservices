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
        BOP modulebop = BOP.initModule("testms", "1", "testmodule");
        BOP bop = modulebop.createBOP("testop");
        MessageDTO m1 = MessageDTO.create(bop);
        assertEquals(Integer.valueOf(1), m1.seq);
        assertEquals("<initial>", m1.message);
        assertEquals("", m1.modifications);
        assertEquals("testms", m1.bops.getFirst().microservice);
        assertEquals("1", m1.bops.getFirst().instanceIndex);
        assertEquals("testmodule", m1.bops.getFirst().businessModule);
        assertEquals(1, m1.bops.getFirst().bopIds.size());
        assertEquals("1", m1.bops.getFirst().bopIds.stream().collect(Collectors.joining(",")));
        
        String newMessage = "newMessage";
        String modificationString = "TrivialTest";
        MessageDTO m2 = m1.transform(bop, newMessage, modificationString);
        assertTrue(m1 != m2);
        assertEquals(m2.seq, m1.seq);
        assertEquals(newMessage, m2.message);
        assertEquals(modificationString, m2.modifications);
        assertEquals("testms", m1.bops.getFirst().microservice);
        assertEquals("1", m1.bops.getFirst().instanceIndex);
        assertEquals("testmodule", m1.bops.getFirst().businessModule);
        assertEquals(1, m1.bops.getFirst().bopIds.size());
        assertEquals("1", m1.bops.getFirst().bopIds.stream().collect(Collectors.joining(",")));

        assertNotNull(m2.toString());
    }

}
