package es.karmadev.locklogin.common.protection.type;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.section.EncryptionConfiguration;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualID;
import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;
import es.karmadev.locklogin.common.protection.CHash;
import es.karmadev.locklogin.common.protection.virtual.CVirtualInput;
import ml.karmaconfigs.api.common.string.random.RandomString;
import ml.karmaconfigs.api.common.string.text.TextContent;
import ml.karmaconfigs.api.common.string.text.TextType;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SHA512Hash extends PluginHash {

    /**
     * basically number of iterations
     */
    private static final int DEFAULT_COST = 512;
    private static final char[] pepper = "abcdefghijklopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789".toCharArray();
    /**
     * Algorithm to use, this is the best i know, PM me if you know better one
     */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    /**
     * Size of PBEKeySpec in method pbkdf2(args...)
     */
    private static final int SIZE = 1024;
    /**
     * Our Random
     */
    private final SecureRandom random;
    private final int cost;
    /**
     * Prefix for our hash, every user should change this
     */
    private final String ID = "$" + new RandomString(
            RandomString.createBuilder().withSize(16).withType(TextType.ALL_LOWER).withContent(TextContent.ONLY_LETTERS)
    ).create() + "$";

    /**
     * Initialize codification class
     */
    public SHA512Hash() {
        this.cost = DEFAULT_COST;
        byte[] seed = new byte[512];
        new SecureRandom().nextBytes(seed);
        this.random = new SecureRandom(seed);
    }

    private static int iterations(int cost) {
        if ((cost & ~0x200) != 0) {
            throw new IllegalArgumentException("cost: " + cost);
        }
        return 1 << cost;
    }

    /**
     * Hashes Password
     *
     * @param password   password to hash
     * @param salt       generated random salt
     * @param iterations how many iterations to use
     * @return hashed version of password
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, SIZE);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            System.out.println("Invalid SecretKeyFactory: " + e.getMessage());
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            System.out.println("No such algorithm: " + ALGORITHM + " : " + e1.getMessage());
        }
        return new byte[1];
    }

    /**
     * Hashes password with salt and pepper
     *
     * @param password The password to hash
     * @return token of salt,pepper(pepper is not stored),id cost and hash
     */
    private String hashInput(String password) {
        byte[] salt = new byte[SIZE / 4]; // size of salt
        random.nextBytes(salt); // generate new salt
        char ppr = pepper[random.nextInt(pepper.length)]; // get random pepper
        password = password + ppr; // add pepper to password
        byte[] dk = pbkdf2(password.toCharArray(), salt, 1 << cost); // hash it
        byte[] hash = new byte[salt.length + dk.length]; // hash it
        System.arraycopy(salt, 0, hash, 0, salt.length); // idk :D
        System.arraycopy(dk, 0, hash, salt.length, dk.length); // idk :D
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding(); // setup encoder
        return ID + cost + "$" + enc.encodeToString(hash); // encode hash and return with all other data
    }

    /**
     * Detects if user entered in the correct password
     *
     * @param password the password user entered in
     * @param token    the token of password stored in database
     * @return true if passwords match
     */
    private boolean auth(final String password, final String token) {
        String[] info = token.split("\\$");
        String salt_str = info[1];
        Pattern layout = Pattern.compile("\\$" + salt_str + "\\$(\\d\\d\\d?)\\$(.{512})");
        if (salt_str.length() <= 1)
            layout = Pattern.compile("\\$" + salt_str + "\\$(\\d\\d\\d?)\\$(.{512})");

        Matcher m = layout.matcher(token);
        if (!m.matches()) {
            return false;
        }
        int iterations = iterations(Integer.parseInt(m.group(1)));
        byte[] hash = Base64.getUrlDecoder().decode(m.group(2));
        byte[] salt = Arrays.copyOfRange(hash, 0, SIZE / 4);
        for (char ppr : pepper) {
            String passw;
            passw = password + ppr;
            byte[] check = pbkdf2(passw.toCharArray(), salt, iterations);

            int zero = 0;
            for (int idx = 0; idx < check.length; ++idx) {
                zero |= hash[salt.length + idx] ^ check[idx];
            }
            if (zero == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the hashing name
     *
     * @return the hashing name
     */
    @Override
    public String name() {
        return "sha512";
    }

    /**
     * Hash the input
     *
     * @param input the input to hash
     * @return the hashed input
     */
    @Override
    public HashResult hash(final String input) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();

        EncryptionConfiguration encryption = plugin.configuration().encryption();
        VirtualizedInput virtualized;

        if (encryption.virtualID()) {
            VirtualID id = hasher.virtualID();
            virtualized = id.virtualize(input);
        } else {
            virtualized = CVirtualInput.of(new int[0], false, input.getBytes(StandardCharsets.UTF_8));
        }

        String pwd = new String(virtualized.product(), StandardCharsets.UTF_8);
        String finalProduct = hashInput(pwd);

        if (encryption.applyBase64()) {
            virtualized = CVirtualInput.of(virtualized.refferences(), virtualized.valid(), base64(finalProduct).getBytes(StandardCharsets.UTF_8));
        } else {
            virtualized = CVirtualInput.of(virtualized.refferences(), virtualized.valid(), finalProduct.getBytes(StandardCharsets.UTF_8));
        }

        return CHash.of(this, virtualized);
    }

    /**
     * Get if the provided hash result needs a rehash
     *
     * @param result the hash result
     * @return if the hahs needs a rehash
     */
    @Override
    public boolean needsRehash(HashResult result) {
        Configuration configuration = CurrentPlugin.getPlugin().configuration();
        EncryptionConfiguration encryption = configuration.encryption();

        return !encryption.algorithm().equalsIgnoreCase("sha512") || result.product().valid() != encryption.virtualID();
    }

    /**
     * Verify a hash
     *
     * @param input  the input to verify with
     * @param result the hashed input
     * @return if the input is correct
     */
    @Override
    public boolean verify(final String input, final HashResult result) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginHasher hasher = plugin.hasher();

        VirtualizedInput virtualized = result.product();
        EncryptionConfiguration encryption = plugin.configuration().encryption();

        String token = new String(virtualized.product(), StandardCharsets.UTF_8);
        String password = input;
        if (virtualized.valid()) {
            try {
                VirtualizedInput clone = hasher.virtualID().virtualize(input, virtualized.refferences());
                password = new String(clone.product(), StandardCharsets.UTF_8);
            } catch (IllegalStateException i) {
                return false;
            }
        }

        if (isBase64(token)) {
            token = base64(token);
        }

        return auth(password, token);
    }
}
