package br.com.best2bee.cliente_service.temp;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GerarChaves {

    public static void main(String[] args) throws Exception {
        Path pasta = Path.of(
                System.getProperty("user.home"),
                ".hotel-keys"
        );

        Files.createDirectories(pasta);

        Path chavePrivada = pasta.resolve("private.pem");
        Path chavePublica = pasta.resolve("public.pem");

        if (Files.exists(chavePrivada) || Files.exists(chavePublica)) {
            throw new IllegalStateException(
                    "Já existem arquivos de chave em: " + pasta
                    + ". Eles não serão sobrescritos."
            );
        }

        var gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);

        var chaves = gerador.generateKeyPair();

        salvar(chavePrivada, "PRIVATE KEY",
                chaves.getPrivate().getEncoded());

        salvar(chavePublica, "PUBLIC KEY",
                chaves.getPublic().getEncoded());

        System.out.println("Chaves criadas em: " + pasta);
    }

    private static void salvar(
            Path arquivo,
            String tipo,
            byte[] chave) throws Exception {

        String base64 = Base64.getMimeEncoder(
                64, new byte[]{'\n'}
        ).encodeToString(chave);

        String pem = "-----BEGIN " + tipo + "-----\n"
                + base64
                + "\n-----END " + tipo + "-----\n";

        Files.writeString(
                arquivo,
                pem,
                StandardCharsets.US_ASCII,
                StandardOpenOption.CREATE_NEW
        );
    }
}