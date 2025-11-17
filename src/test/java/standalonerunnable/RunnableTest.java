package standalonerunnable;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

public class RunnableTest {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Argument <password> is required");
            return;
        }
        final var encryptor = new PooledPBEStringEncryptor();
        final var defaultJasyptConfig = new SimpleStringPBEConfig();
        defaultJasyptConfig.setPasswordCharArray(args[0].toCharArray());
        defaultJasyptConfig.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        defaultJasyptConfig.setKeyObtentionIterations("1000");
        defaultJasyptConfig.setPoolSize(1);
        defaultJasyptConfig.setProviderName("SunJCE");
        defaultJasyptConfig.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        defaultJasyptConfig.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        defaultJasyptConfig.setStringOutputType("base64");
        encryptor.setConfig(defaultJasyptConfig);
        System.out.println("Encrypt: " + encryptor.encrypt(args[1]));
    }
}
