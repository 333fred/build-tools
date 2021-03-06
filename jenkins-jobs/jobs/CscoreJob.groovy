def basePath = 'CSCore'
folder(basePath)

def prJob = job("$basePath/CSCore - PR") {
    scm {
        git {
            remote {
                url('https://github.com/wpilibsuite/cscore.git')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }
            // This is purposefully not a GString. This is a jenkins environment
            // variable, not a groovy variable
            branch('${sha1}')
        }
    }
    triggers {
        githubPullRequest {
            admins(['333fred', 'PeterJohnson', 'bradamiller', 'Kevin-OConnor'])
            orgWhitelist('wpilibsuite')
            useGitHubHooks()
        }
    }
}

setupProperties(prJob)
setupBuildSteps(prJob, false)

def developmentJob = job("$basePath/CSCore - Development") {
    triggers {
        scm('H/15 * * * *')
    }
    publishers {
        downstream('Eclipse Plugins/Eclipse Plugins - Development')
    }
}

setupGit(developmentJob)
setupProperties(developmentJob)
setupBuildSteps(developmentJob, true)

def releaseJob = job("$basePath/CSCore - Release")

setupGit(releaseJob)
setupProperties(releaseJob)
setupBuildSteps(releaseJob, true, ['releaseType=OFFICIAL'])

def setupGit(job) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/cscore.git')
                    branch('*/master')
                }
            }
        }
    }
}

def setupProperties(job) {
    job.with {
        // Note: The pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/cscore')
        }
    }
}

def setupBuildSteps(job, usePublish, properties = null) {
    job.with {
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                if (usePublish) tasks('publish')
                switches('-PjenkinsBuild')
                if (properties != null) {
                    properties.each { prop ->
                        switches("-P$prop")
                    }
                }
            }
        }
        if (usePublish) {
            publishers {
                archiveArtifacts {
                    pattern('**/build/cscore*.zip')
                    pattern('**/build/libs/cscore*.jar')
                    onlyIfSuccessful()
                }
            }
        }
    }
}

