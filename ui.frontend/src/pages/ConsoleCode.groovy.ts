export default `
boolean canRun() {
    return condition.always()
}

void doRun() {
    println "Processing..."

    println "Updating ACLs..."
    def acmeService = acl.createUser { id = "acme.service"; systemUser(); skipIfExists() }
    acl.forUser { authorizable = acmeService } {
        purge { }
        allow { path = "/content"; permissions = ["jcr:read", "jcr:write"] }
    }

    def johnDoe = acl.createUser { id = "john.doe"; fullName = "John Doe"; password = "ilovekittens"; skipIfExists() }
    johnDoe.with {
        purge()
        allow("/content", ["jcr:read"])
    }
    johnDoe.purge()
    johnDoe.purge()

    acl.createGroup { id = "test.group" }.with {
        removeAllMembers()
        addMember(acmeService)
        addMember(johnDoe)
    }

    println "Updating JCR resources..."
    def max = 10
    for (int i = 0; i < max; i++) {
        Thread.sleep(500)
        println "Updated (\${i + 1}/\${max})"
    }

    println "Processing done"
}
`.trim();
