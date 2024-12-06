acl.createUser { id = "${1:id}"; fullName = "${2:fullName}"; password = "${3:password}"; property("email", "${4:email}"); skipIfExists() }
