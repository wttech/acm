export default `
boolean canRun() {
    return condition.always()
}

void doRun() {
    println "Processing..."
    
    println "Updating ACLs..."
    def acmeService = acl.createUser { id = "acme.service"; systemUser(); skipIfExists() }
    acl.allow(acmeService, "/content", ["jcr:read", "jcr:write"])
    
    def johnDoe = acl.createUser { id = "john.doe"; fullName = "John Doe"; password = "ilovekittens"; skipIfExists() }
    acl.purge { id = "john.doe" }
    acl.allow(johnDoe, "/content", ["jcr:read"])
    
    println "Updating JCR resources..."
    def max = 10
    for (int i = 0; i < max; i++) {
        Thread.sleep(500)
        println "Updated (\${i + 1}/\${max})"
    }
    
    println "Processing done"
}
`.trim();
