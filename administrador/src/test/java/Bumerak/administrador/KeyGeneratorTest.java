package Bumerak.administrador;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class KeyGeneratorTest {

    @Getter
    private static String secretKey;


    @Test
    void generateSecureKey() {

        SecureRandom random = new SecureRandom();

        // 32 bytes equivale a 256 bits, tamaño suficiente para el algoritmo HMAC-SHA-256
        byte[] key = new byte[32];
        random.nextBytes(key);

        System.out.println(Arrays.toString(key));

        // Convertir a Base64 para mayor comodidad
        String secretKey = Base64.getEncoder().encodeToString(key);
        System.out.println(secretKey);// Cambia esto por una clave secreta segura
        //4LyjLvUySomBqqvJPH3LZ6x9mwIuX12GdqhpYU1nrb4=
    }
}
