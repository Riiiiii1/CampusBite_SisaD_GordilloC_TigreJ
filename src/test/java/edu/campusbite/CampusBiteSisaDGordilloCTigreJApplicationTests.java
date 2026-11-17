package edu.campusbite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
// Con ayuda de ChatGPT revisé el cierre del contexto para liberar el registro RMI
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CampusBiteSisaDGordilloCTigreJApplicationTests {

    @Test
    void contextLoads() {
    }

}
