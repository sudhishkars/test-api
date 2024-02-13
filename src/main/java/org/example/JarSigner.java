package org.example;

import sun.security.util.SignatureFileVerifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.lang.String.format;

/**
 * Hello world!
 *
 */
public class JarSigner {
    public static void main( String[] args ) {
        String jarFile = args[0];
        String keyStore = args[1];
        String storepass = args[2];
        String alias = args[3];
        try {
            JarFile jar = newJarFile(jarFile);
            KeyStore keystore = loadTruststore (keyStore, "jks", storepass);

        } catch (Exception e) {
            System.out.println ("Error: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void verify (JarFile jarFile, KeyStore truststore, String alias)  throws Exception{
        Vector<JarEntry> jarEntries = readJarEntries(jarFile);

        Manifest manifest = jarFile.getManifest();

        validate(truststore, alias, manifest, jarEntries);
    }

    private static void validate(KeyStore truststore, String alias, Manifest manifest, Vector<JarEntry> jarEntries)  {

        int filesCount = 0;
        int signedFilesCount = 0;


        for (JarEntry jarEntry: jarEntries) {
            if (jarEntry.isDirectory() || SignatureFileVerifier.isBlockOrSF(jarEntry.getName())) continue;

            filesCount++;

            System.out.println ("Jar Entry: " + jarEntry.getName());

            CodeSigner[] codeSigners = jarEntry.getCodeSigners();
            boolean isSigned = (codeSigners != null);

            if (!isSigned) continue;

            for (CodeSigner signer: codeSigners) {

                if (contains(truststore, alias, signer)) signedFilesCount++;
            }
        }

        if (filesCount != signedFilesCount) {
            String message = "Signature validation failed.";
            System.out.println (message);
        }
    }

    private static Vector<JarEntry> readJarEntries(JarFile jarFile) {

        Vector<JarEntry> jarEntries = new Vector<>();

        byte[] buffer = new byte[8192];

        Collections.list(jarFile.entries()).forEach(entry -> {
            jarEntries.add(entry);

            try {
                InputStream is = null;
                try {
                    is = jarFile.getInputStream(entry);
                    int n;
                    while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                        // we just read. this will throw a SecurityException
                        // if  a signature/digest check fails.
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (Exception e) {

            }
        });

        return jarEntries;
    }

    private static boolean contains(KeyStore keystore, String alias, CodeSigner codeSigner) {


        Optional<Certificate> certificate = getCertificate(keystore, alias);

        if (!certificate.isPresent()) {
            System.out.println ("Certificate with alias '{}' not found in truststore");
            return false;
        }

        return contains(certificate.get(), codeSigner);
    }


    private static Optional<Certificate> getCertificate(KeyStore keystore, String alias) {
        Certificate certificate;

        try {
            certificate = keystore.getCertificate(alias);
            return Optional.of(certificate);
        } catch (Exception e) {
            // Ignore
           System.out.println ("Unable to retrieve the certificate with alias " + e.getMessage());
        }

        return Optional.empty();
    }

    private static boolean contains(Certificate certificate, CodeSigner codeSigner) {

        List<? extends Certificate> certificates = codeSigner.getSignerCertPath().getCertificates();

        boolean verified = false;

        for(Certificate c: certificates) {
            System.out.println ("Signer Certificate pub key: " + c.getPublicKey());
            if (verify(c, certificate)) {
                System.out.println ("Signer Certificate pub key: " + c.getPublicKey());
                System.out.println ("Pub Key: " + certificate.getPublicKey());
                verified = true;
                break;
            }
        }

        return verified;
    }

    private static boolean verify(Certificate c1, Certificate c2) {
        try {
            c1.verify(c2.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private static JarFile newJarFile(String path) throws IOException {
        File file = new File(path);
        return new JarFile(file, true);
    }


    private static KeyStore loadTruststore(String truststorePath, String keystoreType, String keystorePassword) throws Exception {

        char[] password = (keystorePassword == null) ? null : keystorePassword.toCharArray();

        try {
            File file = new File(truststorePath);
            try (FileInputStream fis = new FileInputStream(file)) {
                KeyStore keystore = KeyStore.getInstance(keystoreType);
                keystore.load(fis, password);
                return keystore;
            } catch (Exception e) {
                String message = format("Unable to load Jar Signers truststore '%s'. Reason: %s", file.getAbsolutePath(), e.getMessage());
                System.out.println(message);
                throw e;
            }
        } catch (Exception e1) {
            String message = format("Unable to load Jar Signers truststore '%s'. Reason: %s", truststorePath, e1.getMessage());
            System.out.println(message);
            throw e1;
        }
    }
}
