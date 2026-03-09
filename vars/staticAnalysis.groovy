def call(Boolean abortPipeline = false, String branchName = null) {

    echo "Inicio del análisis estático de código"

    // Leer variable de entorno que también puede forzar el abort (ej: ABORT_ON_QG=true)
    def envAbort = (env.ABORT_ON_QG ?: 'false').toString().toLowerCase() == 'true'
    // Determina si debemos abortar siempre: parámetro OR variable de entorno
    def alwaysAbort = abortPipeline || envAbort

    // Determinar nombre de la rama (parámetro -> env.BRANCH_NAME -> env.GIT_BRANCH -> 'unknown')
    def rawBranch = branchName ?: (env.BRANCH_NAME ?: (env.GIT_BRANCH ?: 'unknown'))
    // Normalizar nombres: quitar prefijos como origin/ o refs/heads/
    def branch = rawBranch.replaceAll(/^refs\\/heads\\//, '').replaceAll(/^origin\\//, '')

    withSonarQubeEnv('Sonar Local') {

        sh 'echo "Ejecución de las pruebas de calidad de código"'

        timeout(time: 5, unit: 'MINUTES') {
            echo "Esperando resultado del Quality Gate..."
            // MOCK del resultado (en la práctica sin Sonar real). Cambia a 'ERROR' para probar abortos.
            def qualityGate = [status: 'OK']

            if (qualityGate.status != 'OK') {
                if (alwaysAbort) {
                    error "Quality Gate falló. Abortando pipeline (politica: alwaysAbort=true). Branch: ${branch}"
                } else {
                    // Heurística según la rama
                    if (branch == 'master' || branch == 'main' || branch.startsWith('hotfix')) {
                        error "Quality Gate falló en rama crítica (${branch}). Abortando pipeline."
                    } else {
                        echo "Quality Gate falló, pero el pipeline continuará (rama: ${branch})."
                    }
                }
            } else {
                echo "Quality Gate aprobado."
            }
        }
    }

    echo "Fin del análisis estático de código"
}