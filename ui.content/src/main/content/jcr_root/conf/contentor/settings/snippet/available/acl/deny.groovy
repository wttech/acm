acl.deny { authorizable = "${1:authorizable}"; path = "${2:path}"; glob="/*" permissions = [${3:permissions}]; restrictions = [:]; skipPathMissing() }
