package com.marketplace.auth.util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;

/**
 * Utility to generate RSA key pair for JWT signing and verification.
 * Run this once to generate keys before starting the application.
 * 
 * Usage: mvn compile exec:java -Dexec.mainClass="com.marketplace.auth.util.KeyPairGeneratorUtil"
 */
public class KeyPairGeneratorUtil {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String OUTPUT_DIR = "src/main/resources/keys";
    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE = "public_key.pem";

    public static void main(String[] args) {
        try {
            System.out.println("Generating RSA key pair...");
            
            // Generate key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            // Create output directory if it doesn't exist
            Path outputDir = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                System.out.println("Created directory: " + OUTPUT_DIR);
            }
            
            // Save private key
            savePrivateKey(keyPair.getPrivate(), Paths.get(OUTPUT_DIR, PRIVATE_KEY_FILE));
            System.out.println("Private key saved to: " + OUTPUT_DIR + "/" + PRIVATE_KEY_FILE);
            
            // Save public key
            savePublicKey(keyPair.getPublic(), Paths.get(OUTPUT_DIR, PUBLIC_KEY_FILE));
            System.out.println("Public key saved to: " + OUTPUT_DIR + "/" + PUBLIC_KEY_FILE);
            
            System.out.println("\nRSA key pair generated successfully!");
            System.out.println("Key size: " + KEY_SIZE + " bits");
            System.out.println("\nIMPORTANT: Keep the private key secure and never commit it to version control.");
            
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Error generating key pair: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void savePrivateKey(PrivateKey privateKey, Path filePath) throws IOException {
        String base64Encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String pemFormat = formatPem("PRIVATE KEY", base64Encoded);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(pemFormat);
        }
    }

    private static void savePublicKey(PublicKey publicKey, Path filePath) throws IOException {
        String base64Encoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String pemFormat = formatPem("PUBLIC KEY", base64Encoded);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(pemFormat);
        }
    }

    private static String formatPem(String type, String base64Content) {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(type).append("-----\n");
        
        // Split base64 string into 64-character lines
        int index = 0;
        while (index < base64Content.length()) {
            pem.append(base64Content, index, Math.min(index + 64, base64Content.length()));
            pem.append("\n");
            index += 64;
        }
        
        pem.append("-----END ").append(type).append("-----\n");
        return pem.toString();
    }
}
