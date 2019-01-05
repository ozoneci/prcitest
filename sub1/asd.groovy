node {

    docker.image('elek/ozone-build').inside {


        stage('Clean') {
            status = sh returnStatus: true, script: 'mvn clean'
        }

        stageRunner('Author', "author")

        stage('Build') {
            prStatusStart("build")
            status = sh returnStatus: true, script: 'mvn clean install -DskipTests'
            prStatusResult(status, "build")
        }

        stageRunner('Licence', "rat")

        stageRunner('Unit test', "unit")

        stageRunner('Findbugs', "findbugs")

        stageRunner('Checkstyle', "checkstyle")

    }

}

def stageRunner(name, type) {
    try {
        stage(name) {

            prStatusStart(type)
            status = sh returnStatus: true, script: 'sub1/dev-support/checks/' + type + '.sh'
            prStatusResult(status, type)
        }
        return true
    } catch (RuntimeException ex) {
        currentBuild.result = "FAILED"
        return false
    }
}

def prStatusStart(name) {
    if (env.CHANGE_ID) {
        pullRequest.createStatus(status: "pending",
                context: 'continuous-integration/jenkins/pr-merge/' + name,
                description: name + " is started")
    }
}

def prStatusResult(responseCode, name) {
    status = "error"
    desc = "failed"
    if (responseCode == 0) {
        status = "success"
        desc = "passed"
    }
    message = name + " is " + desc
    //System.out.println(responseCode)
    if (env.CHANGE_ID) {
        pullRequest.createStatus(status: status,
                context: 'continuous-integration/jenkins/pr-merge/' + name,
                description: message)
    }
    if (responseCode != 0) {
        throw new RuntimeException(message)
    }
}
