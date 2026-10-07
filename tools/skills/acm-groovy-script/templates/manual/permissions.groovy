/*
---
version: '1.0'
category: security
tags: ['acl']
---
Creates the ACME service user and authors group with their permissions. Run on demand; safe to repeat.
*/

boolean canRun() {
    return conditions.always()
}

void doRun() {
    log.info "ACL setup started"

    def service = acl.createUser { id = 'acme.service'; systemUser(); skipIfExists() }
    service.allow { path = '/content/acme'; permissions = ['jcr:read', 'rep:write'] }
    service.allow { path = '/content/dam/acme'; permissions = ['jcr:read'] }

    def authors = acl.createGroup { id = 'acme-authors'; skipIfExists() }
    authors.allow { path = '/content/acme'; permissions = ['jcr:read', 'rep:write', 'crx:replicate'] }
    authors.deny { path = '/content/acme/admin'; permissions = ['rep:write'] }
    authors.addMember(service)

    log.info "ACL setup finished"
}
