package es.karmadev.locklogin.common.dependency;

import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

public final class DependencyWrapper {

    public final static LockLoginDependency[] dependencies = new LockLoginDependency[]{
            new ApacheCommons(),
            new GoogleAuth(),
            new GoogleGuava(),
            new Log4j(),
            new Log4jWeb(),
            new SocketIO()
    }.clone();
}
