package es.karmadev.locklogin.spigot.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransientPermissionData<GNode, PNode> {

    private final boolean op;
    private final GNode[] groups;
    private final PNode[] permissions;
}