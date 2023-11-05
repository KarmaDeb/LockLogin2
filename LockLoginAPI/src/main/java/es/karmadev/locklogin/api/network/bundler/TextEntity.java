package es.karmadev.locklogin.api.network.bundler;

import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.TextContainer;
import lombok.Getter;

@Getter
public class TextEntity<T extends TextContainer & NetworkEntity> {

    private final T component;

    private TextEntity(final T component) {
        this.component = component;
    }

    public static <A extends TextContainer & NetworkEntity> TextEntity<A> singleton(final A component) {
        return new TextEntity<>(component);
    }
}
