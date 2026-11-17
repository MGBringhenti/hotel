import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class GerarChaves {
    private static byte[] lerPem(Path path) throws Exception {
        String texto = Files.readString(path).replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(texto);
    }
    private static void escreverPem(Path path, String tipo, byte[] dados) throws Exception {
        String corpo = Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(dados);
        Files.writeString(path, "-----BEGIN " + tipo + "-----\n" + corpo + "\n-----END " + tipo + "-----\n");
    }
    public static void main(String[] args) throws Exception {
        Path privada = Path.of("/keys/private/private.pem");
        Path publica = Path.of("/keys/public/public.pem");
        Files.createDirectories(privada.getParent());
        Files.createDirectories(publica.getParent());
        if (!Files.exists(privada)) {
            if (Files.exists(publica)) throw new IllegalStateException("Chave pública sem chave privada. Restaure o par original.");
            var gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(3072);
            var par = gerador.generateKeyPair();
            escreverPem(privada, "PRIVATE KEY", par.getPrivate().getEncoded());
        }
        var factory = KeyFactory.getInstance("RSA");
        var chave = (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(lerPem(privada)));
        var derivada = factory.generatePublic(new RSAPublicKeySpec(chave.getModulus(), chave.getPublicExponent()));
        if (Files.exists(publica)) {
            if (!Arrays.equals(lerPem(publica), derivada.getEncoded())) throw new IllegalStateException("As chaves JWT existentes não correspondem.");
        } else {
            escreverPem(publica, "PUBLIC KEY", derivada.getEncoded());
        }
        Files.setPosixFilePermissions(privada, PosixFilePermissions.fromString("rw-------"));
        Files.setPosixFilePermissions(publica, PosixFilePermissions.fromString("rw-r--r--"));
        System.out.println("Par de chaves JWT pronto. Chaves existentes preservadas.");
    }
}
