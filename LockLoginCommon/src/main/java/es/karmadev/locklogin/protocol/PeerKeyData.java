package es.karmadev.locklogin.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.crypto.SecretKey;

@AllArgsConstructor @Getter
public class PeerKeyData {

    private final SecretKey key;
    private final String algorithm;
}
