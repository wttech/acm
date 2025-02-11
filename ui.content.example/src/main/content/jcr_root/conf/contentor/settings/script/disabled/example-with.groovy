boolean canRun() {
    return condition.always()
}

void doRun() {
    def experiences = [
            [
                    name: "gokk",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]]
                    ]
            ],
            [
                    name: "company",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]]
                    ]
            ],
            [
                    name: "ddffd",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]]
                    ]
            ],
            [
                    name: "guilty",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "at", languages: ["de"]],
                            [code: "be", languages: ["fr", "nl", "be"]],
                            [code: "dk", languages: ["da"]],
                            [code: "fi", languages: ["fi"]],
                            [code: "fr", languages: ["fr"]],
                            [code: "de", languages: ["de"]],
                            [code: "gb", languages: ["en"]],
                            [code: "ie", languages: ["en"]],
                            [code: "it", languages: ["it"]],
                            [code: "nl", languages: ["nl"]],
                            [code: "pt", languages: ["pt"]],
                            [code: "ru", languages: ["ru"]],
                            [code: "es", languages: ["es"]],
                            [code: "se", languages: ["sv"]],
                            [code: "ch", languages: ["fr", "de", "ch"]]
                    ]
            ],
            [
                    name: "noexp",
                    markets: [
                            [code: "al", languages: ["sq"]],
                            [code: "am", languages: ["hy"]],
                            [code: "ar", languages: ["es"]],
                            [code: "at", languages: ["de"]],
                            [code: "au", languages: ["en"]],
                            [code: "aw", languages: ["nl"]],
                            [code: "az", languages: ["az"]],
                            [code: "ba", languages: ["bs"]],
                            [code: "bb", languages: ["en"]],
                            [code: "bd", languages: ["bn", "en"]],
                            [code: "be", languages: ["fr", "nl"]],
                            [code: "bg", languages: ["bg"]],
                            [code: "bo", languages: ["es"]],
                            [code: "br", languages: ["pt"]],
                            [code: "bs", languages: ["en"]],
                            [code: "bt", languages: ["dz", "en"]],
                            [code: "by", languages: ["be"]],
                            [code: "bz", languages: ["en"]],
                            [code: "ca", languages: ["en", "fr"]],
                            [code: "cb", languages: ["en"]],
                            [code: "ch", languages: ["de", "fr"]],
                            [code: "cl", languages: ["es"]],
                            [code: "cn", languages: ["zh"]],
                            [code: "co", languages: ["es"]],
                            [code: "cr", languages: ["es"]],
                            [code: "cy", languages: ["el"]],
                            [code: "cz", languages: ["cs"]],
                            [code: "de", languages: ["de"]],
                            [code: "dk", languages: ["da"]],
                            [code: "do", languages: ["es"]],
                            [code: "dz", languages: ["ar", "fr"]],
                            [code: "ec", languages: ["es"]],
                            [code: "ee", languages: ["et"]],
                            [code: "eg", languages: ["ar", "en"]],
                            [code: "es", languages: ["es"]],
                            [code: "eu", languages: ["en"]],
                            [code: "fi", languages: ["fi"]],
                            [code: "fj", languages: ["en"]],
                            [code: "fr", languages: ["fr"]],
                            [code: "gb", languages: ["en"]],
                            [code: "gd", languages: ["en"]],
                            [code: "ge", languages: ["ka"]],
                            [code: "gr", languages: ["el"]],
                            [code: "gt", languages: ["es"]],
                            [code: "hk", languages: ["en", "zh"]],
                            [code: "hn", languages: ["es"]],
                            [code: "hr", languages: ["hr"]],
                            [code: "hu", languages: ["hu"]],
                            [code: "id", languages: ["en", "id"]],
                            [code: "ie", languages: ["en"]],
                            [code: "in", languages: ["en", "hi"]],
                            [code: "is", languages: ["is"]],
                            [code: "it", languages: ["it"]],
                            [code: "jm", languages: ["en"]],
                            [code: "jp", languages: ["ja"]],
                            [code: "ke", languages: ["en"]],
                            [code: "kg", languages: ["ky", "ru"]],
                            [code: "kh", languages: ["en","kh"]],
                            [code: "kr", languages: ["ko"]],
                            [code: "kz", languages: ["kk", "ru"]],
                            [code: "lc", languages: ["en"]],
                            [code: "lk", languages: ["en", "si"]],
                            [code: "lt", languages: ["lt"]],
                            [code: "lv", languages: ["lv"]],
                            [code: "ma", languages: ["ar", "fr"]],
                            [code: "md", languages: ["ro"]],
                            [code: "me", languages: ["me"]],
                            [code: "mk", languages: ["mk"]],
                            [code: "mm", languages: ["my", "en"]],
                            [code: "mt", languages: ["en"]],
                            [code: "mv", languages: ["dv", "en"]],
                            [code: "mx", languages: ["es"]],
                            [code: "my", languages: ["en", "ms"]],
                            [code: "ng", languages: ["en"]],
                            [code: "nl", languages: ["nl"]],
                            [code: "no", languages: ["no"]],
                            [code: "np", languages: ["en", "ne"]],
                            [code: "nz", languages: ["en"]],
                            [code: "pa", languages: ["es"]],
                            [code: "pe", languages: ["es"]],
                            [code: "pf", languages: ["fr"]],
                            [code: "pg", languages: ["en"]],
                            [code: "ph", languages: ["en"]],
                            [code: "pk", languages: ["en", "ur"]],
                            [code: "pl", languages: ["pl"]],
                            [code: "pt", languages: ["pt"]],
                            [code: "py", languages: ["es"]],
                            [code: "ro", languages: ["ro"]],
                            [code: "rs", languages: ["sr"]],
                            [code: "ru", languages: ["ru"]],
                            [code: "se", languages: ["sv"]],
                            [code: "sg", languages: ["en"]],
                            [code: "si", languages: ["sl"]],
                            [code: "sk", languages: ["sk"]],
                            [code: "sv", languages: ["en"]],
                            [code: "sv", languages: ["es"]],
                            [code: "th", languages: ["en", "th"]],
                            [code: "tj", languages: ["ru", "tg"]],
                            [code: "tr", languages: ["tr"]],
                            [code: "tt", languages: ["en"]],
                            [code: "tw", languages: ["zh"]],
                            [code: "ua", languages: ["uk"]],
                            [code: "us", languages: ["en", "es"]],
                            [code: "uy", languages: ["es"]],
                            [code: "uz", languages: ["ru", "uz"]],
                            [code: "vn", languages: ["en", "vi"]],
                            [code: "xa", languages: ["fr"]],
                            [code: "xb", languages: ["pt"]],
                            [code: "xc", languages: ["sw"]],
                            [code: "xd", languages: ["am"]],
                            [code: "xe", languages: ["en"]],
                            [code: "xf", languages: ["ar", "en"]],
                            [code: "xg", languages: ["en"]],
                            [code: "xh", languages: ["es"]],
                            [code: "xk", languages: ["sr", "sq"]],
                            [code: "xs", languages: ["en"]],
                            [code: "xx", languages: ["en"]],
                            [code: "za", languages: ["en"]]
                    ]
            ],
            [
                    name: "mynuke",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]]
                    ]
            ],
            [
                    name: "electrolyte",
                    markets: [
                            [code: "ca", languages: ["en", "fr"]],
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]]
                    ]
            ],
            [
                    name: "optic",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]],
                            [code: "ca", languages: ["en", "fr"]]
                    ]
            ],
            [
                    name: "dyson",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]]
                    ]
            ],
            [
                    name: "nna",
                    markets: [
                            [code: "xx", languages: ["en"]],
                            [code: "us", languages: ["en"]]
                    ]
            ]
    ]

    ["guilty", "company", "gokk", "ddffd", "electrolyte", "optic", "dyson", "nna"].each { experience ->
        acl.clear { id = "xyz-$experience-global-content-authors"; path = "/" }
        acl.deleteGroup { id = "xyz-$experience-global-content-authors" }
    }

    experiences.each { experience ->
        acl.clear { id = "xyz-${experience.name}-all-template-authors"; path = "/content" }
    }

    acl.createGroup { id = "xyz-all-users-base" }
    acl.createGroup { id = "xyz-developers-base" }

    acl.createGroup { id = "xyz-forms-users" }.with {
        clear { path = "/conf" }
        clear { path = "/content" }
        clear { path = "/etc" }
        clear { path = "/tmp" }
        removeFromAllGroups()
        removeAllMembers()
        addToGroup { groupId = "workflow-users" }
        addToGroup { groupId = "tag-administrators" }
        addToGroup { groupId = "contributor" }
        addToGroup { groupId = "analytics-administrators" }
        allow { path = "/conf"; permissions = ["jcr:read"]; glob = "/*/dam/*" }
        allow { path = "/conf"; permissions = ["jcr:read", "rep:write"]; glob = "/*/forms/output/batch/*" }
        allow { path = "/conf/forms"; permissions = ["jcr:read", "crx:replicate", "rep:write"]; glob = "/*/sling:configs*" }
        allow { path = "/content/dam/formsanddocuments/xyz"; permissions = ["jcr:read"] }
        allow { path = "/content/dam/formsanddocuments-fdm"; permissions = ["jcr:read", "crx:replicate"] }
        allow { path = "/content/dam/formsanddocuments-themes/reference-themes"; permissions = ["crx:replicate"] }
        allow { path = "/etc/clientlibs/fd/themes"; permissions = ["jcr:read", "jcr:modifyProperties", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
        allow { path = "/etc/clientlibs/reference-themes"; permissions = ["crx:replicate"] }
        deny { path = "/etc/packages"; permissions = ["jcr:read"] }
        allow { path = "/etc/packages/fd/export"; permissions = ["jcr:read", "rep:write"] }
        deny { path = "/etc/packages/fd/export"; permissions = ["jcr:addChildNodes"]; glob = "*js.txt" }
        allow { path = "/tmp/fd/fm/upload"; permissions = ["jcr:read", "rep:write"] }
        allow { path = "/libs/fd/af/dor/templates/defaultTemplate.xdp"; permissions = ["jcr:read"] }
    }

    acl.createGroup { id = "xyz-forms-pu" }.with {
        removeFromAllGroups()
        removeAllMembers()
        addMember { memberId = "ims-xyz-forms-pu" }
        addToGroup { groupId = "xyz-forms-users" }
        addToGroup { groupId = "fdm-authors" }
        addToGroup { groupId = "forms-xfa-writers" }
        addToGroup { groupId = "forms-script-writers" }
    }

    acl.createGroup { id = "xyz-all-users" }.with {
        clear { path = "/conf" }
        clear { path = "/content" }
        clear { path = "/etc" }
        clear { path = "/var" }
        removeFromAllGroups()
        removeAllMembers()
        addToGroup { groupId = "xyz-all-users-base" }
        allow { path = "/conf"; permissions = ["crx:replicate"]; glob = "/*/cloudconfigs" }
        allow { path = "/conf"; permissions = ["crx:replicate"]; glob = "/*/cloudconfigs/*" }
        allow { path = "/conf/xyz/settings/wcm"; permissions = ["crx:replicate"] }
        allow { path = "/conf/global/settings/workflow"; permissions = ["jcr:read"] }
        deny { path = "/content"; permissions = ["rep:write"]; types = ["cq:meta"] }
        allow { path = "/content"; permissions = ["jcr:read"]; types = ["cq:meta"] }
        deny { path = "/content"; permissions = ["rep:write"]; glob = "/*/cq:target-ambits" }
        allow { path = "/content"; permissions = ["jcr:versionManagement"] }
        allow { path = "/content/dam/xyz/images"; permissions = ["crx:replicate"] }
        allow { path = "/content/dam/content-fragments"; permissions = ["jcr:write", "crx:replicate"] }
        allow { path = "/etc/clientlibs/fd/themes"; permissions = ["crx:replicate"] }
        allow { path = "/etc/cloudservices/dmscene7"; permissions = ["jcr:read"] }
        allow { path = "/etc/cloudservices/dynamicmediaservices"; permissions = ["jcr:read"] }
        allow { path = "/etc/cloudservices/scene7"; permissions = ["jcr:read"] }
        allow { path = "/etc/importers/bulkeditor"; permissions = ["jcr:read"] }
        allow { path = "/etc/importers/offline"; permissions = ["jcr:read"] }
        allow { path = "/etc/reports/auditreport"; permissions = ["jcr:read", "rep:write"] }
        allow { path = "/etc/reports/compreport"; permissions = ["jcr:read", "rep:write"] }
        allow { path = "/etc/segmentation"; permissions = ["rep:write"] }
        allow { path = "/etc/workflow"; permissions = ["jcr:read"] }
        allow { path = "/etc/workflow/packages"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
        allow { path = "/var/workflow"; permissions = ["jcr:read"] }
        allow { path = "/var/workflow/instances"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
        allow { path = "/var/workflow/packages"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
        allow { path = "/content/dam/formsanddocuments/xyz"; permissions = ["jcr:read"] }

        experiences.each { experience ->
            deny { path = "/content/${experience.name}"; permissions = ["jcr:read"] }
            deny { path = "/content/dam/${experience.name}"; permissions = ["jcr:read"] }
            deny { path = "/content/dam/formsanddocuments/${experience.name}"; permissions = ["jcr:read"] }
            deny { path = "/content/dam/formsanddocuments-themes/${experience.name}"; permissions = ["jcr:read"] }
            deny { path = "/content/dam/formsanddocuments-fdm/${experience.name}"; permissions = ["jcr:read"] }
        }
    }

    acl.createGroup { id = "xyz-developers" }.with {
        clear { path = "/content" }
        clear { path = "/conf" }
        clear { path = "/etc" }
        clear { path = "/oak:index" }
        clear { path = "/system" }
        clear { path = "/tmp" }
        clear { path = "/var" }
        clear { path = "/home" }
        removeFromAllGroups()
        removeAllMembers()
        addMember { memberId = "ims-xyz-developers" }
        addToGroup { groupId = "xyz-developers-base" }
        allow { path = "/content"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement", "crx:replicate"] }
        allow { path = "/content/experience-fragments"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement", "crx:replicate"] }
        allow { path = "/conf"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement", "crx:replicate"] }
        allow { path = "/etc"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement", "crx:replicate"] }
        allow { path = "/oak:index"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement"] }
        allow { path = "/system"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement"] }
        allow { path = "/tmp"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement"] }
        allow { path = "/var"; permissions = ["jcr:read", "rep:write", "jcr:readAccessControl", "jcr:lockManagement", "jcr:versionManagement", "jcr:retentionManagement", "jcr:lifecycleManagement"] }
        allow { path = "/home"; permissions = ["jcr:read", "jcr:readAccessControl"] }
        allow { path = "/home/users/system"; permissions = ["jcr:read", "jcr:readAccessControl"] }
    }

    acl.createGroup { id = "xyz-script-executors" }.with {
        removeFromAllGroups()
        removeAllMembers()
        addMember { memberId = "ims-xyz-script-executors" }
    }

    acl.createGroup { id = "xyz-all-content-authors" }.with {
        removeFromAllGroups()
        removeAllMembers()
        addMember { memberId = "ims-xyz-all-content-authors" }
    }

    experiences.each { experience ->
        acl.createGroup { id = "xyz-${experience.name}-all-content-authors" }.with {
            removeFromAllGroups()
            removeAllMembers()
            addMember { memberId = "ims-xyz-${experience.name}-all-content-authors" }
            addMember { memberId = "xyz-all-content-authors" }
        }

        acl.createGroup { id = "xyz-${experience.name}-all-template-authors" }.with {
            removeAllMembers()
            removeFromAllGroups()
            addMember { memberId = "ims-xyz-${experience.name}-all-template-authors" }
        }

        acl.createGroup { id = "xyz-${experience.name}-forms" }.with {
            clear { path = "/content" }
            removeFromAllGroups()
            removeAllMembers()
            addMember { memberId = "ims-xyz-${experience.name}-forms" }
            addToGroup { groupId = "xyz-all-users" }
            allow { path = "/content/dam/formsanddocuments/xyz"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/formsanddocuments/${experience.name}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
            allow { path = "/content/dam/formsanddocuments-themes/${experience.name}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
            allow { path = "/content/forms/af/${experience.name}"; permissions = ["jcr:read", "rep:write", "crx:replicate", "jcr:lockManagement", "jcr:versionManagement"] }
        }

        acl.createGroup { id = "xyz-${experience.name}-ca-base" }.with {
            clear { path = "/conf" }
            clear { path = "/content" }
            clear { path = "/etc" }
            removeFromAllGroups()
            removeAllMembers()
            addToGroup { groupId = "xyz-all-users" }
            allow { path = "/conf/${experience.name}"; permissions = ["jcr:read", "crx:replicate"] }
            allow { path = "/etc/cloudservices"; permissions = ["jcr:read"] }
            allow { path = "/content/${experience.name}"; permissions = ["jcr:read"]; glob = "" }
            allow { path = "/content/${experience.name}/jcr:content"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/${experience.name}"; permissions = ["jcr:read"]; glob = "" }
            allow { path = "/content/dam/${experience.name}/jcr:content"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/${experience.name}/global"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/${experience.name}/shared"; permissions = ["jcr:read", "rep:write"] }
            allow { path = "/content/cq:tags"; permissions = ["jcr:read"] }
            allow { path = "/content/cq:tags/${experience.name}/shared"; permissions = ["jcr:read", "crx:replicate"] }
            allow { path = "/content/experience-fragments/${experience.name}"; permissions = ["rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            allow { path = "/content/dam/formsanddocuments/xyz"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/formsanddocuments/${experience.name}"; permissions = ["jcr:read"]; glob = "" }
            allow { path = "/content/dam/formsanddocuments/jcr:content"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/formsanddocuments-themes/${experience.name}"; permissions = ["jcr:read"] }
            allow { path = "/content/${experience.name}/zz"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/${experience.name}/zz"; permissions = ["jcr:read"] }
            deny { path = "/content/experience-fragments/${experience.name}/zz"; permissions = ["rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            allow { path = "/content/${experience.name}/xx"; permissions = ["jcr:read", "rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            deny { path = "/content/${experience.name}/xx"; permissions = ["jcr:removeNode"]; glob = "" }
            allow { path = "/content/dam/${experience.name}/xx"; permissions = ["jcr:read", "rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            deny { path = "/content/dam/${experience.name}/xx"; permissions = ["jcr:removeNode"]; glob = "" }
            allow { path = "/content/cq:tags/${experience.name}/xx"; permissions = ["jcr:read", "rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            deny { path = "/content/cq:tags/${experience.name}/xx"; permissions = ["jcr:removeNode"]; glob = "" }
        }

        acl.createGroup { id = "xyz-${experience.name}-ta-base" }.with {
            clear { path = "/conf" }
            clear { path = "/content" }
            clear { path = "/etc" }
            removeFromAllGroups()
            removeAllMembers()
            addToGroup { groupId = "xyz-all-users" }
            allow { path = "/conf/xyz"; permissions = ["jcr:read"] }
            allow { path = "/conf/${experience.name}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
            allow { path = "/etc/cloudservices"; permissions = ["jcr:read", "crx:replicate"] }
            allow { path = "/content/xyz"; permissions = ["jcr:read"] }
            allow { path = "/content/${experience.name}"; permissions = ["jcr:read"]; glob = "" }
            allow { path = "/content/${experience.name}/jcr:content"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/${experience.name}"; permissions = ["jcr:read"]; glob = "" }
            allow { path = "/content/dam/${experience.name}/jcr:content"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/${experience.name}/global"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
            allow { path = "/content/dam/${experience.name}/shared"; permissions = ["jcr:read", "rep:write"] }
            allow { path = "/content/cq:tags/${experience.name}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
            allow { path = "/content/experience-fragments/${experience.name}"; permissions = ["rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            allow { path = "/content/dam/formsanddocuments/xyz"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/formsanddocuments/${experience.name}"; permissions = ["jcr:read"]; glob = "" }
            allow { path = "/content/dam/formsanddocuments/jcr:content"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/formsanddocuments-themes/${experience.name}"; permissions = ["jcr:read"] }
            allow { path = "/content/${experience.name}/zz"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/${experience.name}/zz"; permissions = ["jcr:read"] }
            deny { path = "/content/experience-fragments/${experience.name}/zz"; permissions = ["rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            allow { path = "/content/${experience.name}/xx"; permissions = ["jcr:read", "rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            deny { path = "/content/${experience.name}/xx"; permissions = ["jcr:removeNode"]; glob = "" }
            allow { path = "/content/dam/${experience.name}/xx"; permissions = ["jcr:read", "rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
            deny { path = "/content/dam/${experience.name}/xx"; permissions = ["jcr:removeNode"]; glob = "" }
        }

        acl.createGroup { id = "xyz-${experience.name}-fe-base" }.with {
            clear { path = "/content" }
            removeFromAllGroups()
            removeAllMembers()
            addToGroup { groupId = "xyz-forms-pu" }
            addToGroup { groupId = "xyz-all-users" }
            allow { path = "/content/dam/formsanddocuments/xyz"; permissions = ["jcr:read"] }
            allow { path = "/content/dam/formsanddocuments/${experience.name}"; permissions = ["jcr:read", "rep:write", "crx:replicate"]; glob = "" }
            allow { path = "/content/dam/formsanddocuments/${experience.name}/jcr:content"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
            allow { path = "/content/dam/formsanddocuments/jcr:content"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
            allow { path = "/content/dam/formsanddocuments-themes/${experience.name}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
        }
    }

    acl.allow { id = "xyz-company-ca-base"; path = "/conf/noexp"; permissions = ["jcr:read"] }

    acl.allow { id = "xyz-company-ta-base"; path = "/conf/noexp"; permissions = ["jcr:read"] }

    experiences.each { experience ->
        experience.markets.each { market ->
            acl.createGroup { id = "xyz-${experience.name}-ca-${market.code}" }.with {
                clear { path = "/content" }
                removeFromAllGroups()
                removeAllMembers()
                addMember { memberId = "ims-xyz-${experience.name}-ca-${market.code}" }
                addMember { memberId = "xyz-${experience.name}-all-content-authors" }
                addToGroup { groupId = "xyz-${experience.name}-ca-base" }
                allow { path = "/content/${experience.name}/${market.code}"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
                allow { path = "/content/${experience.name}/${market.code}"; permissions = ["jcr:read", "jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "*/jcr:content*" }
                allow { path = "/content/dam/${experience.name}/${market.code}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
                allow { path = "/content/cq:tags/${experience.name}/${market.code}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
                allow { path = "/content/dam/formsanddocuments/${experience.name}/${market.code}"; permissions = ["jcr:read"] }
                allow { path = "/content/forms/af/${experience.name}/${market.code}"; permissions = ["jcr:read"] }
                market.languages.each { language ->
                    allow { path = "/content/${experience.name}/${market.code}/${language}"; permissions = ["jcr:read", "rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
                    allow { path = "/content/${experience.name}/${market.code}/${language}"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "*/jcr:content*" }
                    deny { path = "/content/${experience.name}/${market.code}/${language}"; permissions = ["jcr:removeNode"]; glob = "" }
                    deny { path = "/content/dam/${experience.name}/${market.code}/${language}/forms"; permissions = ["rep:write", "crx:replicate"] }
                    deny { path = "/content/${experience.name}/${market.code}/${language}/archive"; permissions = ["crx:replicate"] }
                }
            }

            acl.createGroup { id = "xyz-${experience.name}-ta-${market.code}" }.with {
                clear { path = "/content" }
                removeFromAllGroups()
                removeAllMembers()
                addMember { memberId = "ims-xyz-${experience.name}-ta-${market.code}" }
                addMember { memberId = "xyz-${experience.name}-all-template-authors" }
                addToGroup { groupId = "xyz-${experience.name}-ta-base" }
                allow { path = "/content/${experience.name}/${market.code}"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
                allow { path = "/content/${experience.name}/${market.code}"; permissions = ["jcr:read", "jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "*/jcr:content*" }
                allow { path = "/content/dam/${experience.name}/${market.code}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
                allow { path = "/content/dam/formsanddocuments/${experience.name}/${market.code}"; permissions = ["jcr:read"] }
                allow { path = "/content/forms/af/${experience.name}/${market.code}"; permissions = ["jcr:read"] }
                market.languages.each { language ->
                    allow { path = "/content/${experience.name}/${market.code}/${language}"; permissions = ["jcr:read", "rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
                    allow { path = "/content/${experience.name}/${market.code}/${language}"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "*/jcr:content*" }
                    deny { path = "/content/${experience.name}/${market.code}/${language}"; permissions = ["jcr:removeNode"]; glob = "" }
                    deny { path = "/content/${experience.name}/${market.code}/${language}/archive"; permissions = ["crx:replicate"] }
                }
            }

            acl.createGroup { id = "xyz-${experience.name}-fe-${market.code}" }.with {
                clear { path = "/content" }
                removeFromAllGroups()
                removeAllMembers()
                addMember { memberId = "ims-xyz-${experience.name}-fe-${market.code}" }
                addToGroup { groupId = "xyz-${experience.name}-fe-base" }
                allow { path = "/content/dam/formsanddocuments/${experience.name}/${market.code}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
                allow { path = "/content/forms/af/${experience.name}/${market.code}"; permissions = ["jcr:read", "rep:write", "crx:replicate", "jcr:lockManagement", "jcr:versionManagement"] }
            }
        }
    }

    experiences[4].markets.each { market ->
        acl.createGroup { id = "xyz-noexp-ma-${market.code}" }.with {
            clear { path = "/content" }
            clear { path = "/conf" }
            removeFromAllGroups()
            removeAllMembers()
            addMember { memberId = "ims-xyz-noexp-ma-${market.code}" }
            addToGroup { groupId = "xyz-noexp-ca-${market.code}" }
            market.languages.each { language ->
                allow { path = "/conf/noexp/${market.code}/${language}"; permissions = ["jcr:read", "rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
                allow { path = "/content/dam/noexp/${market.code}/${language}/forms"; permissions = ["rep:write", "crx:replicate"] }
                deny { path = "/conf/noexp/${market.code}/${language}"; permissions = ["rep:write"]; glob = "*com.acme.config*" }
                allow { path = "/conf/noexp/${market.code}/${language}"; permissions = ["rep:write"]; glob = "*com.acme.config.GateConfig*" }
                allow { path = "/conf/noexp/${market.code}/${language}"; permissions = ["rep:write"]; glob = "*com.acme.config.AuthConfig*" }
                allow { path = "/conf/noexp/${market.code}/${language}"; permissions = ["rep:write"]; glob = "*com.acme.config.PriceConfig*" }
                allow { path = "/conf/noexp/${market.code}/${language}"; permissions = ["rep:write"]; glob = "*com.acme.config.PublicationConfig*" }
                allow { path = "/conf/noexp/${market.code}/${language}"; permissions = ["rep:write"]; glob = "*com.acme.config.SdkConfig*" }
                allow { path = "/conf/noexp/${market.code}/${language}"; permissions = ["rep:write"]; glob = "*com.acme.config.NotifyConfig*" }
            }
        }
    }

    acl.createGroup { id = "xyz-noexp-global-content-authors" }.with {
        clear { path = "/content" }
        clear { path = "/conf" }
        removeFromAllGroups()
        removeAllMembers()
        addMember { memberId = "ims-xyz-noexp-global-content-authors" }
        allow { path = "/content/noexp/configuration"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/conf/noexp/sling:configs"; permissions = ["rep:write", "crx:replicate"]; glob = "*com.acme.config.DictionaryConfig*" }
        allow { path = "/conf/noexp/sling:configs"; permissions = ["rep:write", "crx:replicate"]; glob = "*com.acme.config.BrandsAndProductsConfig*" }
        allow { path = "/conf/noexp/sling:configs"; permissions = ["rep:write", "crx:replicate"]; glob = "*com.acme.config.CampaignConfig*" }
        allow { path = "/content/noexp/country-selector"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/content/noexp/country-selector"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "/jcr:content" }
        allow { path = "/content/noexp/404"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/content/noexp/404"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "/jcr:content" }
        allow { path = "/content/noexp/500"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/content/noexp/500"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "/jcr:content" }
        allow { path = "/content/noexp/terms-of-service"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/content/noexp/terms-of-service"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "/jcr:content" }
        allow { path = "/content/noexp/privacy-policy"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/content/noexp/privacy-policy"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "/jcr:content" }
        allow { path = "/content/noexp/dsr"; permissions = ["jcr:read", "jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/content/noexp/dsr"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "/jcr:content" }
        allow { path = "/content/dam/noexp/root"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
        allow { path = "/content/noexp/zz/en/offerings"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/offerings"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/dam/noexp/global/central/offerings"; permissions = ["rep:write", "crx:replicate"] }
        deny { path = "/content/dam/noexp/global/central/offerings"; permissions = ["jcr:removeNode"]; glob = ""  }
        allow { path = "/content/experience-fragments/noexp/zz"; permissions = ["rep:write", "jcr:lockManagement", "jcr:versionManagement", "crx:replicate"] }
        deny { path = "/content/experience-fragments/noexp/zz"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/noexp/zz/en/social"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/social"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/dam/noexp/zz/en/social-impact"; permissions = ["rep:write", "crx:replicate"] }
        deny { path = "/content/dam/noexp/zz/en/social-impact"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/noexp/zz/en/sustainability"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/sustainability"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/dam/noexp/zz/en/sustainability"; permissions = ["rep:write", "crx:replicate"] }
        deny { path = "/content/dam/noexp/zz/en/sustainability"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/noexp/zz/en/demo/release-notes"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/demo/release-notes"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/dam/noexp/zz/en/demo/release-notes"; permissions = ["rep:write", "crx:replicate"] }
        deny { path = "/content/dam/noexp/zz/en/demo/release-notes"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/noexp/zz/en/demo/noexp-support"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/demo/noexp-support"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/dam/noexp/zz/en/demo/noexp-support"; permissions = ["rep:write", "crx:replicate"] }
        deny { path = "/content/dam/noexp/zz/en/demo/noexp-support"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/noexp/zz/en/demo"; permissions = ["jcr:versionManagement", "jcr:modifyProperties", "jcr:lockManagement", "crx:replicate"] }
        allow { path = "/content/noexp/zz/en/demo"; permissions = ["jcr:removeChildNodes", "jcr:removeNode", "jcr:addChildNodes", "jcr:nodeTypeManagement"]; glob = "/jcr:content" }
        allow { path = "/content/noexp/zz/en/demo/knowledge"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/demo/knowledge"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/noexp/zz/en/demo/support"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/demo/support"; permissions = ["jcr:removeNode"]; glob = "" }
        allow { path = "/content/noexp/zz/en/demo/authoring"; permissions = ["rep:write", "jcr:versionManagement", "jcr:lockManagement", "crx:replicate"] }
        deny { path = "/content/noexp/zz/en/demo/authoring"; permissions = ["jcr:removeNode"]; glob = "" }
    }

    acl.createUser { id = "xyz-cf-replicator"; systemUser() }.with {
        clear { path = "/content" }
        allow { path = "/content/dam"; permissions = ["jcr:read", "crx:replicate"] }
    }

    acl.createUser { id = "xyz-distribution-job-user"; systemUser() }

    acl.createGroup { id = "test-xyz-noexp-dam-authors" }.with {
        clear { path = "/content" }
        removeFromAllGroups()
        removeAllMembers()
        addToGroup { groupId = "xyz-noexp-ca-base" }
        allow { path = "/content/dam/noexp/global"; permissions = ["jcr:read", "rep:write"] }
    }
}
